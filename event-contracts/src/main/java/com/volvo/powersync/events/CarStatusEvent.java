package com.volvo.powersync.events;

public record CarStatusEvent(
        String vin,
        int batteryPercentage,
        String status,
        String assignedChargingStationId,
        boolean vipEligible,
        long timestampMs) {}
