package com.volvo.powersync.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CarStatusEventsPublisher {

    static final String CAR_STATUS_TOPIC = "car-status-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CarStatusEventsPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Car car) {
        CarStatusEvent event = new CarStatusEvent(
                car.vin(),
                car.batteryPercentage(),
                car.state().name(),
                car.assignedChargingStationId(),
                car.vipEligible(),
                System.currentTimeMillis());
        try {
            kafkaTemplate.send(CAR_STATUS_TOPIC, car.vin(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize car status event", e);
        }
    }
}
