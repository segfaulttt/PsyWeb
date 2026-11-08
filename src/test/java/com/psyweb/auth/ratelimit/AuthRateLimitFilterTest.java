package com.psyweb.auth.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.psyweb.auth.config.AuthRateLimitProperties;

class AuthRateLimitFilterTest {

	private AuthRateLimiter rateLimiter;
	private AuthRateLimitFilter filter;

	@BeforeEach
	void setUp() {
		rateLimiter = mock(AuthRateLimiter.class);

		AuthRateLimitProperties properties = new AuthRateLimitProperties(10, Duration.ofMinutes(1), 30,
				Duration.ofMinutes(1), 5, Duration.ofMinutes(15), 10, Duration.ofMinutes(15));

		filter = new AuthRateLimitFilter(rateLimiter, properties);
	}

	static Stream<Arguments> protectedEndpoints() {
		return Stream.of(Arguments.of("/api/auth/login", "login", new RateLimitPolicy(10, Duration.ofMinutes(1))),
				Arguments.of("/api/auth/refresh", "refresh", new RateLimitPolicy(30, Duration.ofMinutes(1))),
				Arguments.of("/api/auth/password-reset/request", "request",
						new RateLimitPolicy(5, Duration.ofMinutes(15))),
				Arguments.of("/api/auth/password-reset/confirm", "confirm",
						new RateLimitPolicy(10, Duration.ofMinutes(15))));
	}

	@ParameterizedTest
	@MethodSource("protectedEndpoints")
	void shouldApplyCorrectPolicyAndIndependentKey(String path, String endpoint, RateLimitPolicy expectedPolicy)
			throws Exception {

		MockHttpServletRequest request = request("POST", path);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		String expectedKey = "auth:" + endpoint + ":ip:192.0.2.10";

		when(rateLimiter.check(eq(expectedKey), eq(expectedPolicy)))
				.thenReturn(new RateLimitDecision(true, Duration.ZERO));

		filter.doFilter(request, response, chain);

		verify(rateLimiter).check(expectedKey, expectedPolicy);
		assertEquals(200, response.getStatus());
		assertEquals(request, chain.getRequest());
	}

	@ParameterizedTest
	@MethodSource("protectedEndpoints")
	void shouldReturn429AndRetryAfterForRejectedRequest(String path, String endpoint, RateLimitPolicy expectedPolicy)
			throws Exception {

		MockHttpServletRequest request = request("POST", path);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		String expectedKey = "auth:" + endpoint + ":ip:192.0.2.10";

		when(rateLimiter.check(eq(expectedKey), eq(expectedPolicy)))
				.thenReturn(new RateLimitDecision(false, Duration.ofMillis(1501)));

		filter.doFilter(request, response, chain);

		assertEquals(429, response.getStatus());
		assertEquals("2", response.getHeader(HttpHeaders.RETRY_AFTER));
		assertEquals("no-store", response.getHeader(HttpHeaders.CACHE_CONTROL));

		assertNull(chain.getRequest());

		verify(rateLimiter).check(expectedKey, expectedPolicy);
	}

	@Test
	void shouldSkipOtherPostEndpoints() throws Exception {
		MockHttpServletRequest request = request("POST", "/api/auth/logout");

		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, new MockHttpServletResponse(), chain);

		assertEquals(request, chain.getRequest());
		verifyNoInteractions(rateLimiter);
	}

	@Test
	void shouldSkipNonPostRequestsEvenOnProtectedPaths() throws Exception {

		MockHttpServletRequest request = request("GET", "/api/auth/password-reset/request");

		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, new MockHttpServletResponse(), chain);

		assertEquals(request, chain.getRequest());
		verifyNoInteractions(rateLimiter);
	}

	private static MockHttpServletRequest request(String method, String path) {

		MockHttpServletRequest request = new MockHttpServletRequest(method, path);

		request.setServletPath(path);
		request.setRemoteAddr("192.0.2.10");

		return request;
	}
}