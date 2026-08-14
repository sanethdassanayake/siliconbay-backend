package com.hogger.siliconbay;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.servlet.ServletContainer;

import com.hogger.siliconbay.config.AppConfig;
import com.hogger.siliconbay.listener.ContextPathListener;
import com.hogger.siliconbay.util.DataSeeder;

public class Main {

    // In here we configure the embedded Tomcat server to run the SiliconBay application

    // Constants for server configuration
    private static final int SERVER_PORT = 8080;
    private static final String CONTEXT_PATH = "/siliconbay";

    public static void main(String[] args) {
        try {
            DataSeeder.seed();

            // Initialize and configure the embedded Tomcat server
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(SERVER_PORT);
            tomcat.getConnector();

            // Webapp directory is not needed for API-only apps
            Context context = tomcat.addContext(CONTEXT_PATH, null);
            // Ensure the webapp classloader can see application classes compiled in target/classes
            context.setParentClassLoader(Main.class.getClassLoader());

            Tomcat.addServlet(context, "JerseyServlet", new ServletContainer(new AppConfig()));
            context.addServletMappingDecoded("/api/*", "JerseyServlet");

            context.addApplicationListener(ContextPathListener.class.getName());

            tomcat.start();
            System.out.println("App URL: http://localhost:" + SERVER_PORT + CONTEXT_PATH);
            tomcat.getServer().await();

        } catch (LifecycleException e) {
            throw new RuntimeException("Tomcat Embedded Server loading failed: " + e.getMessage());
        }
    }
}
