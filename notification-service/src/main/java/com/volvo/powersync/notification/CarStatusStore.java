package com.volvo.powersync.notification;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class CarStatusStore {

    private final AtomicReference<Map<String, CarStatusEvent>> current =
            new AtomicReference<>(new LinkedHashMap<>());

    public List<CarStatusEvent> getCurrent() {
        List<CarStatusEvent> list = new ArrayList<>(current.get().values());
        list.sort(Comparator.comparing(CarStatusEvent::vin));
        return list;
    }

    public void update(CarStatusEvent event) {
        Map<String, CarStatusEvent> updated = new LinkedHashMap<>(current.get());
        updated.put(event.vin(), event);
        current.set(updated);
    }
}
