package com.volvo.powersync.simulator.service;

import com.volvo.powersync.simulator.domain.Car;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BatterySimulationScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatterySimulationScheduler.class);

    private final CarFleetRegistry fleet;

    public BatterySimulationScheduler(CarFleetRegistry fleet) {
        this.fleet = fleet;
    }

    @Scheduled(fixedRateString = "${simulator.tick-interval-ms:1000}")
    public void tick() {
        StringBuilder line = new StringBuilder(128);
        for (Car car : fleet.allCars()) {
            car.applyBatteryTick();
            if (!line.isEmpty()) {
                line.append(" | ");
            }
            line.append(car.vin())
                    .append('=')
                    .append(car.batteryPercentage())
                    .append('%');
        }
        log.info("[simulator] {}", line);
    }
}
