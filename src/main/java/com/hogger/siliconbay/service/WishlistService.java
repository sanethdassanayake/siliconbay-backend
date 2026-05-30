package com.hogger.siliconbay.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.WishlistItemDTO;
import com.hogger.siliconbay.entity.Product;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.Wishlist;
import com.hogger.siliconbay.entity.WishlistItem;
import com.hogger.siliconbay.entity.User;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class WishlistService {
    public String getMyWishlist(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Wishlist wishlist = getOrCreateWishlist(session, userId);
            tx.commit();

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Wishlist found successfully");
            responseObject.add("wishlist", buildWishlistJson(session, wishlist));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String addItem(String jsonData, HttpServletRequest request) {
        WishlistItemDTO dto = AppUtil.GSON.fromJson(jsonData, WishlistItemDTO.class);
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null || dto.getProductId() <= 0) {
            return error("Product is required");
        }
        if (dto.getQuantity() <= 0) {
            dto.setQuantity(1);
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Wishlist wishlist = getOrCreateWishlist(session, userId);
            Product product = session.get(Product.class, dto.getProductId());
            if (product == null) {
                tx.rollback();
                return error("Product not found");
            }

            WishlistItem item = session.createQuery("FROM WishlistItem wi WHERE wi.wishlist.id=:wishlistId AND wi.product.id=:productId", WishlistItem.class)
                    .setParameter("wishlistId", wishlist.getId())
                    .setParameter("productId", dto.getProductId())
                    .getSingleResultOrNull();
            if (item == null) {
                item = new WishlistItem();
                item.setWishlist(wishlist);
                item.setProduct(product);
                item.setQuantity(dto.getQuantity());
                session.persist(item);
            } else {
                item.setQuantity(item.getQuantity() + dto.getQuantity());
            }
            tx.commit();
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Wishlist item saved successfully");
            responseObject.add("wishlist", buildWishlistJson(session, wishlist));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String updateItem(long itemId, String jsonData, HttpServletRequest request) {
        WishlistItemDTO dto = AppUtil.GSON.fromJson(jsonData, WishlistItemDTO.class);
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null || dto.getQuantity() <= 0) {
            return error("Quantity must be greater than zero");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            WishlistItem item = session.get(WishlistItem.class, itemId);
            if (item == null || item.getWishlist().getUser().getId() != userId) {
                tx.rollback();
                return error("Wishlist item not found");
            }
            item.setQuantity(dto.getQuantity());
            tx.commit();
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Wishlist item updated successfully");
            responseObject.add("wishlist", buildWishlistJson(session, item.getWishlist()));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String removeItem(long itemId, HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            WishlistItem item = session.get(WishlistItem.class, itemId);
            if (item == null || item.getWishlist().getUser().getId() != userId) {
                tx.rollback();
                return error("Wishlist item not found");
            }
            Wishlist wishlist = item.getWishlist();
            session.remove(item);
            tx.commit();
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Wishlist item removed successfully");
            responseObject.add("wishlist", buildWishlistJson(session, wishlist));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String clearWishlist(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Wishlist wishlist = getOrCreateWishlist(session, userId);
            List<WishlistItem> items = session.createQuery("FROM WishlistItem wi WHERE wi.wishlist.id=:wishlistId", WishlistItem.class)
                    .setParameter("wishlistId", wishlist.getId())
                    .getResultList();
            for (WishlistItem item : items) {
                session.remove(item);
            }
            tx.commit();
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Wishlist cleared successfully");
            responseObject.add("wishlist", buildWishlistJson(session, wishlist));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    private Wishlist getOrCreateWishlist(Session session, int userId) {
        Wishlist wishlist = session.createQuery("FROM Wishlist w WHERE w.user.id=:userId", Wishlist.class)
                .setParameter("userId", userId)
                .getSingleResultOrNull();
        if (wishlist == null) {
            wishlist = new Wishlist();
            wishlist.setUser(session.get(User.class, userId));
            Status status = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.ACTIVE))
                    .getSingleResult();
            wishlist.setStatus(status);
            session.persist(wishlist);
        }
        return wishlist;
    }

    private JsonObject buildWishlistJson(Session session, Wishlist wishlist) {
        JsonObject wishlistObject = new JsonObject();
        wishlistObject.addProperty("id", wishlist.getId());
        wishlistObject.addProperty("status", wishlist.getStatus() == null ? null : wishlist.getStatus().getValue());
        JsonArray itemArray = new JsonArray();
        List<WishlistItem> items = session.createQuery("FROM WishlistItem wi WHERE wi.wishlist.id=:wishlistId", WishlistItem.class)
                .setParameter("wishlistId", wishlist.getId())
                .getResultList();
        for (WishlistItem item : items) {
            JsonObject itemObject = new JsonObject();
            itemObject.addProperty("id", item.getId());
            itemObject.addProperty("quantity", item.getQuantity());
            itemObject.addProperty("productId", item.getProduct().getId());
            itemObject.addProperty("productName", item.getProduct().getName());
            itemArray.add(itemObject);
        }
        wishlistObject.add("items", itemArray);
        return wishlistObject;
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
