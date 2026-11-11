package com.psyweb.user.service.dto;

import com.psyweb.specialist.domain.Specialist;
import com.psyweb.user.domain.User;

public record CurrentSpecialistProfile (User user, Specialist specialist) implements CurrentUserProfile {

}
