package com.psyweb.auth.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiAccessDeniedHandler implements AccessDeniedHandler{

	private final SecurityErrorResponseWriter responseWriter;

	public ApiAccessDeniedHandler(SecurityErrorResponseWriter responseWriter) {
		this.responseWriter = responseWriter;
	}
	
	@Override
	public void handle(
			HttpServletRequest request,
	        HttpServletResponse response,
	        AccessDeniedException accessDeniedException) throws IOException {
		responseWriter.write(HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "Access denied", request, response);
	}
}
