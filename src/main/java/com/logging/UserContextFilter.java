package com.logging;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UserContextFilter extends OncePerRequestFilter {

    public static final String USER_ATTRIBUTE = "auth.userId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwt) {
                String user = jwt.getToken().getSubject();
                req.setAttribute(USER_ATTRIBUTE, user);   // read later by the access log
                MDC.put("userId", user);                  // appears on every line of this request
            }
            chain.doFilter(req, res);
        } finally {
            MDC.remove("userId");                         // threads are reused, so never leave it behind
        }
    }
}