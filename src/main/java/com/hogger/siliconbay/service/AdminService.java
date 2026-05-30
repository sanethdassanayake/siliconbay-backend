package com.hogger.siliconbay.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.AdminActionDTO;
import com.hogger.siliconbay.entity.Seller;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.User;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class AdminService {
    public String listUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<User> users = session.createQuery("FROM User u ORDER BY u.createdAt DESC", User.class).getResultList();
            JsonArray array = new JsonArray();
            for (User user : users) {
                array.add(buildUserJson(user));
            }
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Users found successfully");
            responseObject.add("users", array);
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String updateUser(int userId, String jsonData) {
        AdminActionDTO dto = AppUtil.GSON.fromJson(jsonData, AdminActionDTO.class);
        if (dto == null) {
            return error("Invalid request");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            User user = session.get(User.class, userId);
            if (user == null) {
                tx.rollback();
                return error("User not found");
            }

            if (dto.getRole() != null && !dto.getRole().isBlank()) {
                user.setRole(dto.getRole().toUpperCase());
            }
            if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
                Status status = session.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", dto.getStatus().toUpperCase())
                        .getSingleResultOrNull();
                if (status == null) {
                    tx.rollback();
                    return error("Invalid status");
                }
                user.setStatus(status);
            }
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "User updated successfully");
            responseObject.add("user", buildUserJson(user));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String listSellers() {
        return new SellerService().getAllSellers();
    }

    public String updateSeller(int sellerId, String jsonData) {
        AdminActionDTO dto = AppUtil.GSON.fromJson(jsonData, AdminActionDTO.class);
        if (dto == null || dto.getStatus() == null || dto.getStatus().isBlank()) {
            return error("Seller status is required");
        }
        return new SellerService().updateSellerStatus(sellerId, dto.getStatus());
    }

    private JsonObject buildUserJson(User user) {
        JsonObject userObject = new JsonObject();
        userObject.addProperty("id", user.getId());
        userObject.addProperty("firstName", user.getFirstName());
        userObject.addProperty("lastName", user.getLastName());
        userObject.addProperty("email", user.getEmail());
        userObject.addProperty("role", user.getRole());
        userObject.addProperty("status", user.getStatus() == null ? null : user.getStatus().getValue());
        return userObject;
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}

