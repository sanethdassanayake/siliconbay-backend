package com.hogger.siliconbay.controller.api;

import com.google.gson.JsonObject;
import com.hogger.siliconbay.annotation.IsUser;
import com.hogger.siliconbay.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orders")
@IsUser
@Produces(MediaType.APPLICATION_JSON)
public class OrderController {
    private final OrderService orderService = new OrderService();

    @POST
    @Path("/checkout")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkout(String jsonData, @Context HttpServletRequest request) {
        return Response.ok(orderService.checkout(jsonData, request)).build();
    }

    @GET
    public Response getMyOrders(@Context HttpServletRequest request) {
        return Response.ok(orderService.getMyOrders(request)).build();
    }

    @GET
    @Path("/{id}")
    public Response getOrderById(@PathParam("id") int id, @Context HttpServletRequest request) {
        return Response.ok(orderService.getOrderById(id, request)).build();
    }

    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOrderStatus(@PathParam("id") int id, String jsonData, @Context HttpServletRequest request) {
        JsonObject body = com.hogger.siliconbay.util.AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        String status = body == null || !body.has("status") ? null : body.get("status").getAsString();
        return Response.ok(orderService.updateOrderStatus(id, status, request)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteOrder(@PathParam("id") int id, @Context HttpServletRequest request) {
        return Response.ok(orderService.deleteOrder(id, request)).build();
    }
}
