package com.volvo.powersync.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class VipStationStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(VipStationStatusConsumer.class);

    private final ObjectMapper objectMapper;
    private final VipStationStatusStore store;
    private final VipStationStatusBroadcaster broadcaster;

    public VipStationStatusConsumer(
            ObjectMapper objectMapper,
            VipStationStatusStore store,
            VipStationStatusBroadcaster broadcaster) {
        this.objectMapper = objectMapper;
        this.store = store;
        this.broadcaster = broadcaster;
    }

    @KafkaListener(topics = "vip-station-status-events", groupId = "notification-service")
    public void onVipStatus(String payload) {
        try {
            VipStationStatusEvent event = objectMapper.readValue(payload, VipStationStatusEvent.class);
            store.update(event);
            broadcaster.broadcast(event);
            log.info("VIP station status update: {} assignedVin={}", event.status(), event.assignedVin());
        } catch (JsonProcessingException e) {
            log.error("Invalid VIP station status payload: {}", payload);
        }
    }
}
