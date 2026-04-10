package com.volvo.powersync.booking;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupStationResetRunner implements ApplicationRunner {

    private final StationBooker stationBooker;

    public StartupStationResetRunner(StationBooker stationBooker) {
        this.stationBooker = stationBooker;
    }

    @Override
    public void run(ApplicationArguments args) {
        stationBooker.resetAllStationsToFree();
    }
}
