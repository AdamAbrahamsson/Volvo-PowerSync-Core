package com.volvo.powersync.simulator;

import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cars")
@CrossOrigin(origins = "*")
public class CarStatusController {

    private final CarFleetRegistry fleet;

    public CarStatusController(CarFleetRegistry fleet) {
        this.fleet = fleet;
    }

    @GetMapping
    public List<CarSummaryResponse> listCars() {
        return fleet.allCarsByFleetKey()
                .entrySet()
                .stream()
                .map(entry -> new CarSummaryResponse(
                        entry.getValue().vin(),
                        fleetKeyToLabel(entry.getKey()),
                        entry.getValue().vipEligible()))
                .toList();
    }

    private String fleetKeyToLabel(String fleetKey) {
        return fleetKey == null ? "UNKNOWN" : fleetKey.toUpperCase();
    }

    public record CarSummaryResponse(String vin, String name, boolean vipEligible) {}
}
