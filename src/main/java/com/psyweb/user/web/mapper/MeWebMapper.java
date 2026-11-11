package com.psyweb.user.web.mapper;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.psyweb.client.domain.Client;
import com.psyweb.client.web.dto.response.ClientProfileResponse;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.web.dto.response.SpecialistProfileResponse;
import com.psyweb.user.domain.User;
import com.psyweb.user.exception.InvalidUserDataException;
import com.psyweb.user.service.dto.CurrentClientProfile;
import com.psyweb.user.service.dto.CurrentSpecialistProfile;
import com.psyweb.user.service.dto.CurrentUserProfile;
import com.psyweb.user.service.dto.UpdateCurrentProfileCommand;
import com.psyweb.user.web.dto.response.ClientMeResponse;
import com.psyweb.user.web.dto.response.MeResponse;
import com.psyweb.user.web.dto.response.SpecialistMeResponse;
import com.psyweb.user.web.dto.response.UserAccountResponse;

import tools.jackson.databind.JsonNode;

@Component
public class MeWebMapper {
	public MeResponse toResponse(CurrentUserProfile profile) {
		if (profile instanceof CurrentClientProfile client) {
			return toClientMeResponse(client);
		} else if (profile instanceof CurrentSpecialistProfile spec) {
			return toSpecialistMeResponse(spec);
		} else {
			throw new IllegalStateException();
		}
	}

	private UserAccountResponse toUserAccountResponse(User user) {
		return new UserAccountResponse(user.getId(), user.getEmail(), user.getRole());
	}

	public ClientMeResponse toClientMeResponse(CurrentClientProfile clientProfile) {
		User user = clientProfile.user();
		Client client = clientProfile.client();
		return new ClientMeResponse(toUserAccountResponse(user),
				new ClientProfileResponse(client.getId(), client.getFirstName(), client.getLastName()));
	}

	public SpecialistMeResponse toSpecialistMeResponse(CurrentSpecialistProfile specialistProfile) {
		User user = specialistProfile.user();
		Specialist specialist = specialistProfile.specialist();

		return new SpecialistMeResponse(toUserAccountResponse(user), new SpecialistProfileResponse(specialist.getId(),
				specialist.getFirstName(), specialist.getLastName(), specialist.getApprovalStatus()));
	}

	public UpdateCurrentProfileCommand toUpdateCommand(JsonNode json) {
		if (json == null || !json.isObject()) {
		    throw new InvalidUserDataException("Profile update body must be a JSON object");
		}

		if (json.isEmpty()) {
		    throw new InvalidUserDataException("Profile update body cannot be empty");
		}
		for (Map.Entry<String, JsonNode> entry : json.properties()) {
		    String field = entry.getKey();
		    JsonNode value = entry.getValue();

		    if (!field.equals("firstName") && !field.equals("lastName")) {
		        throw new InvalidUserDataException("Unknown profile field: " + field);
		    }
		    if (!value.isString()) {
		        throw new InvalidUserDataException(
		                "Profile field must be a string: " + field
		        );
		    }
		}
		String firstName = json.has("firstName")
		        ? json.get("firstName").asString()
		        : null;

		String lastName = json.has("lastName")
		        ? json.get("lastName").asString()
		        : null;

		return new UpdateCurrentProfileCommand(firstName, lastName);
	}
}
