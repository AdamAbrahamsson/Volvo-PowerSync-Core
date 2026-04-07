package com.volvo.powersync.simulator;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/{vin}")
    public CarDetailsResponse getCar(@PathVariable String vin) {
        Map<String, Car> carsByFleetKey = fleet.allCarsByFleetKey();
        for (Map.Entry<String, Car> entry : carsByFleetKey.entrySet()) {
            Car car = entry.getValue();
            if (car.vin().equals(vin)) {
                return new CarDetailsResponse(
                        car.vin(),
                        fleetKeyToLabel(entry.getKey()),
                        car.batteryPercentage(),
                        car.state().name(),
                        car.assignedChargingStationId(),
                        car.vipEligible());
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found: " + vin);
    }

    @PostMapping("/{vin}/vip-booking")
    public CarDetailsResponse startVipCharging(@PathVariable String vin, @RequestParam String chargingStationId) {
        Map<String, Car> carsByFleetKey = fleet.allCarsByFleetKey();
        for (Map.Entry<String, Car> entry : carsByFleetKey.entrySet()) {
            Car car = entry.getValue();
            if (!car.vin().equals(vin)) {
                continue;
            }
            if (!car.vipEligible()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only VIP cars can use this endpoint");
            }
            if (car.assignedChargingStationId() != null && !car.assignedChargingStationId().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Car already has a station");
            }
            car.setAssignedChargingStationId(chargingStationId);
            car.setState(CarState.CHARGING);
            return new CarDetailsResponse(
                    car.vin(),
                    fleetKeyToLabel(entry.getKey()),
                    car.batteryPercentage(),
                    car.state().name(),
                    car.assignedChargingStationId(),
                    car.vipEligible());
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found: " + vin);
    }

    private String fleetKeyToLabel(String fleetKey) {
        return fleetKey == null ? "UNKNOWN" : fleetKey.toUpperCase();
    }

    public record CarSummaryResponse(String vin, String name, boolean vipEligible) {}

    public record CarDetailsResponse(
            String vin,
            String name,
            int batteryPercentage,
            String status,
            String assignedChargingStationId,
            boolean vipEligible) {}
}
