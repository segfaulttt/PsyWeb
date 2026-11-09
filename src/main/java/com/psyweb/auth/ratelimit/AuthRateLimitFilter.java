package com.psyweb.auth.ratelimit;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import com.psyweb.auth.config.AuthRateLimitProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

public class AuthRateLimitFilter extends OncePerRequestFilter {

	private final AuthRateLimiter rateLimiter;
	private final AuthRateLimitProperties properties;

	public AuthRateLimitFilter(AuthRateLimiter rateLimiter, AuthRateLimitProperties properties) {
		this.rateLimiter = rateLimiter;
		this.properties = properties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!"POST".equals(request.getMethod())) {
			return true;
		}

		return !request.getServletPath().equals("/api/auth/login")
		        && !request.getServletPath().equals("/api/auth/refresh")
		        && !request.getServletPath().equals("/api/auth/password-reset/request")
		        && !request.getServletPath().equals("/api/auth/password-reset/confirm");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		RateLimitPolicy policy;
		String endpoint;
		switch (request.getServletPath()) {
		case "/api/auth/login" :
			policy = properties.loginPolicy();
			endpoint = "login";
			break;
		case "/api/auth/refresh" :
			policy = properties.refreshPolicy();
			endpoint = "refresh";
			break;
		case "/api/auth/password-reset/request" :
			policy = properties.passwordResetRequestPolicy();
			endpoint = "request";
			break;
		case "/api/auth/password-reset/confirm" :
			policy = properties.passwordResetConfirmPolicy();
			endpoint = "confirm";
			break;
		default:
		    filterChain.doFilter(request, response);
		    return;	
		}

		String key = "auth:" + endpoint + ":ip:" + request.getRemoteAddr();

		RateLimitDecision decision = rateLimiter.check(key, policy);

		if (!decision.allowed()) {
			long seconds = decision.retryAfter().getSeconds();

			long retryAfterSeconds = seconds
			        + (decision.retryAfter().getNano() > 0 ? 1 : 0);

			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
			response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
			return;
		}

		filterChain.doFilter(request, response);
	}
}