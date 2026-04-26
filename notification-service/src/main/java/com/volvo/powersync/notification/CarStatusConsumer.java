package com.volvo.powersync.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volvo.powersync.events.CarStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CarStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(CarStatusConsumer.class);

    private final ObjectMapper objectMapper;
    private final CarStatusStore store;
    private final CarStatusBroadcaster broadcaster;

    public CarStatusConsumer(
            ObjectMapper objectMapper,
            CarStatusStore store,
            CarStatusBroadcaster broadcaster) {
        this.objectMapper = objectMapper;
        this.store = store;
        this.broadcaster = broadcaster;
    }

    @KafkaListener(topics = "car-status-events", groupId = "notification-service")
    public void onCarStatus(String payload) {
        try {
            CarStatusEvent event = objectMapper.readValue(payload, CarStatusEvent.class);
            store.update(event);
            broadcaster.broadcast(store.getCurrent());
        } catch (JsonProcessingException e) {
            log.error("Invalid car status payload: {}", payload);
        }
    }
}
