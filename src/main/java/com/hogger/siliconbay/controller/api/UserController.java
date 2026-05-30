package com.hogger.siliconbay.controller.api;

import com.google.gson.JsonObject;
import com.hogger.siliconbay.annotation.IsUser;
import com.hogger.siliconbay.dto.UserDTO;
import com.hogger.siliconbay.service.UserService;
import com.hogger.siliconbay.util.AppUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
public class UserController {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNewAccount(String jsonData) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().addNewUser(userDTO);
        JsonObject responseObject = AppUtil.GSON.fromJson(responseJson, JsonObject.class);
        boolean status = responseObject != null && responseObject.has("status") && responseObject.get("status").getAsBoolean();
        return Response.status(status ? Response.Status.OK : Response.Status.BAD_REQUEST).entity(responseJson).build();
    }

    @Path("/login")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String jsonData, @Context HttpServletRequest request) {
        UserDTO userDTO = AppUtil.GSON.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().userLogin(userDTO, request);
        return Response.ok().entity(responseJson).build();
    }

    @IsUser
    @Path("/logout")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest request) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }
        return Response.ok().entity("{\"status\":true,\"message\":\"Logout successful\"}").build();
    }
}
