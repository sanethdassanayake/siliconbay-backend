package com.hogger.siliconbay.controller.api;

import com.hogger.siliconbay.annotation.IsUser;
import com.hogger.siliconbay.service.SellerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/sellers")
@IsUser
@Produces(MediaType.APPLICATION_JSON)
public class SellerController {
    private final SellerService sellerService = new SellerService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createProfile(String jsonData, @Context HttpServletRequest request) {
        return Response.ok(sellerService.createSellerProfile(jsonData, request)).build();
    }

    @GET
    @Path("/me")
    public Response getMyProfile(@Context HttpServletRequest request) {
        return Response.ok(sellerService.getMySellerProfile(request)).build();
    }

    @PUT
    @Path("/me")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMyProfile(String jsonData, @Context HttpServletRequest request) {
        return Response.ok(sellerService.updateMySellerProfile(jsonData, request)).build();
    }
}

