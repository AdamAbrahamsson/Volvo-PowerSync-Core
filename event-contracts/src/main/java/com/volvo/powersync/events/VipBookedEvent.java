package com.volvo.powersync.events;

public record VipBookedEvent(String vin, String chargingStationId, long timestampMs) {}
