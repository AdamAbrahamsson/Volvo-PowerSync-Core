package com.volvo.powersync.simulator;

import com.volvo.powersync.grpc.booking.BookChargingReply;
import com.volvo.powersync.grpc.booking.BookChargingRequest;
import com.volvo.powersync.grpc.booking.ChargingBookingGrpc;
import com.volvo.powersync.grpc.booking.ReleaseChargingReply;
import com.volvo.powersync.grpc.booking.ReleaseChargingRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls booking-service over gRPC (same contract as {@code BookingGrpcService} on the server). */
@Service
public class BookingGrpcClient {

    private final ChargingBookingGrpc.ChargingBookingBlockingStub stub;

    public BookingGrpcClient(@GrpcClient("booking-service") ChargingBookingGrpc.ChargingBookingBlockingStub stub) {
        this.stub = stub;
    }

    public BookChargingReply bookChargingStation(String vin) {
        return stub.bookChargingStation(BookChargingRequest.newBuilder().setVin(vin).build());
    }

    public ReleaseChargingReply releaseChargingStation(String vin, String chargingStationId) {
        return stub.releaseChargingStation(
                ReleaseChargingRequest.newBuilder()
                        .setVin(vin)
                        .setChargingStationId(chargingStationId)
                        .build());
    }
}
