package com.volvo.powersync.notification;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class VipStationStatusBroadcaster {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter register(VipStationStatusEvent initialEvent) throws IOException {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ignored -> emitters.remove(emitter));
        emitter.send(SseEmitter.event().name("vip-station-status").data(initialEvent));
        return emitter;
    }

    public void broadcast(VipStationStatusEvent event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("vip-station-status").data(event));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
