package com.psyweb.schedule.service.model;

import java.time.ZoneId;

public record TimeZoneChangePreview(
        ZoneId currentZoneId,
        ZoneId newZoneId,
        int affectedBookingsCount,
        int affectedReservationsCount
) {
    public boolean requiresAcknowledgement() {
        return affectedBookingsCount > 0 || affectedReservationsCount > 0;
    }
}
