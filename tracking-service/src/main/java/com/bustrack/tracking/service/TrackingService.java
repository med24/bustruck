package com.bustrack.tracking.service;

import com.bustrack.tracking.domain.Bus;
import com.bustrack.tracking.domain.BusRepository;
import com.bustrack.tracking.domain.PositionHistory;
import com.bustrack.tracking.domain.PositionHistoryRepository;
import com.bustrack.tracking.grpc.Position;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackingService {

    private final BusRepository buses;
    private final PositionHistoryRepository history;

    public TrackingService(BusRepository buses, PositionHistoryRepository history) {
        this.buses = buses;
        this.history = history;
    }

    /** UPSERT the bus last position + INSERT a history row. */
    @Transactional
    public void record(Position p) {
        long ts = p.getTimestampEpochMs() > 0 ? p.getTimestampEpochMs() : System.currentTimeMillis();
        Bus bus = buses.findById(p.getBusId()).orElseGet(() -> new Bus(p.getBusId()));
        bus.update(p.getLineCode(), p.getLatitude(), p.getLongitude(), p.getSpeedKmh(), ts);
        buses.save(bus);
        history.save(new PositionHistory(p.getBusId(), p.getLineCode(), p.getLatitude(), p.getLongitude(), p.getSpeedKmh(), ts));
    }

    public static Position toProto(Bus b) {
        return Position.newBuilder()
                .setBusId(b.getBusId())
                .setLineCode(b.getLineCode() == null ? "" : b.getLineCode())
                .setLatitude(b.getLatitude())
                .setLongitude(b.getLongitude())
                .setSpeedKmh(b.getSpeedKmh())
                .setTimestampEpochMs(b.getLastTimestamp())
                .build();
    }
}
