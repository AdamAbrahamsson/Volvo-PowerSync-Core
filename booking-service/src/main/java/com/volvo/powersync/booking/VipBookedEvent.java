package com.volvo.powersync.booking;

public record VipBookedEvent(String vin, String chargingStationId, long timestampMs) {}
