package com.volvo.powersync.simulator.service;

import com.volvo.powersync.simulator.domain.Car;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * In-memory store for up to 10 vehicles. Fleet entries are keyed by model slot (ex30, ex40, …);
 * each {@link Car} carries a 4-digit VIN. Initially seeds four cars with battery between 30% and 70%.
 */
@Component
public class CarFleetRegistry {

    public static final int MAX_VEHICLES = 10;

    private final Map<String, Car> carsByFleetKey = Collections.synchronizedMap(new LinkedHashMap<>());

    @PostConstruct
    void seedFleet() {
        synchronized (carsByFleetKey) {
            carsByFleetKey.clear();
            carsByFleetKey.put("ex30", Car.create("3030", 45));
            carsByFleetKey.put("ex40", Car.create("4040", 52));
            carsByFleetKey.put("ex60", Car.create("6060", 38));
            carsByFleetKey.put("ex90", Car.create("9090", 67));
        }
    }

    public Optional<Car> findByFleetKey(String fleetKey) {
        return Optional.ofNullable(carsByFleetKey.get(fleetKey));
    }

    public Optional<Car> findByVin(String vin) {
        synchronized (carsByFleetKey) {
            for (Car car : carsByFleetKey.values()) {
                if (car.vin().equals(vin)) {
                    return Optional.of(car);
                }
            }
            return Optional.empty();
        }
    }

    public Collection<Car> allCars() {
        synchronized (carsByFleetKey) {
            return List.copyOf(carsByFleetKey.values());
        }
    }

    public int size() {
        return carsByFleetKey.size();
    }
}
