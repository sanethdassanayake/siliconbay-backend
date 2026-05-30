package com.hogger.siliconbay.config;

import org.glassfish.jersey.server.ResourceConfig;

// Application configuration class for Jersey RESTful web services
public class AppConfig extends ResourceConfig {
    public AppConfig() {
        packages("com.hogger.siliconbay.controller"); // REST API classes
        packages("com.hogger.siliconbay.middleware"); // Middleware classes (e.g., filters, interceptors)

        // Enable multipart feature for file uploads
        // register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    }
}
