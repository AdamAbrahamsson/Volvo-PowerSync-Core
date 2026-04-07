package com.volvo.powersync.booking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class VipBookingEventsPublisher {

    static final String VIP_BOOKED_TOPIC = "vip-booked-events";
    static final String VIP_STATION_STATUS_TOPIC = "vip-station-status-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public VipBookingEventsPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishVipBooked(String vin, String stationId) {
        VipBookedEvent event = new VipBookedEvent(vin, stationId, System.currentTimeMillis());
        try {
            kafkaTemplate.send(VIP_BOOKED_TOPIC, vin, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize VIP booking event", e);
        }
    }

    public void publishVipStationStatus(String status, String assignedVin) {
        VipStationStatusEvent event = new VipStationStatusEvent(status, assignedVin, System.currentTimeMillis());
        try {
            kafkaTemplate.send(VIP_STATION_STATUS_TOPIC, "vip-hub", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize VIP station status event", e);
        }
    }
}
