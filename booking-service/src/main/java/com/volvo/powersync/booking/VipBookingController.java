package com.volvo.powersync.booking;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/vip-stations")
@CrossOrigin(origins = "*")
public class VipBookingController {

    private static final Set<String> VIP_ELIGIBLE_VINS = Set.of("7070", "8080");

    private final StationBooker stationBooker;

    public VipBookingController(StationBooker stationBooker) {
        this.stationBooker = stationBooker;
    }

    @PostMapping("/book")
    public VipBookReply bookVip(@RequestParam String vin) {
        if (!VIP_ELIGIBLE_VINS.contains(vin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIN is not VIP eligible");
        }
        String stationId = stationBooker.tryBookOneFreeStationByType(vin, StationType.VIP);
        if (stationId == null) {
            return new VipBookReply(false, "", "VIP station is already booked");
        }
        return new VipBookReply(true, stationId, "VIP station booked");
    }

    public record VipBookReply(boolean success, String chargingStationId, String message) {}
}
