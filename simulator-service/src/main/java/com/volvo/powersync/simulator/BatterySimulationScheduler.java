package com.volvo.powersync.simulator;

import com.volvo.powersync.grpc.booking.BookChargingReply;
import com.volvo.powersync.grpc.booking.ReleaseChargingReply;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every tick: battery, optional book when low, optional release when charged enough, then log.
 * Read this class top-to-bottom to follow the simulator loop.
 */
@Component
public class BatterySimulationScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatterySimulationScheduler.class);

    private final CarFleetRegistry fleet;
    private final BookingGrpcClient booking;
    private final VipChargingEventsPublisher vipChargingEventsPublisher;
    private final CarStatusEventsPublisher carStatusEventsPublisher;

    @Value("${simulator.low-battery-threshold-percent:20}")
    private int lowBatteryThresholdPercent;

    @Value("${simulator.charging-complete-percent:80}")
    private int chargingCompletePercent;

    /** Percent battery gain per tick while charging. */
    @Value("${simulator.battery-charge-percent-per-tick:4}")
    private int batteryChargePercentPerTick;

    public BatterySimulationScheduler(
            CarFleetRegistry fleet,
            BookingGrpcClient booking,
            VipChargingEventsPublisher vipChargingEventsPublisher,
            CarStatusEventsPublisher carStatusEventsPublisher) {
        this.fleet = fleet;
        this.booking = booking;
        this.vipChargingEventsPublisher = vipChargingEventsPublisher;
        this.carStatusEventsPublisher = carStatusEventsPublisher;
    }

    @Scheduled(fixedRateString = "${simulator.tick-interval-ms:1000}")
    public void tick() {
        StringBuilder line = new StringBuilder(128);
        for (Car car : fleet.allCars()) {
            int drivingDrain = ThreadLocalRandom.current().nextBoolean() ? 2 : 4;
            car.applyBatteryTick(drivingDrain, batteryChargePercentPerTick);
            maybeBookChargingStation(car);
            maybeReleaseChargingStation(car);
            carStatusEventsPublisher.publish(car);
            if (!line.isEmpty()) {
                line.append(" | ");
            }
            line.append(car.vin())
                    .append('=')
                    .append(car.batteryPercentage())
                    .append('%')
                    .append('(')
                    .append(car.state())
                    .append(')');
        }
        log.info("[simulator] {}", line);
    }

    /** If the car is low on battery while driving, call booking-service to reserve a station. */
    private void maybeBookChargingStation(Car car) {
        if (car.vipEligible()) {
            return;
        }
        if (car.batteryPercentage() > lowBatteryThresholdPercent) {
            car.clearLowBatteryBookingAttempted();
            return;
        }
        if ((car.state() != CarState.DRIVING && car.state() != CarState.STOPPED)
                || car.assignedChargingStationId() != null) {
            return;
        }
        if (car.isLowBatteryBookingAttempted()) {
            return;
        }

        car.markLowBatteryBookingAttempted();
        try {
            BookChargingReply reply = booking.bookChargingStation(car.vin());
            if (reply.getSuccess()) {
                car.setAssignedChargingStationId(reply.getChargingStationId());
                car.setState(CarState.CHARGING);
                log.info(
                        "[simulator] Booked charging station id={} for vin={} (battery {}%)",
                        reply.getChargingStationId(),
                        car.vin(),
                        car.batteryPercentage());
            } else {
                car.clearLowBatteryBookingAttempted();
                log.warn(
                        "[simulator] booking-service could not book for vin={} (battery {}%): {}",
                        car.vin(),
                        car.batteryPercentage(),
                        reply.getMessage());
            }
        } catch (Exception e) {
            car.clearLowBatteryBookingAttempted();
            log.error(
                    "[simulator] gRPC booking failed for vin={} (battery {}%): {}",
                    car.vin(),
                    car.batteryPercentage(),
                    e.getMessage());
        }
    }

    /** After charging to the target level, tell booking-service to free the slot, then drive again. */
    private void maybeReleaseChargingStation(Car car) {
        if (car.state() != CarState.CHARGING) {
            return;
        }
        if (car.batteryPercentage() < chargingCompletePercent) {
            return;
        }
        String stationId = car.assignedChargingStationId();
        if (stationId == null) {
            stationId = "";
        }
        if (car.vipEligible()) {
            vipChargingEventsPublisher.publishVipChargingCompleted(car.vin(), stationId);
            car.setAssignedChargingStationId(null);
            car.setState(CarState.DRIVING);
            log.info(
                    "[simulator] VIP car vin={} reached {}%, published completion and returned to DRIVING",
                    car.vin(),
                    car.batteryPercentage());
            return;
        }
        try {
            ReleaseChargingReply reply = booking.releaseChargingStation(car.vin(), stationId);
            if (reply.getSuccess()) {
                car.setAssignedChargingStationId(null);
                car.setState(CarState.DRIVING);
                log.info(
                        "[simulator] Released station id={} for vin={} (battery {}%), back to DRIVING",
                        stationId.isEmpty() ? "(by VIN)" : stationId,
                        car.vin(),
                        car.batteryPercentage());
            } else {
                log.warn(
                        "[simulator] booking-service could not release for vin={} (local station id={}): {}",
                        car.vin(),
                        stationId.isEmpty() ? "(none)" : stationId,
                        reply.getMessage());
                car.setAssignedChargingStationId(null);
                car.setState(CarState.DRIVING);
                log.warn(
                        "[simulator] Cleared local CHARGING state for vin={} after release failure (drift recovery)",
                        car.vin());
            }
        } catch (Exception e) {
            log.error(
                    "[simulator] gRPC release failed for vin={} station={}: {}",
                    car.vin(),
                    stationId.isEmpty() ? "(none)" : stationId,
                    e.getMessage());
            car.setAssignedChargingStationId(null);
            car.setState(CarState.DRIVING);
            log.warn(
                    "[simulator] Cleared local CHARGING state for vin={} after gRPC error (drift recovery)",
                    car.vin());
        }
    }
}
