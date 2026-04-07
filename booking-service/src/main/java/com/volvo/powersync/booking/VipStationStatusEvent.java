package com.volvo.powersync.booking;

public record VipStationStatusEvent(String status, String assignedVin, long timestampMs) {}
