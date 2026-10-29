
package com.psyweb.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.psyweb.testsupport.PostgreSQLIntegrationTest;

@SpringBootTest(properties = { "server.forward-headers-strategy=framework" })
@AutoConfigureMockMvc
class ForwardedHeadersIntegrationTest extends PostgreSQLIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldRecognizeForwardedHttpsAndReturnHsts() throws Exception {
		mockMvc.perform(get("/api/admin/security-test").secure(false).header("X-Forwarded-Proto", "https")
				.header("X-Forwarded-Host", "example.com").header("X-Forwarded-Port", "443"))
				.andExpect(status().isUnauthorized()).andExpect(header().exists("Strict-Transport-Security"));
	}
}