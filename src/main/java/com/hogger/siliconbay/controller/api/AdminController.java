package com.hogger.siliconbay.controller.api;

import com.hogger.siliconbay.annotation.IsAdmin;
import com.hogger.siliconbay.service.AdminService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin")
@IsAdmin
@Produces(MediaType.APPLICATION_JSON)
public class AdminController {
    private final AdminService adminService = new AdminService();

    @GET
    @Path("/users")
    public Response listUsers() {
        return Response.ok(adminService.listUsers()).build();
    }

    @PUT
    @Path("/users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(@PathParam("id") int id, String jsonData) {
        return Response.ok(adminService.updateUser(id, jsonData)).build();
    }

    @GET
    @Path("/sellers")
    public Response listSellers() {
        return Response.ok(adminService.listSellers()).build();
    }

    @PUT
    @Path("/sellers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateSeller(@PathParam("id") int id, String jsonData) {
        return Response.ok(adminService.updateSeller(id, jsonData)).build();
    }
}

