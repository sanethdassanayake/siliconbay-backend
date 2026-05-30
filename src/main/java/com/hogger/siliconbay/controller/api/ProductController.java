package com.hogger.siliconbay.controller.api;

import com.hogger.siliconbay.service.ProductService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/products")
public class ProductController {
    @Path("/featured")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFeatured() {
        String responseJson = new ProductService().getFeaturedProducts();
        return Response.ok().entity(responseJson).build();
    }

    @Path("/best-sellers")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBestSellers() {
        String responseJson = new ProductService().getBestSellingProducts();
        return Response.ok().entity(responseJson).build();
    }

    @Path("/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") int id) {
        String responseJson = new ProductService().getProductById(id);
        return Response.ok().entity(responseJson).build();
    }
}
