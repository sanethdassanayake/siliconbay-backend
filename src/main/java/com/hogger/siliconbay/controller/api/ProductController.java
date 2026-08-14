package com.hogger.siliconbay.controller.api;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import com.hogger.siliconbay.annotation.IsUser;
import com.hogger.siliconbay.service.ProductService;
import com.hogger.siliconbay.dto.ReviewDTO;
import com.hogger.siliconbay.util.CurrentUserUtil;
import com.hogger.siliconbay.util.AppUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductController {
    private final ProductService productService = new ProductService();

    @Path("/featured")
    @GET
    public Response getFeatured() {
        String responseJson = productService.getFeaturedProducts();
        return Response.ok().entity(responseJson).build();
    }

    @Path("/best-sellers")
    @GET
    public Response getBestSellers() {
        String responseJson = productService.getBestSellingProducts();
        return Response.ok().entity(responseJson).build();
    }

    @Path("/{id}")
    @GET
    public Response getById(@PathParam("id") int id) {
        String responseJson = productService.getProductById(id);
        return Response.ok().entity(responseJson).build();
    }

    @Path("/mine")
    @GET
    @IsUser
    public Response getMyProducts(@Context HttpServletRequest request) {
        String responseJson = productService.getMyProducts(request);
        return Response.ok().entity(responseJson).build();
    }

    @POST
    @IsUser
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createProduct(FormDataMultiPart formData, @Context HttpServletRequest request) {
        String responseJson = productService.createProduct(formData, request);
        return Response.ok().entity(responseJson).build();
    }

    @PUT
    @Path("/{id}")
    @IsUser
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateProduct(@PathParam("id") int id, FormDataMultiPart formData, @Context HttpServletRequest request) {
        String responseJson = productService.updateProduct(id, formData, request);
        return Response.ok().entity(responseJson).build();
    }
}
