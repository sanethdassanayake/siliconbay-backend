package com.hogger.siliconbay.controller.api;

import com.google.gson.Gson;
import com.hogger.siliconbay.dto.UserDTO;
import com.hogger.siliconbay.service.UserService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/verify")
@Produces(MediaType.APPLICATION_JSON)
public class VerificationController {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response verifyUserAccount(String jsonData) {
        Gson gson = new Gson();
        UserDTO userDTO = gson.fromJson(jsonData, UserDTO.class);
        String responseJson = new UserService().verifyUserAccount(userDTO);
        return Response.ok().entity(responseJson).build();
    }

    @GET
    public Response verifyUserAccount(@QueryParam("email") String email, @QueryParam("verificationCode") String verificationCode) {
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail(email);
        userDTO.setVerificationCode(verificationCode);
        String responseJson = new UserService().verifyUserAccount(userDTO);
        return Response.ok().entity(responseJson).build();
    }
}
