
package com.psyweb.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.psyweb.testsupport.PostgreSQLIntegrationTest;

@SpringBootTest(properties = { "psyweb.security.cors.allowed-origins[0]=http://localhost:3000" })
@AutoConfigureMockMvc
class CorsIntegrationTest extends PostgreSQLIntegrationTest{

	private static final String API_PATH = "/api/auth/register/client";
	private static final String ALLOWED_ORIGIN = "http://localhost:3000";
	private static final String DISALLOWED_ORIGIN = "https://evil.example";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldAllowPreflightFromConfiguredOrigin() throws Exception {
		mockMvc.perform(options(API_PATH).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
				.andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
				.andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
	}

	@Test
	void shouldRejectPreflightFromDisallowedOrigin() throws Exception {
		mockMvc.perform(options(API_PATH).header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")).andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void shouldRejectWildcardOrigin() throws Exception {
		mockMvc.perform(options(API_PATH).header(HttpHeaders.ORIGIN, "https://unknown.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")).andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void shouldAllowConfiguredRequestHeaders() throws Exception {
		mockMvc.perform(options(API_PATH).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type,X-CSRF-TOKEN"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
	}

	@Test
	void shouldRejectUnconfiguredRequestHeader() throws Exception {
		mockMvc.perform(options(API_PATH).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Unapproved-Header"))
				.andExpect(status().isForbidden());
	}

	@Test
	void shouldRejectUnconfiguredRequestMethod() throws Exception {
		mockMvc.perform(options(API_PATH).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "TRACE")).andExpect(status().isForbidden());
	}

	@Test
	void shouldNotUseWildcardAllowOrigin() throws Exception {
		mockMvc.perform(options(API_PATH).header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")).andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}
}