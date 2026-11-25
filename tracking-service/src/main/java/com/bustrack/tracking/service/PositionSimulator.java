package com.bustrack.tracking.service;

import com.bustrack.tracking.grpc.Position;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Demo data: 3 buses going back and forth on lines L10 and L20 (disable with tracking.simulator.enabled=false). */
@Component
@ConditionalOnProperty(name = "tracking.simulator.enabled", havingValue = "true")
public class PositionSimulator {

    private record Route(String busId, String lineCode, double lat1, double lon1, double lat2, double lon2) {}

    private final List<Route> routes = List.of(
            new Route("BUS-101", "L10", 36.8065, 10.1815, 36.8450, 10.2150),
            new Route("BUS-102", "L10", 36.8450, 10.2150, 36.8065, 10.1815),
            new Route("BUS-201", "L20", 36.7950, 10.1700, 36.8300, 10.0900));

    private final TrackingService tracking;
    private long tick;

    public PositionSimulator(TrackingService tracking) {
        this.tracking = tracking;
    }

    @Scheduled(fixedDelayString = "${tracking.simulator.interval-ms:5000}")
    public void emit() {
        tick++;
        for (int i = 0; i < routes.size(); i++) {
            Route r = routes.get(i);
            double phase = (tick * 0.05 + i * 0.3) % 2.0;
            double t = phase <= 1.0 ? phase : 2.0 - phase;      // ping-pong between both ends
            tracking.record(Position.newBuilder()
                    .setBusId(r.busId())
                    .setLineCode(r.lineCode())
                    .setLatitude(r.lat1() + (r.lat2() - r.lat1()) * t)
                    .setLongitude(r.lon1() + (r.lon2() - r.lon1()) * t)
                    .setSpeedKmh(25 + 10 * Math.abs(Math.sin(tick + i)))
                    .setTimestampEpochMs(System.currentTimeMillis())
                    .build());
        }
    }
}
