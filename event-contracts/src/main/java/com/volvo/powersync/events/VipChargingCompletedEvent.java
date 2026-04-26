package com.volvo.powersync.events;

public record VipChargingCompletedEvent(String vin, String chargingStationId, long timestampMs) {}
