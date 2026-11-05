package com.logging;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)     // outside Spring Security, so 401 and 403 are seen
public class AccessLogFilter extends OncePerRequestFilter {

    public static final String LOGGED = "gateway.failure.logged";

    private static final Logger access = LoggerFactory.getLogger("access");
    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final long SLOW_MS = 3000;
    private static final Set<String> SKIP = Set.of("/actuator/health",
            "/actuator/health/liveness", "/actuator/health/readiness");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SKIP.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        long start = System.nanoTime();
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            res.setHeader("X-Trace-Id", traceId);
        }

        try {
            chain.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            int status = res.getStatus();
            Object user = req.getAttribute(UserContextFilter.USER_ATTRIBUTE);
            String route = req.getMethod() + " " + req.getRequestURI();     // path only, never the query string

            access.info("REQUEST {} status={} ms={} user={} ip={} trace={}",
                    route, status, ms, user == null ? "-" : user, req.getRemoteAddr(),
                    traceId == null ? "-" : traceId);

            if (status == 401 || status == 403) {
                log.warn("AUTH_REJECTED {} status={} ip={}", route, status, req.getRemoteAddr());
            } else if (status == 404) {
                log.warn("ROUTE_NOT_FOUND {} ip={}", route, req.getRemoteAddr());
            } else if (status >= 500 && req.getAttribute(LOGGED) == null) {
                // a service answered with a 5xx and nothing in the gateway threw
                log.error("UPSTREAM_ERROR {} status={} ms={}", route, status, ms);
            } else if (ms > SLOW_MS) {
                log.warn("REQUEST_SLOW {} ms={}", route, ms);
            }
        }
    }
}