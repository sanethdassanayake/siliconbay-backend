package com.hogger.siliconbay.controller.api;

import org.hibernate.Session;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hogger.siliconbay.entity.Category;
import com.hogger.siliconbay.util.AppUtil;
import com.hogger.siliconbay.util.HibernateUtil;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryController {
    @GET
    public Response getCategories() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            JsonArray categories = new JsonArray();
            for (Category category : session.createQuery("FROM Category c ORDER BY c.name ASC", Category.class).getResultList()) {
                JsonObject categoryObject = new JsonObject();
                categoryObject.addProperty("id", category.getId());
                categoryObject.addProperty("name", category.getName());
                categories.add(categoryObject);
            }

            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("status", true);
            responseObject.addProperty("message", "Categories found successfully");
            responseObject.add("categories", categories);
            return Response.ok(AppUtil.GSON.toJson(responseObject)).build();
        }
    }
}