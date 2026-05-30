package com.hogger.siliconbay.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hogger.siliconbay.dto.CartItemDTO;
import com.hogger.siliconbay.entity.Cart;
import com.hogger.siliconbay.entity.CartItem;
import com.hogger.siliconbay.entity.Product;
import com.hogger.siliconbay.entity.Stock;
import com.hogger.siliconbay.entity.Status;
import com.hogger.siliconbay.entity.User;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.HibernateUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CartService {
    public String getMyCart(HttpServletRequest request) {
        JsonObject responseObject = new JsonObject();
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Cart cart = getOrCreateCart(session, userId);
            tx.commit();

            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Cart found successfully");
            responseObject.add("cart", buildCartJson(session, cart));
        }
        return AppUtil.GSON.toJson(responseObject);
    }

    public String addItem(String jsonData, HttpServletRequest request) {
        CartItemDTO dto = AppUtil.GSON.fromJson(jsonData, CartItemDTO.class);
        JsonObject responseObject = new JsonObject();
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null || dto.getProductId() <= 0) {
            return error("Product is required");
        }
        if (dto.getQuantity() <= 0) {
            return error("Quantity must be greater than zero");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Cart cart = getOrCreateCart(session, userId);
            Product product = session.get(Product.class, dto.getProductId());
            if (product == null) {
                tx.rollback();
                return error("Product not found");
            }

            CartItem item = session.createQuery("FROM CartItem ci WHERE ci.cart.id=:cartId AND ci.product.id=:productId", CartItem.class)
                    .setParameter("cartId", cart.getId())
                    .setParameter("productId", dto.getProductId())
                    .getSingleResultOrNull();
            if (item == null) {
                item = new CartItem();
                item.setCart(cart);
                item.setProduct(product);
                item.setQuantity(dto.getQuantity());
                session.persist(item);
            } else {
                item.setQuantity(item.getQuantity() + dto.getQuantity());
            }
            tx.commit();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Cart item saved successfully");
            responseObject.add("cart", buildCartJson(session, cart));
        }
        return AppUtil.GSON.toJson(responseObject);
    }

    public String updateItem(long itemId, String jsonData, HttpServletRequest request) {
        CartItemDTO dto = AppUtil.GSON.fromJson(jsonData, CartItemDTO.class);
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        if (dto == null || dto.getQuantity() <= 0) {
            return error("Quantity must be greater than zero");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            CartItem item = session.get(CartItem.class, (int) itemId);
            if (item == null || item.getCart().getUser().getId() != userId) {
                tx.rollback();
                return error("Cart item not found");
            }
            item.setQuantity(dto.getQuantity());
            tx.commit();
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Cart item updated successfully");
            responseObject.add("cart", buildCartJson(session, item.getCart()));
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
            CartItem item = session.get(CartItem.class, (int) itemId);
            if (item == null || item.getCart().getUser().getId() != userId) {
                tx.rollback();
                return error("Cart item not found");
            }
            Cart cart = item.getCart();
            session.remove(item);
            tx.commit();
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Cart item removed successfully");
            responseObject.add("cart", buildCartJson(session, cart));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    public String clearCart(HttpServletRequest request) {
        Integer userId = CurrentUserUtil.getUserId(request);
        if (userId == null) {
            return error("User session not found");
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Cart cart = getOrCreateCart(session, userId);
            List<CartItem> items = session.createQuery("FROM CartItem ci WHERE ci.cart.id=:cartId", CartItem.class)
                    .setParameter("cartId", cart.getId())
                    .getResultList();
            for (CartItem item : items) {
                session.remove(item);
            }
            tx.commit();
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Cart cleared successfully");
            responseObject.add("cart", buildCartJson(session, cart));
            return AppUtil.GSON.toJson(responseObject);
        }
    }

    private Cart getOrCreateCart(Session session, int userId) {
        Cart cart = session.createQuery("FROM Cart c WHERE c.user.id=:userId", Cart.class)
                .setParameter("userId", userId)
                .getSingleResultOrNull();
        if (cart == null) {
            cart = new Cart();
            User user = session.get(User.class, userId);
            cart.setUser(user);
            Status status = session.createNamedQuery("Status.findByValue", Status.class)
                    .setParameter("value", String.valueOf(Status.Type.ACTIVE))
                    .getSingleResult();
            cart.setStatus(status);
            session.persist(cart);
        }
        return cart;
    }

    private JsonObject buildCartJson(Session session, Cart cart) {
        JsonObject cartObject = new JsonObject();
        cartObject.addProperty("id", cart.getId());
        cartObject.addProperty("status", cart.getStatus() == null ? null : cart.getStatus().getValue());
        JsonArray itemArray = new JsonArray();
        double total = 0;

        List<CartItem> items = session.createQuery("FROM CartItem ci WHERE ci.cart.id=:cartId", CartItem.class)
                .setParameter("cartId", cart.getId())
                .getResultList();
        for (CartItem item : items) {
            JsonObject itemObject = new JsonObject();
            itemObject.addProperty("id", item.getId());
            itemObject.addProperty("quantity", item.getQuantity());
            itemObject.addProperty("productId", item.getProduct().getId());
            itemObject.addProperty("productName", item.getProduct().getName());

            Stock stock = session.createQuery("FROM Stock s WHERE s.product.id=:productId ORDER BY s.price ASC", Stock.class)
                    .setParameter("productId", item.getProduct().getId())
                    .setMaxResults(1)
                    .getSingleResultOrNull();
            double price = stock == null ? 0 : stock.getPrice();
            itemObject.addProperty("price", price);
            itemObject.addProperty("subtotal", price * item.getQuantity());
            total += price * item.getQuantity();
            itemArray.add(itemObject);
        }

        cartObject.add("items", itemArray);
        cartObject.addProperty("total", total);
        return cartObject;
    }

    private String error(String message) {
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
