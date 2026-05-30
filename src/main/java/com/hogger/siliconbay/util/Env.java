package com.hogger.siliconbay.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Env {
    private static final Properties APP_PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = Env.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (inputStream == null) {
                throw new IOException("app.properties resource not found");
            }
            APP_PROPERTIES.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Application properties loading failed: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return APP_PROPERTIES.getProperty(key);
    }

    public static Properties getAppProperties() {
        return APP_PROPERTIES;
    }
}
