package com.psyweb.auth.web.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpStatus;

import com.psyweb.auth.service.RegistrationService;
import com.psyweb.auth.web.dto.request.ClientRegistrationRequest;
import com.psyweb.auth.web.dto.request.SpecialistRegistrationRequest;
import com.psyweb.auth.web.dto.response.RegistrationResponse;
import com.psyweb.auth.web.mapper.RegistrationWebMapper;
import com.psyweb.client.domain.Client;
import com.psyweb.specialist.domain.Specialist;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final RegistrationService registrationService;
	private final RegistrationWebMapper registrationWebMapper;

	public AuthController(RegistrationService registrationService, RegistrationWebMapper registrationWebMapper) {
		this.registrationService = registrationService;
		this.registrationWebMapper = registrationWebMapper;
	}

	@PostMapping("/register/client")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistrationResponse registerClient(@Valid @RequestBody ClientRegistrationRequest request) {
		Client client = registrationService.registerClient(request.email(), request.password(), request.firstName(),
				request.lastName());
		return registrationWebMapper.toResponse(client);
	}

	@PostMapping("/register/specialist")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistrationResponse registerSpecialist(@Valid @RequestBody SpecialistRegistrationRequest request) {
		Specialist specialist = registrationService.registerSpecialist(request.email(), request.password(),
				request.firstName(), request.lastName());
		return registrationWebMapper.toResponse(specialist);
	}
}
