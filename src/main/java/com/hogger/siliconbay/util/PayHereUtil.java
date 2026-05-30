package com.hogger.siliconbay.util;

public final class PayHereUtil {
    private PayHereUtil() {}

    public static String env(String key, String fallback) {
        String v = System.getenv(key);
        if (v != null && !v.isEmpty()) return v;
        v = System.getProperty(key);
        if (v != null && !v.isEmpty()) return v;
        return fallback;
    }

    public static String getMerchantId() {
        return env("PAYHERE_MERCHANT_ID", "");
    }

    public static String getMerchantSecret() {
        return env("PAYHERE_MERCHANT_SECRET", "");
    }

    public static String getAppUrl() {
        return env("APP_URL", "http://localhost:8080/siliconbay");
    }
}

