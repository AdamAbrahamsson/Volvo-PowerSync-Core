package com.volvo.powersync.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class VipBookedConsumer {

    private static final Logger log = LoggerFactory.getLogger(VipBookedConsumer.class);

    private final CarFleetRegistry fleet;
    private final ObjectMapper objectMapper;

    public VipBookedConsumer(CarFleetRegistry fleet, ObjectMapper objectMapper) {
        this.fleet = fleet;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "vip-booked-events", groupId = "simulator-service")
    public void onVipBooked(String payload) {
        VipBookedEvent event;
        try {
            event = objectMapper.readValue(payload, VipBookedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Invalid VIP booked payload: {}", payload);
            return;
        }
        fleet.findByVin(event.vin()).ifPresentOrElse(
                car -> {
                    if (!car.vipEligible()) {
                        log.warn("Received VIP booking for non-VIP car vin={}", event.vin());
                        return;
                    }
                    car.setAssignedChargingStationId(event.chargingStationId());
                    car.setState(CarState.CHARGING);
                    log.info(
                            "[simulator] VIP booking consumed: vin={} station={} now CHARGING",
                            event.vin(),
                            event.chargingStationId());
                },
                () -> log.warn("Received VIP booking for unknown vin={}", event.vin()));
    }
}
