package com.volvo.powersync.booking;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BookingMetrics {

    private final MeterRegistry registry;

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void bookingSuccess(ChargingStation station) {
        counter("powersync_station_bookings_total",
                "stationId", String.valueOf(station.getId()),
                "stationName", station.getName(),
                "stationType", station.getStationType().name())
                .increment();
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(registry);
    }
}
