package com.volvo.powersync.booking;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class BookingMetrics {

    private final MeterRegistry registry;
    private final ConcurrentMap<String, AtomicInteger> stationBookedById = new ConcurrentHashMap<>();

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void bookingSuccess(ChargingStation station) {
        counter("powersync_station_bookings_total",
                "stationId", String.valueOf(station.getId()),
                "stationName", station.getName(),
                "stationType", station.getStationType().name())
                .increment();
        updateBookedGauge(station);
    }

    public void bookingFailure(StationType stationType, String reason) {
        counter("powersync_station_booking_failures_total",
                "stationType", stationType.name(),
                "reason", reason)
                .increment();
    }

    public void releaseSuccess(ChargingStation station) {
        counter("powersync_station_releases_total",
                "stationId", String.valueOf(station.getId()),
                "stationName", station.getName(),
                "stationType", station.getStationType().name())
                .increment();
        updateBookedGauge(station);
    }

    public void releaseFailure(String reason) {
        counter("powersync_station_release_failures_total", "reason", reason).increment();
    }

    public void stationStateSync(ChargingStation station) {
        updateBookedGauge(station);
    }

    private void updateBookedGauge(ChargingStation station) {
        String stationId = String.valueOf(station.getId());
        AtomicInteger gaugeValue = stationBookedById.computeIfAbsent(stationId, key -> {
            AtomicInteger ref = new AtomicInteger(0);
            Gauge.builder("powersync_station_booked", ref, AtomicInteger::get)
                    .description("1 when station is booked, 0 when free")
                    .tag("stationId", key)
                    .tag("stationName", station.getName())
                    .tag("stationType", station.getStationType().name())
                    .register(registry);
            return ref;
        });
        gaugeValue.set(station.getState() == StationState.BOOKED ? 1 : 0);
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(registry);
    }
}
