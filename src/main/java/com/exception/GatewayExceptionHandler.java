package com.exception;

import java.net.ConnectException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import com.logging.AccessLogFilter;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @ExceptionHandler({ResourceAccessException.class, ConnectException.class})
    public ResponseEntity<Map<String, String>> upstreamDown(Exception ex, HttpServletRequest req) {
        req.setAttribute(AccessLogFilter.LOGGED, true);
        log.error("UPSTREAM_UNAVAILABLE {} {} cause={}", req.getMethod(), req.getRequestURI(),
                ex.getClass().getSimpleName(), ex);                 // stack trace, once
        return body(HttpStatus.SERVICE_UNAVAILABLE, "service_unavailable");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void denied(AccessDeniedException ex) { throw ex; }       // let security return 403

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception ex, HttpServletRequest req) {
        req.setAttribute(AccessLogFilter.LOGGED, true);
        log.error("GATEWAY_ERROR {} {}", req.getMethod(), req.getRequestURI(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error");
    }

    private ResponseEntity<Map<String, String>> body(HttpStatus status, String code) {
        String trace = MDC.get("traceId");
        return ResponseEntity.status(status).body(trace == null
                ? Map.of("error", code)
                : Map.of("error", code, "traceId", trace));        // the screen can show "ref ..."
    }
}