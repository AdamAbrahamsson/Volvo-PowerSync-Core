package com.volvo.powersync.notification;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class CarStatusBroadcaster {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter register(List<CarStatusEvent> initialSnapshot) throws IOException {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ignored -> emitters.remove(emitter));
        emitter.send(SseEmitter.event().name("car-status").data(initialSnapshot));
        return emitter;
    }

    public void broadcast(List<CarStatusEvent> snapshot) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("car-status").data(snapshot));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
