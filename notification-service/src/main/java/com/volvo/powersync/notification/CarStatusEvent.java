package com.volvo.powersync.notification;

public record CarStatusEvent(
        String vin,
        int batteryPercentage,
        String status,
        String assignedChargingStationId,
        boolean vipEligible,
        long timestampMs) {}
