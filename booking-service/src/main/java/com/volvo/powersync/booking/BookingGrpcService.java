package com.volvo.powersync.booking;

import com.volvo.powersync.grpc.booking.BookChargingReply;
import com.volvo.powersync.grpc.booking.BookChargingRequest;
import com.volvo.powersync.grpc.booking.ChargingBookingGrpc;
import com.volvo.powersync.grpc.booking.ReleaseChargingReply;
import com.volvo.powersync.grpc.booking.ReleaseChargingRequest;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC entry point: the simulator calls this over the network.
 * Flow: read VIN → ask {@link StationBooker} to update the database → send back success or failure.
 */
@GrpcService
public class BookingGrpcService extends ChargingBookingGrpc.ChargingBookingImplBase {

    private static final Logger log = LoggerFactory.getLogger(BookingGrpcService.class);

    private final StationBooker stationBooker;

    public BookingGrpcService(StationBooker stationBooker) {
        this.stationBooker = stationBooker;
    }

    @Override
    public void bookChargingStation(BookChargingRequest request, StreamObserver<BookChargingReply> responseObserver) {
        String vin = request.getVin();
        log.info("Booking request for car {}", vin);

        String stationId = stationBooker.tryBookOneFreeStation(vin);

        BookChargingReply reply;
        if (stationId == null) {
            reply = BookChargingReply.newBuilder()
                    .setSuccess(false)
                    .setChargingStationId("")
                    .setMessage("No free charging station")
                    .build();
            log.warn("No free station for car {}", vin);
        } else {
            reply = BookChargingReply.newBuilder()
                    .setSuccess(true)
                    .setChargingStationId(stationId)
                    .setMessage("Booked")
                    .build();
            log.info("Booked station {} for car {}", stationId, vin);
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void releaseChargingStation(ReleaseChargingRequest request, StreamObserver<ReleaseChargingReply> responseObserver) {
        String vin = request.getVin();
        String stationId = request.getChargingStationId();
        log.info("Release request for car {} station {}", vin, stationId);

        boolean ok = stationBooker.releaseStation(vin, stationId);
        ReleaseChargingReply reply = ReleaseChargingReply.newBuilder()
                .setSuccess(ok)
                .setMessage(ok ? "Released" : "Could not release (not found, not booked, or VIN mismatch)")
                .build();
        if (ok) {
            log.info("Released station {} for car {}", stationId, vin);
        } else {
            log.warn("Release failed for car {} station {}", vin, stationId);
        }
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
