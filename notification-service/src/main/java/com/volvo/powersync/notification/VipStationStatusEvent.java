package com.volvo.powersync.notification;

public record VipStationStatusEvent(String status, String assignedVin, long timestampMs) {}
