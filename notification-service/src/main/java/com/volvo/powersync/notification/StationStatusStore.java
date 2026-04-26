package com.volvo.powersync.notification;

import com.volvo.powersync.events.StationStatusEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class StationStatusStore {

    private final AtomicReference<Map<String, StationStatusEvent>> current =
            new AtomicReference<>(new LinkedHashMap<>());

    public List<StationStatusEvent> getCurrent() {
        List<StationStatusEvent> list = new ArrayList<>(current.get().values());
        list.sort(Comparator.comparing(StationStatusEvent::stationName));
        return list;
    }

    public void update(StationStatusEvent event) {
        Map<String, StationStatusEvent> updated = new LinkedHashMap<>(current.get());
        updated.put(event.stationId(), event);
        current.set(updated);
    }
}
