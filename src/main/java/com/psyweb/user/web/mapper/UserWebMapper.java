package com.psyweb.user.web.mapper;

import org.springframework.stereotype.Component;

import com.psyweb.user.web.dto.response.UserAccountResponse;
import com.psyweb.user.domain.User;

@Component
public class UserWebMapper {

	public UserAccountResponse toAccountResponse(User user) {
		return new UserAccountResponse(user.getId(), user.getEmail(), user.getRole());
	}
}