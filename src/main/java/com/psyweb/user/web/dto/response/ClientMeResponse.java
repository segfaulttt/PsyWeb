package com.psyweb.user.web.dto.response;

import com.psyweb.client.web.dto.response.ClientProfileResponse;

public record ClientMeResponse(UserAccountResponse account, ClientProfileResponse profile) implements MeResponse {
}
