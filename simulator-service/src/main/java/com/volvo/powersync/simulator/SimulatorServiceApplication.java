package com.volvo.powersync.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** All simulator beans live in this package (flat layout, same idea as booking-service). */
@SpringBootApplication
@EnableScheduling
public class SimulatorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorServiceApplication.class, args);
    }
}
