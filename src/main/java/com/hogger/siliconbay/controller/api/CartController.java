package com.hogger.siliconbay.controller.api;

import com.hogger.siliconbay.annotation.IsUser;
import com.hogger.siliconbay.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/cart")
@IsUser
@Produces(MediaType.APPLICATION_JSON)
public class CartController {
    private final CartService cartService = new CartService();

    @GET
    public Response getCart(@Context HttpServletRequest request) {
        return Response.ok(cartService.getMyCart(request)).build();
    }

    @POST
    @Path("/items")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addItem(String jsonData, @Context HttpServletRequest request) {
        return Response.ok(cartService.addItem(jsonData, request)).build();
    }

    @PUT
    @Path("/items/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateItem(@PathParam("id") long id, String jsonData, @Context HttpServletRequest request) {
        return Response.ok(cartService.updateItem(id, jsonData, request)).build();
    }

    @DELETE
    @Path("/items/{id}")
    public Response removeItem(@PathParam("id") long id, @Context HttpServletRequest request) {
        return Response.ok(cartService.removeItem(id, request)).build();
    }

    @DELETE
    @Path("/clear")
    public Response clearCart(@Context HttpServletRequest request) {
        return Response.ok(cartService.clearCart(request)).build();
    }
}

