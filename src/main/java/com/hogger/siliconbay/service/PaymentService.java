package com.hogger.siliconbay.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.PaymentInstrumentDTO;
import com.hogger.siliconbay.entity.PaymentMethod;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.UserPaymentInstrument;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PaymentService {
    public String listMethods() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<PaymentMethod> methods = session.createQuery("FROM PaymentMethod pm ORDER BY pm.id", PaymentMethod.class).getResultList();
            JsonArray array = new JsonArray();
            for (PaymentMethod method : methods) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", method.getId());
                obj.addProperty("code", method.getCode());
                obj.addProperty("type", method.getType() == null ? null : method.getType().getName());
                array.add(obj);
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Payment methods found successfully");
            responseObject.add("methods", array);
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String listInstruments(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<UserPaymentInstrument> instruments = session.createQuery("FROM UserPaymentInstrument p WHERE p.user.id=:userId ORDER BY p.id DESC", UserPaymentInstrument.class)
                    .setParameter("userId", userId)
                    .getResultList();
            JsonArray array = new JsonArray();
            for (UserPaymentInstrument instrument : instruments) {
                array.add(buildInstrumentJson(instrument));
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Payment instruments found successfully");
            responseObject.add("instruments", array);
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String createInstrument(String jsonData, HttpServletRequest request) {
        PaymentInstrumentDTO dto = AppUtil.GSON.fromJson(jsonData, PaymentInstrumentDTO.class);
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null || dto.getPaymentMethodId() <= 0 || dto.getGatewayToken() == null || dto.getGatewayToken().isBlank()) {
            return error("Payment method and gateway token are required");
        }
        if (dto.getBrand() == null || dto.getBrand().isBlank() || dto.getLast4() < 0 || dto.getExpMonth() < 1 || dto.getExpMonth() > 12 || dto.getExpYear() < 2000) {
            return error("Payment instrument data is invalid");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            PaymentMethod method = session.get(PaymentMethod.class, dto.getPaymentMethodId());
            if (method == null) {
                tx.rollback();
                return error("Payment method not found");
            }

            UserPaymentInstrument instrument = new UserPaymentInstrument();
            instrument.setUser(session.get(com.hogger.siliconbay.entity.User.class, userId));
            instrument.setPaymentMethod(method);
            instrument.setGatewayToken(dto.getGatewayToken());
            instrument.setLast4(dto.getLast4());
            instrument.setBrand(dto.getBrand());
            instrument.setExpMonth(dto.getExpMonth());
            instrument.setExpYear(dto.getExpYear());
            instrument.setDefault(dto.isDefault());
            if (dto.isDefault()) {
                unsetDefaultCards(session, userId);
            }
            session.persist(instrument);
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Payment instrument saved successfully");
            responseObject.add("instrument", buildInstrumentJson(instrument));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String updateInstrument(long id, String jsonData, HttpServletRequest request) {
        PaymentInstrumentDTO dto = AppUtil.GSON.fromJson(jsonData, PaymentInstrumentDTO.class);
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null || dto.getPaymentMethodId() <= 0 || dto.getGatewayToken() == null || dto.getGatewayToken().isBlank()) {
            return error("Payment method and gateway token are required");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            UserPaymentInstrument instrument = session.get(UserPaymentInstrument.class, id);
            if (instrument == null || instrument.getUser().getId() != userId) {
                tx.rollback();
                return error("Payment instrument not found");
            }
            PaymentMethod method = session.get(PaymentMethod.class, dto.getPaymentMethodId());
            if (method == null) {
                tx.rollback();
                return error("Payment method not found");
            }
            instrument.setPaymentMethod(method);
            instrument.setGatewayToken(dto.getGatewayToken());
            instrument.setLast4(dto.getLast4());
            instrument.setBrand(dto.getBrand());
            instrument.setExpMonth(dto.getExpMonth());
            instrument.setExpYear(dto.getExpYear());
            instrument.setDefault(dto.isDefault());
            if (dto.isDefault()) {
                unsetDefaultCards(session, userId);
            }
            tx.commit();
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Payment instrument updated successfully");
            responseObject.add("instrument", buildInstrumentJson(instrument));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String deleteInstrument(long id, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            UserPaymentInstrument instrument = session.get(UserPaymentInstrument.class, id);
            if (instrument == null || instrument.getUser().getId() != userId) {
                tx.rollback();
                return error("Payment instrument not found");
            }
            session.remove(instrument);
            tx.commit();
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Payment instrument deleted successfully");
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    private void unsetDefaultCards(Session session, int userId) {
        List<UserPaymentInstrument> instruments = session.createQuery("FROM UserPaymentInstrument p WHERE p.user.id=:userId", UserPaymentInstrument.class)
                .setParameter("userId", userId)
                .getResultList();
        for (UserPaymentInstrument instrument : instruments) {
            instrument.setDefault(false);
        }
    }

    private JsonObject buildInstrumentJson(UserPaymentInstrument instrument) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", instrument.getId());
        obj.addProperty("paymentMethodId", instrument.getPaymentMethod() == null ? 0 : instrument.getPaymentMethod().getId());
        obj.addProperty("paymentMethodCode", instrument.getPaymentMethod() == null ? null : instrument.getPaymentMethod().getCode());
        obj.addProperty("brand", instrument.getBrand());
        obj.addProperty("last4", instrument.getLast4());
        obj.addProperty("expMonth", instrument.getExpMonth());
        obj.addProperty("expYear", instrument.getExpYear());
        obj.addProperty("isDefault", instrument.isDefault());
        return obj;
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}

