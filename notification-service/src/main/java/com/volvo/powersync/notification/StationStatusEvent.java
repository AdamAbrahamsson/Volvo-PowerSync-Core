package com.volvo.powersync.notification;

public record StationStatusEvent(
        String stationId,
        String stationName,
        String stationType,
        String status,
        String assignedVin,
        long timestampMs) {}
