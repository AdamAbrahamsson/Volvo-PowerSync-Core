package com.volvo.powersync.notification;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class VipStationStatusStore {

    private final AtomicReference<VipStationStatusEvent> current =
            new AtomicReference<>(new VipStationStatusEvent("UNKNOWN", null, System.currentTimeMillis()));

    public VipStationStatusEvent getCurrent() {
        return current.get();
    }

    public void update(VipStationStatusEvent event) {
        current.set(event);
    }
}
