package com.psyweb.auth.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

import com.psyweb.common.web.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

public class SecurityErrorResponseWriter {

	private final ObjectMapper objectMapper;
	private final Clock clock;

	public SecurityErrorResponseWriter(ObjectMapper objectMapper, Clock clock) {
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	public void write(int httpStatus,
	        String errorCode,
	        String message,
	        HttpServletRequest request,
	        HttpServletResponse response) throws IOException {
		
		response.setCharacterEncoding("UTF-8");
		
		response.setStatus(httpStatus);
		response.setContentType("application/json");

		ApiError error = new ApiError(errorCode, message, request.getRequestURI(), Instant.now(clock));
		objectMapper.writeValue(response.getOutputStream(), error);
	}
}