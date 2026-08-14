package com.hogger.siliconbay.middleware;

import com.hogger.siliconbay.annotation.IsAdmin;
import com.hogger.siliconbay.util.JwtUtil;

import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION)
@IsAdmin
public class AdminFilter implements ContainerRequestFilter {
    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext ctx) {
        // Allow preflight requests
        if ("OPTIONS".equalsIgnoreCase(ctx.getMethod())) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session != null && "ADMIN".equals(session.getAttribute("role"))) {
            return;
        }

        String authorizationHeader = ctx.getHeaderString("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7).trim();
            try {
                if (!JwtUtil.isTokenExpired(token) && "ADMIN".equalsIgnoreCase(JwtUtil.getRoleFromToken(token))) {
                    HttpSession newSession = request.getSession(true);
                    newSession.setAttribute("user", JwtUtil.getEmailFromToken(token));
                    newSession.setAttribute("userId", JwtUtil.getUserIdFromToken(token).intValue());
                    newSession.setAttribute("role", JwtUtil.getRoleFromToken(token));
                    return;
                }
            } catch (RuntimeException ignored) {
                // fall through to unauthorized response
            }
        }

        ctx.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"ADMIN_REQUIRED\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build()
        );
    }
}

