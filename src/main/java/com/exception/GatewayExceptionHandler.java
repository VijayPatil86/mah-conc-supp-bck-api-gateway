package com.exception;

import java.net.ConnectException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class GatewayExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

	@ExceptionHandler({ResourceAccessException.class, ConnectException.class})
	public ResponseEntity<Map<String, String>> upstreamDown(ResourceAccessException ex) {
		log.error("Upstream service unavailable", ex); 
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("error", "Upstream service is down. Please try again later."));
	}
}
