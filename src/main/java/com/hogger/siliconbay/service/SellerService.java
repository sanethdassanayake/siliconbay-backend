package com.hogger.siliconbay.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.SellerDTO;
import com.hogger.siliconbay.entity.Seller;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.User;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class SellerService {
    public String createSellerProfile(String jsonData, HttpServletRequest request) {
        SellerDTO dto = AppUtil.GSON.fromJson(jsonData, SellerDTO.class);
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null || dto.getCompanyName() == null || dto.getCompanyName().isBlank() || dto.getCompanyMobile() == null || dto.getCompanyMobile().isBlank() || dto.getCompanyEmail() == null || dto.getCompanyEmail().isBlank()) {
            return error("Company name, mobile and email are required");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Seller existingSeller = session.createQuery("FROM Seller s WHERE s.user.id=:userId", Seller.class)
                    .setParameter("userId", userId)
                    .getSingleResultOrNull();
            if (existingSeller != null) {
                tx.rollback();
                return error("Seller profile already exists");
            }

            User user = session.get(User.class, userId);
            Status pendingStatus = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.PENDING))
                    .getSingleResult();

            Seller seller = new Seller();
            seller.setUser(user);
            seller.setCompanyName(dto.getCompanyName());
            seller.setCompanyMobile(dto.getCompanyMobile());
            seller.setCompanyEmail(dto.getCompanyEmail());
            seller.setStatus(pendingStatus);
            session.persist(seller);

            user.setRole("SELLER");
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Seller profile created successfully");
            responseObject.add("seller", buildSellerJson(seller));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String getMySellerProfile(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Seller seller = session.createQuery("FROM Seller s WHERE s.user.id=:userId", Seller.class)
                    .setParameter("userId", userId)
                    .getSingleResultOrNull();
            if (seller == null) {
                return error("Seller profile not found");
            }

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Seller profile found successfully");
            responseObject.add("seller", buildSellerJson(seller));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String updateMySellerProfile(String jsonData, HttpServletRequest request) {
        SellerDTO dto = AppUtil.GSON.fromJson(jsonData, SellerDTO.class);
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null || dto.getCompanyName() == null || dto.getCompanyName().isBlank() || dto.getCompanyMobile() == null || dto.getCompanyMobile().isBlank() || dto.getCompanyEmail() == null || dto.getCompanyEmail().isBlank()) {
            return error("Company name, mobile and email are required");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Seller seller = session.createQuery("FROM Seller s WHERE s.user.id=:userId", Seller.class)
                    .setParameter("userId", userId)
                    .getSingleResultOrNull();
            if (seller == null) {
                tx.rollback();
                return error("Seller profile not found");
            }

            seller.setCompanyName(dto.getCompanyName());
            seller.setCompanyMobile(dto.getCompanyMobile());
            seller.setCompanyEmail(dto.getCompanyEmail());
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Seller profile updated successfully");
            responseObject.add("seller", buildSellerJson(seller));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String getAllSellers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Seller> sellers = session.createQuery("FROM Seller s ORDER BY s.createdAt DESC", Seller.class).getResultList();
            JsonArray sellerArray = new JsonArray();
            for (Seller seller : sellers) {
                sellerArray.add(buildSellerJson(seller));
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Sellers found successfully");
            responseObject.add("sellers", sellerArray);
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String updateSellerStatus(int sellerId, String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            return error("Status is required");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Seller seller = session.get(Seller.class, sellerId);
            if (seller == null) {
                tx.rollback();
                return error("Seller not found");
            }

            Status status = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", statusValue.toUpperCase())
                    .getSingleResultOrNull();
            if (status == null) {
                tx.rollback();
                return error("Invalid status");
            }

            seller.setStatus(status);
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Seller status updated successfully");
            responseObject.add("seller", buildSellerJson(seller));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    private JsonObject buildSellerJson(Seller seller) {
        JsonObject sellerObject = new JsonObject();
        sellerObject.addProperty("id", seller.getId());
        sellerObject.addProperty("userId", seller.getUser() == null ? 0 : seller.getUser().getId());
        sellerObject.addProperty("companyName", seller.getCompanyName());
        sellerObject.addProperty("companyMobile", seller.getCompanyMobile());
        sellerObject.addProperty("companyEmail", seller.getCompanyEmail());
        sellerObject.addProperty("status", seller.getStatus() == null ? null : seller.getStatus().getValue());
        return sellerObject;
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}

