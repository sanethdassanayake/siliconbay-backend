package com.hogger.siliconbay.controller.api;

import com.hogger.siliconbay.annotation.IsUser;
import com.hogger.siliconbay.service.ReturnService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/returns")
@IsUser
@Produces(MediaType.APPLICATION_JSON)
public class ReturnController {
    private final ReturnService returnService = new ReturnService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createReturn(String jsonData, @Context HttpServletRequest request) {
        return Response.ok(returnService.createReturn(jsonData, request)).build();
    }

    @GET
    public Response getMyReturns(@Context HttpServletRequest request) {
        return Response.ok(returnService.getMyReturns(request)).build();
    }

    @GET
    @Path("/{id}")
    public Response getReturnById(@PathParam("id") long id, @Context HttpServletRequest request) {
        return Response.ok(returnService.getReturnById(id, request)).build();
    }

    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateReturnStatus(@PathParam("id") long id, String jsonData, @Context HttpServletRequest request) {
        return Response.ok(returnService.updateReturnStatus(id, jsonData, request)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteReturn(@PathParam("id") long id, @Context HttpServletRequest request) {
        return Response.ok(returnService.deleteReturn(id, request)).build();
    }
}
