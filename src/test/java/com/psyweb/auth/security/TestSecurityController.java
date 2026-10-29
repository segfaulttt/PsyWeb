package com.psyweb.auth.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestSecurityController {

	@GetMapping("/api/admin/security-test")
	public String adminEndpoint() {
		return "OK";
	}
	
	@PostMapping("/api/admin/security-test")
	public String adminPostEndpoint() {
	    return "POST_OK";
	}
}