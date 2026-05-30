package com.hogger.siliconbay.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.ReturnDTO;
import com.hogger.siliconbay.entity.Order;
import com.hogger.siliconbay.entity.Product;
import com.hogger.siliconbay.entity.Return;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ReturnService {
    public String createReturn(String jsonData, HttpServletRequest request) {
        ReturnDTO dto = AppUtil.GSON.fromJson(jsonData, ReturnDTO.class);
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null || dto.getOrderId() <= 0 || dto.getProductId() <= 0 || dto.getQuantity() <= 0 || dto.getReason() == null || dto.getReason().isBlank()) {
            return error("Order, product, quantity and reason are required");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Order order = session.get(Order.class, dto.getOrderId());
            Product product = session.get(Product.class, dto.getProductId());
            if (order == null || product == null || order.getUser().getId() != userId) {
                tx.rollback();
                return error("Order or product not found");
            }

            Return returnRequest = new Return();
            returnRequest.setOrder(order);
            returnRequest.setProduct(product);
            returnRequest.setQuantity(dto.getQuantity());
            returnRequest.setReason(dto.getReason());
            returnRequest.setRefundAmount(dto.getRefundAmount() < 0 ? 0 : dto.getRefundAmount());
            Status pendingStatus = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.PENDING))
                    .getSingleResult();
            returnRequest.setStatus(pendingStatus);
            session.persist(returnRequest);
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Return request created successfully");
            responseObject.add("return", buildReturnJson(returnRequest));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String getMyReturns(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Return> returns = session.createQuery("FROM Return r WHERE r.order.user.id=:userId ORDER BY r.createdAt DESC", Return.class)
                    .setParameter("userId", userId)
                    .getResultList();
            JsonArray array = new JsonArray();
            for (Return r : returns) {
                array.add(buildReturnJson(r));
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Return requests found successfully");
            responseObject.add("returns", array);
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String getReturnById(long id, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        String role = CurrentUserUtil.getRole(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Return returnRequest = session.get(Return.class, id);
            if (returnRequest == null || (returnRequest.getOrder().getUser().getId() != userId && (role == null || !role.equalsIgnoreCase("ADMIN")))) {
                return error("Return request not found");
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Return request found successfully");
            responseObject.add("return", buildReturnJson(returnRequest));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String updateReturnStatus(long id, String jsonData, HttpServletRequest request) {
        ReturnDTO dto = AppUtil.GSON.fromJson(jsonData, ReturnDTO.class);
        String role = CurrentUserUtil.getRole(request);
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            return error("Admin access required");
        }
        if (dto == null || dto.getStatus() == null || dto.getStatus().isBlank()) {
            return error("Status is required");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Return returnRequest = session.get(Return.class, id);
            if (returnRequest == null) {
                tx.rollback();
                return error("Return request not found");
            }

            Status status = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", dto.getStatus().toUpperCase())
                    .getSingleResultOrNull();
            if (status == null) {
                tx.rollback();
                return error("Invalid status");
            }

            returnRequest.setStatus(status);
            if (dto.getRefundAmount() > 0) {
                returnRequest.setRefundAmount(dto.getRefundAmount());
            }
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Return request updated successfully");
            responseObject.add("return", buildReturnJson(returnRequest));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String deleteReturn(long id, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        String role = CurrentUserUtil.getRole(request);
        if (userId == null) {
            return error("User session not found");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Return returnRequest = session.get(Return.class, id);
            if (returnRequest == null) {
                tx.rollback();
                return error("Return request not found");
            }

            boolean isAdmin = role != null && role.equalsIgnoreCase("ADMIN");
            boolean isOwner = returnRequest.getOrder().getUser().getId() == userId;
            boolean isPending = returnRequest.getStatus() != null && String.valueOf(Status.Type.PENDING).equals(returnRequest.getStatus().getValue());
            if (!isAdmin && !(isOwner && isPending)) {
                tx.rollback();
                return error("Return request can not be deleted");
            }

            session.remove(returnRequest);
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Return request deleted successfully");
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    private JsonObject buildReturnJson(Return r) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", r.getId());
        obj.addProperty("orderId", r.getOrder() == null ? 0 : r.getOrder().getId());
        obj.addProperty("productId", r.getProduct() == null ? 0 : r.getProduct().getId());
        obj.addProperty("productName", r.getProduct() == null ? null : r.getProduct().getName());
        obj.addProperty("quantity", r.getQuantity());
        obj.addProperty("reason", r.getReason());
        obj.addProperty("refundAmount", r.getRefundAmount());
        obj.addProperty("status", r.getStatus() == null ? null : r.getStatus().getValue());
        return obj;
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}

