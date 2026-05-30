package com.hogger.siliconbay.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class CurrentUserUtil {
    private CurrentUserUtil() {
    }

    public static Integer getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute("userId");
        if (userId instanceof Integer integer) {
            return integer;
        }
        if (userId instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    public static String getRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object role = session.getAttribute("role");
        return role == null ? null : String.valueOf(role);
    }
}

