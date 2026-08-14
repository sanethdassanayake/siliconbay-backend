package com.hogger.siliconbay.controller.api;

import java.io.IOException;
import java.nio.file.Files;

import com.hogger.siliconbay.util.ProductImageStorage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
@Path("/uploads/products")
public class UploadController {
    @GET
    @Path("/{fileName: .+}")
    @Produces(MediaType.WILDCARD)
    public Response getProductImage(@PathParam("fileName") String fileName) {
        try {
            java.nio.file.Path imagePath = ProductImageStorage.resolveFile(fileName);
            if (!Files.exists(imagePath)) {
                throw new NotFoundException("Image not found");
            }

            String contentType = Files.probeContentType(imagePath);
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }

            StreamingOutput stream = output -> Files.copy(imagePath, output);
            return Response.ok(stream, contentType).build();
        } catch (IOException e) {
            throw new NotFoundException("Image not found");
        }
    }
}