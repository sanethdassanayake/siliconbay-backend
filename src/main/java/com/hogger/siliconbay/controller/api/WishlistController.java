package com.hogger.siliconbay.controller.api;

import com.hogger.siliconbay.annotation.IsUser;
import com.hogger.siliconbay.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/wishlist")
@IsUser
@Produces(MediaType.APPLICATION_JSON)
public class WishlistController {
    private final WishlistService wishlistService = new WishlistService();

    @GET
    public Response getWishlist(@Context HttpServletRequest request) {
        return Response.ok(wishlistService.getMyWishlist(request)).build();
    }

    @POST
    @Path("/items")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addItem(String jsonData, @Context HttpServletRequest request) {
        return Response.ok(wishlistService.addItem(jsonData, request)).build();
    }

    @PUT
    @Path("/items/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateItem(@PathParam("id") long id, String jsonData, @Context HttpServletRequest request) {
        return Response.ok(wishlistService.updateItem(id, jsonData, request)).build();
    }

    @DELETE
    @Path("/items/{id}")
    public Response removeItem(@PathParam("id") long id, @Context HttpServletRequest request) {
        return Response.ok(wishlistService.removeItem(id, request)).build();
    }

    @DELETE
    @Path("/clear")
    public Response clearWishlist(@Context HttpServletRequest request) {
        return Response.ok(wishlistService.clearWishlist(request)).build();
    }
}

