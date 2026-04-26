package com.volvo.powersync.events;

public record StationStatusEvent(
        String stationId,
        String stationName,
        String stationType,
        String status,
        String assignedVin,
        long timestampMs) {}
