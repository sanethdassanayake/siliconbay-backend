package com.hogger.siliconbay.controller.api;

import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.PayHereIPNDTO;
import com.hogger.siliconbay.dto.PayHereRequestDTO;
import com.hogger.siliconbay.provider.PayHerePaymentProvider;
import com.hogger.siliconbay.service.PaymentService;
import com.hogger.siliconbay.util.PayHereUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import com.hogger.siliconbay.entity.Order;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.Transaction;
import com.hogger.siliconbay.entity.UserPaymentInstrument;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hibernate.Session;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.logging.Logger;

@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
public class PaymentController {
    private static final Logger logger = Logger.getLogger(PaymentController.class.getName());
    private final PaymentService paymentService = new PaymentService();
    private final PayHerePaymentProvider provider = new PayHerePaymentProvider();

    @GET
    @Path("/methods")
    public Response getMethods() {
        return Response.ok(paymentService.listMethods()).build();
    }

    @GET
    @Path("/instruments")
    public Response getInstruments(@Context HttpServletRequest request) {
        return Response.ok(paymentService.listInstruments(request)).build();
    }

    @POST
    @Path("/instruments")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createInstrument(String jsonData, @Context HttpServletRequest request) {
        return Response.ok(paymentService.createInstrument(jsonData, request)).build();
    }

    @PUT
    @Path("/instruments/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateInstrument(@PathParam("id") long id, String jsonData, @Context HttpServletRequest request) {
        return Response.ok(paymentService.updateInstrument(id, jsonData, request)).build();
    }

    @DELETE
    @Path("/instruments/{id}")
    public Response deleteInstrument(@PathParam("id") long id, @Context HttpServletRequest request) {
        return Response.ok(paymentService.deleteInstrument(id, request)).build();
    }

    @POST
    @Path("/payhere/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public Response createPayHerePayment(PayHereRequestDTO req) {
        if (PayHereUtil.getMerchantId().isEmpty()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("PAYHERE_MERCHANT_ID not configured").build();
        }
        String html = provider.buildAutoSubmitForm(req);
        return Response.ok(html, MediaType.TEXT_HTML).build();
    }

    @POST
    @Path("/payhere/ipn")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response handleIpn(@Context HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        PayHereIPNDTO ipn = new PayHereIPNDTO();
        ipn.setMerchant_id(getParam(params, "merchant_id"));
        ipn.setOrder_id(getParam(params, "order_id"));
        ipn.setPayment_id(getParam(params, "payment_id"));
        ipn.setPayhere_amount(getParam(params, "payhere_amount"));
        ipn.setPayhere_currency(getParam(params, "payhere_currency"));
        ipn.setStatus_code(getParam(params, "status_code"));
        ipn.setMd5sig(getParam(params, "md5sig"));
        ipn.setMethod(getParam(params, "method"));
        ipn.setStatus_message(getParam(params, "status_message"));
        ipn.setCustom_1(getParam(params, "custom_1"));
        ipn.setCustom_2(getParam(params, "custom_2"));

        logger.info("Received PayHere IPN: merchant=" + ipn.getMerchant_id() + " order=" + ipn.getOrder_id()
                + " status=" + ipn.getStatus_code() + " amount=" + ipn.getPayhere_amount());

        String configuredMerchant = PayHereUtil.getMerchantId();
        if (!configuredMerchant.equals(ipn.getMerchant_id())) {
            logger.warning("IPN merchant_id mismatch: " + ipn.getMerchant_id());
            return Response.status(Response.Status.BAD_REQUEST).entity("FAIL").build();
        }

        // verify md5sig
        String localMd5 = computeLocalMd5(ipn);
        if (localMd5 == null || !localMd5.equalsIgnoreCase(ipn.getMd5sig())) {
            logger.warning("IPN signature mismatch. local=" + localMd5 + " remote=" + ipn.getMd5sig());
            return Response.status(Response.Status.BAD_REQUEST).entity("FAIL").build();
        }

        // status_code 2 means success
        if ("2".equals(ipn.getStatus_code())) {
            // Mark order and transaction as completed
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                org.hibernate.Transaction tx = session.beginTransaction();
                try {
                    // order_id may be numeric or custom; try numeric first
                    Order order = null;
                    try {
                        int oid = Integer.parseInt(ipn.getOrder_id());
                        order = session.get(Order.class, oid);
                    } catch (NumberFormatException ignored) {
                        // Order id not numeric — for this simple university project we expect numeric Order.id
                        logger.warning("IPN order_id is not numeric: " + ipn.getOrder_id());
                    }

                    if (order == null) {
                        logger.warning("Order not found for IPN order_id=" + ipn.getOrder_id());
                        tx.rollback();
                        return Response.status(Response.Status.BAD_REQUEST).entity("FAIL").build();
                    }

                    Status completedStatus = session.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("value", String.valueOf(Status.Type.COMPLETED))
                            .getSingleResult();
                    order.setStatus(completedStatus);
                    session.persist(order);

                    // Create or update Transaction
                    com.hogger.siliconbay.entity.Transaction transaction = session.createQuery("FROM Transaction t WHERE t.order.id=:orderId", com.hogger.siliconbay.entity.Transaction.class)
                            .setParameter("orderId", order.getId())
                            .setMaxResults(1)
                            .getSingleResultOrNull();
                    if (transaction == null) {
                        transaction = new com.hogger.siliconbay.entity.Transaction();
                        transaction.setOrder(order);
                    }
                    double amount = 0;
                    try {
                        amount = Double.parseDouble(ipn.getPayhere_amount());
                    } catch (Exception ignored) {}
                    transaction.setAmount(amount);

                    // Persist PayHere identifiers
                    transaction.setPaymentId(ipn.getPayment_id());
                    transaction.setPaymentMethod(ipn.getMethod());

                    // No payment instrument mapping here; leave null or map by method if you want
                    transaction.setStatus(completedStatus);
                    session.persist(transaction);

                    tx.commit();
                    logger.info("Order " + order.getId() + " marked as COMPLETED from IPN");
                    return Response.ok("OK").build();
                } catch (Exception e) {
                    tx.rollback();
                    logger.severe("Error processing IPN: " + e.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("FAIL").build();
                }
            }
        } else {
            logger.info("Payment NOT successful for order " + ipn.getOrder_id() + " status " + ipn.getStatus_code());
            return Response.ok("FAIL").build();
        }
    }

    @POST
    @Path("/payhere/hash")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generatePayHereHash(PayHereRequestDTO req) {
        if (PayHereUtil.getMerchantId().isEmpty() || PayHereUtil.getMerchantSecret().isEmpty()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("PAYHERE_MERCHANT_ID or PAYHERE_MERCHANT_SECRET not configured").build();
        }

        try {
            String merchantId = PayHereUtil.getMerchantId();
            String merchantSecret = PayHereUtil.getMerchantSecret();
            DecimalFormat df = new DecimalFormat("0.00");
            String amountFormatted = df.format(req.getAmount());

            String hashedSecret = md5(merchantSecret).toUpperCase();
            String raw = merchantId + req.getOrderId() + amountFormatted + req.getCurrency() + hashedSecret;
            String hash = md5(raw).toUpperCase();

            JsonObject obj = new JsonObject();
            obj.addProperty("hash", hash);
            return Response.ok(obj).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("HASH_ERROR").build();
        }
    }

    @POST
    @Path("/payhere/create-js")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPayHereJsPayload(PayHereRequestDTO req) {
        if (PayHereUtil.getMerchantId().isEmpty() || PayHereUtil.getMerchantSecret().isEmpty()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("PAYHERE_MERCHANT_ID or PAYHERE_MERCHANT_SECRET not configured").build();
        }

        try {
            String merchantId = PayHereUtil.getMerchantId();
            String merchantSecret = PayHereUtil.getMerchantSecret();
            DecimalFormat df = new DecimalFormat("0.00");
            String amountFormatted = df.format(req.getAmount());

            String hashedSecret = md5(merchantSecret).toUpperCase();
            String raw = merchantId + req.getOrderId() + amountFormatted + req.getCurrency() + hashedSecret;
            String hash = md5(raw).toUpperCase();

            JsonObject payload = new JsonObject();
            payload.addProperty("sandbox", true);
            payload.addProperty("merchant_id", merchantId);
            payload.addProperty("return_url", PayHereUtil.getAppUrl() + "/payments/success");
            payload.addProperty("cancel_url", PayHereUtil.getAppUrl() + "/payments/cancel");
            payload.addProperty("notify_url", PayHereUtil.getAppUrl() + "/api/payments/payhere/ipn");
            payload.addProperty("order_id", req.getOrderId());
            payload.addProperty("items", req.getItems());
            payload.addProperty("currency", req.getCurrency());
            payload.addProperty("amount", amountFormatted);
            payload.addProperty("hash", hash);
            payload.addProperty("first_name", req.getFirstName());
            payload.addProperty("last_name", req.getLastName());
            payload.addProperty("email", req.getEmail());
            payload.addProperty("phone", req.getPhone());
            payload.addProperty("address", req.getAddress());
            payload.addProperty("city", req.getCity());
            payload.addProperty("country", req.getCountry());

            return Response.ok(payload).build();
        } catch (Exception e) {
            logger.severe("Error building PayHere JS payload: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("HASH_ERROR").build();
        }
    }

    private String computeLocalMd5(PayHereIPNDTO ipn) {
        try {
            String merchantSecret = PayHereUtil.getMerchantSecret();
            String hashedSecret = md5(merchantSecret).toUpperCase();
            String data = ipn.getMerchant_id() + ipn.getOrder_id() + ipn.getPayhere_amount() + ipn.getPayhere_currency() + ipn.getStatus_code() + hashedSecret;
            return md5(data).toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }

    private String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(input.getBytes("UTF-8"));
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }

    private String getParam(Map<String, String[]> params, String name) {
        String[] arr = params.get(name);
        if (arr == null || arr.length == 0) return null;
        return arr[0];
    }
}
