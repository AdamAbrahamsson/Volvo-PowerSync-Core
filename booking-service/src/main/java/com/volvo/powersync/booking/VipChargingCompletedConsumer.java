package com.volvo.powersync.booking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volvo.powersync.events.VipChargingCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class VipChargingCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(VipChargingCompletedConsumer.class);

    private final StationBooker stationBooker;
    private final ObjectMapper objectMapper;

    public VipChargingCompletedConsumer(StationBooker stationBooker, ObjectMapper objectMapper) {
        this.stationBooker = stationBooker;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "vip-charging-completed-events", groupId = "booking-service")
    public void onVipChargingCompleted(String payload) {
        VipChargingCompletedEvent event;
        try {
            event = objectMapper.readValue(payload, VipChargingCompletedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Invalid VIP completion payload: {}", payload);
            return;
        }
        boolean released = stationBooker.releaseStation(event.vin(), event.chargingStationId());
        if (released) {
            log.info(
                    "Released VIP station {} for vin={} after 80% event",
                    event.chargingStationId(),
                    event.vin());
        } else {
            log.warn(
                    "Could not release VIP station {} for vin={} from Kafka event",
                    event.chargingStationId(),
                    event.vin());
        }
    }
}
