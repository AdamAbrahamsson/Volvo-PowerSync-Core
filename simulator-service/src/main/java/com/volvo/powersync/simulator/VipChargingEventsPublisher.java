package com.volvo.powersync.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volvo.powersync.events.VipChargingCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class VipChargingEventsPublisher {

    static final String VIP_CHARGING_COMPLETED_TOPIC = "vip-charging-completed-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public VipChargingEventsPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishVipChargingCompleted(String vin, String stationId) {
        VipChargingCompletedEvent event = new VipChargingCompletedEvent(vin, stationId, System.currentTimeMillis());
        try {
            kafkaTemplate.send(VIP_CHARGING_COMPLETED_TOPIC, vin, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize VIP completion event", e);
        }
    }
}
