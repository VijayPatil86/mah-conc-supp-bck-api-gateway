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
@Order(Ordered.HIGHEST_PRECEDENCE + 10)   // outside Spring Security, so 401 and 403 are logged too
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger access = LoggerFactory.getLogger("access");
    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);
    private static final long SLOW_MS = 3000;
    private static final Set<String> SKIP = Set.of("/actuator/health", "/actuator/health/liveness",
                                                   "/actuator/health/readiness");

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
            res.setHeader("X-Trace-Id", traceId);          // lets support quote it from the browser
        }

        Throwable failure = null;
        try {
            chain.doFilter(req, res);
        } catch (ServletException | IOException | RuntimeException e) {
            failure = e;
            throw e;
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            int status = (failure != null && res.getStatus() < 400) ? 500 : res.getStatus();
            Object user = req.getAttribute(UserContextFilter.USER_ATTRIBUTE);

            // path only: the query string can hold search text and personal data
            String line = String.format("%s %s -> %d %dms user=%s ip=%s trace=%s",
                    req.getMethod(), req.getRequestURI(), status, ms,
                    user == null ? "-" : user, clientIp(req), traceId == null ? "-" : traceId);

            access.info(line);

            if (status >= 500) {
                log.error("Request failed: {}", line);
            } else if (status == 401 || status == 403) {
                log.warn("Request rejected: {}", line);
            } else if (ms > SLOW_MS) {
                log.warn("Slow request: {}", line);
            }
        }
    }

    private String clientIp(HttpServletRequest req) {
        return req.getRemoteAddr();     // behind a proxy, configure forward-headers-strategy instead
    }
}