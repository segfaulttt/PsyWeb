package com.psyweb.auth.web.mapper;

import org.springframework.stereotype.Component;

import com.psyweb.auth.web.dto.response.RegistrationResponse;
import com.psyweb.client.domain.Client;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.UserRole;

@Component
public class RegistrationWebMapper {
	public RegistrationResponse toResponse(Client client) {
		return new RegistrationResponse(client.getId(), UserRole.CLIENT);
	}
	
	public RegistrationResponse toResponse(Specialist specialist) {
		return new RegistrationResponse(specialist.getId(), UserRole.SPECIALIST);
	}
}
