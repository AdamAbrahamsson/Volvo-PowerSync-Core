package com.volvo.powersync.simulator;

public record VipBookedEvent(String vin, String chargingStationId, long timestampMs) {}
