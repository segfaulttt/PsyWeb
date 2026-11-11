package com.psyweb.user.web.mapper;

import org.springframework.stereotype.Component;

import com.psyweb.client.domain.Client;
import com.psyweb.client.web.dto.response.ClientProfileResponse;
import com.psyweb.specialist.domain.Specialist;
import com.psyweb.specialist.web.dto.response.SpecialistProfileResponse;
import com.psyweb.user.domain.User;
import com.psyweb.user.service.dto.CurrentClientProfile;
import com.psyweb.user.service.dto.CurrentSpecialistProfile;
import com.psyweb.user.service.dto.CurrentUserProfile;
import com.psyweb.user.web.dto.response.ClientMeResponse;
import com.psyweb.user.web.dto.response.MeResponse;
import com.psyweb.user.web.dto.response.SpecialistMeResponse;
import com.psyweb.user.web.dto.response.UserAccountResponse;

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
}
