package com.hogger.siliconbay.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class TransactionService {
    public String getMyTransactions(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<com.hogger.siliconbay.entity.Transaction> transactions = session.createQuery("FROM Transaction t WHERE t.order.user.id=:userId ORDER BY t.createdAt DESC", com.hogger.siliconbay.entity.Transaction.class)
                    .setParameter("userId", userId)
                    .getResultList();
            JsonArray array = new JsonArray();
            for (com.hogger.siliconbay.entity.Transaction tx : transactions) {
                array.add(buildTransactionJson(tx));
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Transactions found successfully");
            responseObject.add("transactions", array);
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String getTransactionById(long id, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        String role = CurrentUserUtil.getRole(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            com.hogger.siliconbay.entity.Transaction tx = session.get(com.hogger.siliconbay.entity.Transaction.class, id);
            if (tx == null || (tx.getOrder().getUser().getId() != userId && (role == null || !role.equalsIgnoreCase("ADMIN")))) {
                return error("Transaction not found");
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Transaction found successfully");
            responseObject.add("transaction", buildTransactionJson(tx));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String updateTransactionStatus(long id, String statusValue, HttpServletRequest request) {
        String role = CurrentUserUtil.getRole(request);
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            return error("Admin access required");
        }
        if (statusValue == null || statusValue.isBlank()) {
            return error("Status is required");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            com.hogger.siliconbay.entity.Transaction entity = session.get(com.hogger.siliconbay.entity.Transaction.class, id);
            if (entity == null) {
                tx.rollback();
                return error("Transaction not found");
            }

            Status status = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", statusValue.toUpperCase())
                    .getSingleResultOrNull();
            if (status == null) {
                tx.rollback();
                return error("Invalid status");
            }

            entity.setStatus(status);
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Transaction updated successfully");
            responseObject.add("transaction", buildTransactionJson(entity));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String deleteTransaction(long id, HttpServletRequest request) {
        String role = CurrentUserUtil.getRole(request);
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            return error("Admin access required");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            com.hogger.siliconbay.entity.Transaction entity = session.get(com.hogger.siliconbay.entity.Transaction.class, id);
            if (entity == null) {
                tx.rollback();
                return error("Transaction not found");
            }
            session.remove(entity);
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Transaction deleted successfully");
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    private JsonObject buildTransactionJson(com.hogger.siliconbay.entity.Transaction tx) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", tx.getId());
        obj.addProperty("orderId", tx.getOrder() == null ? 0 : tx.getOrder().getId());
        obj.addProperty("amount", tx.getAmount());
        obj.addProperty("status", tx.getStatus() == null ? null : tx.getStatus().getValue());
        obj.addProperty("paymentMethod", tx.getUserPaymentInstrument() == null || tx.getUserPaymentInstrument().getPaymentMethod() == null ? null : tx.getUserPaymentInstrument().getPaymentMethod().getCode());
        obj.addProperty("paymentBrand", tx.getUserPaymentInstrument() == null ? null : tx.getUserPaymentInstrument().getBrand());
        return obj;
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
