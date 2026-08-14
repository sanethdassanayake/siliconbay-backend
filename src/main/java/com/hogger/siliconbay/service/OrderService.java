package com.hogger.siliconbay.service;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.CheckoutDTO;
import com.hogger.siliconbay.entity.Cart;
import com.hogger.siliconbay.entity.CartItem;
import com.hogger.siliconbay.entity.DeliveryType;
import com.hogger.siliconbay.entity.Order;
import com.hogger.siliconbay.entity.OrderItem;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.Stock;
import com.hogger.siliconbay.entity.User;
import com.hogger.siliconbay.entity.UserPaymentInstrument;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.Env;
import com.hogger.siliconbay.util.HibernateUtil;

import jakarta.servlet.http.HttpServletRequest;

public class OrderService {
    public String checkout(String jsonData, HttpServletRequest request) {
        CheckoutDTO dto = AppUtil.GSON.fromJson(jsonData, CheckoutDTO.class);
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null) {
            return error("Checkout data is required");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            User user = session.get(User.class, userId);
            DeliveryType deliveryType = dto.getDeliveryTypeId() > 0
                    ? session.get(DeliveryType.class, dto.getDeliveryTypeId())
                    : session.createQuery("FROM DeliveryType d ORDER BY d.id ASC", DeliveryType.class)
                    .setMaxResults(1)
                    .getSingleResultOrNull();
            UserPaymentInstrument instrument = dto.getPaymentInstrumentId() == null
                    ? null
                    : session.get(UserPaymentInstrument.class, dto.getPaymentInstrumentId());
            Cart cart = session.createQuery("FROM Cart c WHERE c.user.id=:userId", Cart.class)
                    .setParameter("userId", userId)
                    .getSingleResultOrNull();
            if (user == null || deliveryType == null || cart == null) {
                tx.rollback();
                return error("Checkout data not found");
            }
            if (instrument != null && instrument.getUser().getId() != userId) {
                instrument = null;
            }

            List<CartItem> items = session.createQuery("FROM CartItem ci WHERE ci.cart.id=:cartId", CartItem.class)
                    .setParameter("cartId", cart.getId())
                    .getResultList();
            if (items.isEmpty()) {
                tx.rollback();
                return error("Cart is empty");
            }

            Order order = new Order();
            order.setUser(user);
            order.setDeliveryType(deliveryType);
            order.setStatus(session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.PACKING))
                    .getSingleResult());
            session.persist(order);

            double total = deliveryType.getPrice();
            for (CartItem cartItem : items) {
                Stock stock = session.createQuery("FROM Stock s WHERE s.product.id=:productId ORDER BY s.price ASC", Stock.class)
                        .setParameter("productId", cartItem.getProduct().getId())
                        .setMaxResults(1)
                        .getSingleResultOrNull();
                if (stock == null || stock.getQty() < cartItem.getQuantity()) {
                    tx.rollback();
                    return error("Insufficient stock for product " + cartItem.getProduct().getName());
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setStock(stock);
                orderItem.setQty(cartItem.getQuantity());
                session.persist(orderItem);

                stock.setQty(stock.getQty() - cartItem.getQuantity());
                total += stock.getPrice() * cartItem.getQuantity();
            }

            com.hogger.siliconbay.entity.Transaction paymentTransaction = new com.hogger.siliconbay.entity.Transaction();
            paymentTransaction.setOrder(order);
            paymentTransaction.setAmount(roundMoney(total));
            paymentTransaction.setUserPaymentInstrument(instrument);
                paymentTransaction.setStatus(session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.PENDING))
                    .getSingleResult());
            session.persist(paymentTransaction);

            for (CartItem item : items) {
                session.remove(item);
            }

            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Order placed successfully");
            responseObject.add("order", buildOrderJson(session, order));
            responseObject.addProperty("transactionId", paymentTransaction.getId());
            responseObject.addProperty("total", roundMoney(total));
            String currency = Env.get("app.currency") == null ? "LKR" : Env.get("app.currency");
            responseObject.addProperty("currency", currency);
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String getMyOrders(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Order> orders = session.createQuery("FROM Order o WHERE o.user.id=:userId ORDER BY o.createdAt DESC", Order.class)
                    .setParameter("userId", userId)
                    .getResultList();
            JsonArray array = new JsonArray();
            for (Order order : orders) {
                array.add(buildOrderJson(session, order));
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Orders found successfully");
            responseObject.add("orders", array);
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String getOrderById(int orderId, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        String role = CurrentUserUtil.getRole(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Order order = session.get(Order.class, orderId);
            if (order == null || (order.getUser().getId() != userId && (role == null || !role.equalsIgnoreCase("ADMIN")))) {
                return error("Order not found");
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Order found successfully");
            responseObject.add("order", buildOrderJson(session, order));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String updateOrderStatus(int orderId, String statusValue, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        String role = CurrentUserUtil.getRole(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (statusValue == null || statusValue.isBlank()) {
            return error("Status is required");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Order order = session.get(Order.class, orderId);
            if (order == null) {
                tx.rollback();
                return error("Order not found");
            }

            boolean isAdmin = role != null && role.equalsIgnoreCase("ADMIN");
            boolean isOwner = order.getUser().getId() == userId;
            if (!isAdmin && !isOwner) {
                tx.rollback();
                return error("Order not found");
            }

            String requestedStatus = statusValue.toUpperCase();
            if (!isAdmin && !String.valueOf(Status.Type.CANCELED).equals(requestedStatus)) {
                tx.rollback();
                return error("Only admin can set this status");
            }

            Status status = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", requestedStatus)
                    .getSingleResultOrNull();
            if (status == null) {
                tx.rollback();
                return error("Invalid status");
            }

            order.setStatus(status);
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Order status updated successfully");
            responseObject.add("order", buildOrderJson(session, order));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String deleteOrder(int orderId, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        String role = CurrentUserUtil.getRole(request);
        if (userId == null) {
            return error("User session not found");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Order order = session.get(Order.class, orderId);
            if (order == null) {
                tx.rollback();
                return error("Order not found");
            }

            boolean isAdmin = role != null && role.equalsIgnoreCase("ADMIN");
            boolean isOwner = order.getUser().getId() == userId;
            if (!isAdmin && !isOwner) {
                tx.rollback();
                return error("Order not found");
            }

            List<OrderItem> items = session.createQuery("FROM OrderItem oi WHERE oi.order.id=:orderId", OrderItem.class)
                    .setParameter("orderId", orderId)
                    .getResultList();
            for (OrderItem item : items) {
                session.remove(item);
            }

            com.hogger.siliconbay.entity.Transaction transaction = session.createQuery("FROM Transaction t WHERE t.order.id=:orderId", com.hogger.siliconbay.entity.Transaction.class)
                    .setParameter("orderId", orderId)
                    .getSingleResultOrNull();
            if (transaction != null) {
                session.remove(transaction);
            }

            session.remove(order);
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Order deleted successfully");
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    private JsonObject buildOrderJson(Session session, Order order) {
        JsonObject orderObject = new JsonObject();
        orderObject.addProperty("id", order.getId());
        orderObject.addProperty("orderId", order.getId());
        orderObject.addProperty("createdAt", order.getCreatedAt() == null ? null : order.getCreatedAt().toString());
        orderObject.addProperty("updatedAt", order.getUpdatedAt() == null ? null : order.getUpdatedAt().toString());
        orderObject.addProperty("date", order.getCreatedAt() == null ? null : order.getCreatedAt().toString());
        orderObject.addProperty("status", order.getStatus() == null ? null : order.getStatus().getValue());
        orderObject.addProperty("deliveryType", order.getDeliveryType() == null ? null : order.getDeliveryType().getName());
        orderObject.addProperty("deliveryPrice", order.getDeliveryType() == null ? 0 : roundMoney(order.getDeliveryType().getPrice()));
        JsonArray itemArray = new JsonArray();
        double total = order.getDeliveryType() == null ? 0 : roundMoney(order.getDeliveryType().getPrice());

        List<OrderItem> items = session.createQuery("FROM OrderItem oi WHERE oi.order.id=:orderId", OrderItem.class)
                .setParameter("orderId", order.getId())
                .getResultList();
        for (OrderItem item : items) {
            JsonObject itemObject = new JsonObject();
            itemObject.addProperty("id", item.getId());
            itemObject.addProperty("quantity", item.getQty());
            itemObject.addProperty("stockId", item.getStock().getId());
            itemObject.addProperty("productId", item.getStock().getProduct().getId());
            itemObject.addProperty("productName", item.getStock().getProduct().getName());
            double itemPrice = roundMoney(item.getStock().getPrice());
            double subtotal = roundMoney(itemPrice * item.getQty());
            itemObject.addProperty("price", itemPrice);
            itemObject.addProperty("subtotal", subtotal);
            total += subtotal;
            itemArray.add(itemObject);
        }

        orderObject.addProperty("itemsCount", items.size());

        com.hogger.siliconbay.entity.Transaction transaction = session.createQuery("FROM Transaction t WHERE t.order.id=:orderId", com.hogger.siliconbay.entity.Transaction.class)
                .setParameter("orderId", order.getId())
                .getSingleResultOrNull();
        long transactionId = 0L;
        if (transaction != null && transaction.getId() != null) {
            transactionId = transaction.getId();
        }
        orderObject.addProperty("transactionId", transactionId);
        double roundedTransactionAmount = transaction == null || transaction.getAmount() == null ? total : roundMoney(transaction.getAmount());
        orderObject.addProperty("transactionAmount", roundedTransactionAmount);
        orderObject.addProperty("total", roundedTransactionAmount);
        orderObject.addProperty("amount", roundedTransactionAmount);
        String currency = Env.get("app.currency") == null ? "LKR" : Env.get("app.currency");
        orderObject.addProperty("currency", currency);
        orderObject.addProperty("paymentMethod", transaction == null || transaction.getPaymentMethod() == null ? "PayHere" : transaction.getPaymentMethod());
        orderObject.addProperty("paymentId", transaction == null || transaction.getPaymentId() == null ? "" : transaction.getPaymentId());
        orderObject.addProperty("trackingNumber", transaction == null || transaction.getPaymentId() == null ? "" : transaction.getPaymentId());
        orderObject.add("items", itemArray);
        return orderObject;
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}

