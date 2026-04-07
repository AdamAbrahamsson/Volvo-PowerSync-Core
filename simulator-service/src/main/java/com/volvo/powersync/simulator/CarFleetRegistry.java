package com.volvo.powersync.simulator;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** In-memory fleet: keys ex30 / ex40 / …, each car has a 4-digit VIN. */
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
            carsByFleetKey.put("vip70", Car.createVip("7070", 55));
            carsByFleetKey.put("vip80", Car.createVip("8080", 58));
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

    public Map<String, Car> allCarsByFleetKey() {
        synchronized (carsByFleetKey) {
            return new LinkedHashMap<>(carsByFleetKey);
        }
    }

    public int size() {
        return carsByFleetKey.size();
    }
}
