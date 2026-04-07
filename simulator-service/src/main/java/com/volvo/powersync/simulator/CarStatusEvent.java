package com.volvo.powersync.simulator;

public record CarStatusEvent(
        String vin,
        int batteryPercentage,
        String status,
        String assignedChargingStationId,
        boolean vipEligible,
        long timestampMs) {}
