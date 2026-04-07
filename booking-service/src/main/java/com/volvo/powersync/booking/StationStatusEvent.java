package com.volvo.powersync.booking;

public record StationStatusEvent(
        String stationId,
        String stationName,
        String stationType,
        String status,
        String assignedVin,
        long timestampMs) {}
