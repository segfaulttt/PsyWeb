package com.psyweb.common.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ValidationApiError(String code, String message, String path, Instant timestamp,
		Map<String, List<String>> fieldErrors) {

}
