package com.volvo.powersync.notification;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stations-status")
@CrossOrigin(origins = "*")
public class VipStationStatusController {

    private final VipStationStatusStore store;
    private final VipStationStatusBroadcaster broadcaster;

    public VipStationStatusController(VipStationStatusStore store, VipStationStatusBroadcaster broadcaster) {
        this.store = store;
        this.broadcaster = broadcaster;
    }

    @GetMapping
    public List<StationStatusEvent> current() {
        return store.getCurrent();
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        try {
            return broadcaster.register(store.getCurrent());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not open SSE stream");
        }
    }
}
