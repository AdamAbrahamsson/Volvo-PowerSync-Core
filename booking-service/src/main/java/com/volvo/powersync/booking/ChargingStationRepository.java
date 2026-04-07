package com.volvo.powersync.booking;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {

    /**
     * Loads at most one FREE station and locks that database row until we finish booking.
     * That way two cars cannot book the same station at the same time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ChargingStation> findTop1ByStateOrderByIdAsc(StationState state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ChargingStation> findTop1ByStateAndStationTypeOrderByIdAsc(StationState state, StationType stationType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChargingStation c where c.id = :id")
    Optional<ChargingStation> findByIdWithLock(@Param("id") Long id);
}
