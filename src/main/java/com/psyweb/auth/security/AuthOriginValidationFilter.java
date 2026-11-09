package com.psyweb.auth.security;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.psyweb.auth.config.CorsProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthOriginValidationFilter extends OncePerRequestFilter {

	private final CorsProperties corsProperties;

	public AuthOriginValidationFilter(CorsProperties corsProperties) {
		this.corsProperties = corsProperties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String origin = request.getHeader("Origin");

		if (origin == null || !corsProperties.allowedOrigins().contains(origin)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden origin");
			return;
		}

		filterChain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!"POST".equals(request.getMethod())) {
			return true;
		}

		return switch (request.getServletPath()) {
		case "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/auth/logout-all" -> false;
		default -> true;
		};
	}
}
