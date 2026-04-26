package com.volvo.powersync.notification;

import com.volvo.powersync.events.StationStatusEvent;
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
public class StationStatusController {

    private final StationStatusStore store;
    private final StationStatusBroadcaster broadcaster;

    public StationStatusController(StationStatusStore store, StationStatusBroadcaster broadcaster) {
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
