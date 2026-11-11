package com.psyweb.user.web.dto.response;

import com.psyweb.specialist.web.dto.response.SpecialistProfileResponse;

public record SpecialistMeResponse (UserAccountResponse account, SpecialistProfileResponse profile) implements MeResponse {
}
