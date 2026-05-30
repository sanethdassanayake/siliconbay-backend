package com.hogger.siliconbay.controller.api;

import com.google.gson.JsonObject;
import com.hogger.siliconbay.annotation.IsUser;
import com.hogger.siliconbay.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/transactions")
@IsUser
@Produces(MediaType.APPLICATION_JSON)
public class TransactionController {
    private final TransactionService transactionService = new TransactionService();

    @GET
    public Response getMyTransactions(@Context HttpServletRequest request) {
        return Response.ok(transactionService.getMyTransactions(request)).build();
    }

    @GET
    @Path("/{id}")
    public Response getTransactionById(@PathParam("id") long id, @Context HttpServletRequest request) {
        return Response.ok(transactionService.getTransactionById(id, request)).build();
    }

    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTransactionStatus(@PathParam("id") long id, String jsonData, @Context HttpServletRequest request) {
        JsonObject body = com.hogger.siliconbay.util.AppUtil.GSON.fromJson(jsonData, JsonObject.class);
        String status = body == null || !body.has("status") ? null : body.get("status").getAsString();
        return Response.ok(transactionService.updateTransactionStatus(id, status, request)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTransaction(@PathParam("id") long id, @Context HttpServletRequest request) {
        return Response.ok(transactionService.deleteTransaction(id, request)).build();
    }
}
