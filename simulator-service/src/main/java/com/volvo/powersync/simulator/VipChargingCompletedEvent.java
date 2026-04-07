package com.volvo.powersync.simulator;

public record VipChargingCompletedEvent(String vin, String chargingStationId, long timestampMs) {}
