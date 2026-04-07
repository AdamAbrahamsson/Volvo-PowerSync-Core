package com.volvo.powersync.booking;

public record VipChargingCompletedEvent(String vin, String chargingStationId, long timestampMs) {}
