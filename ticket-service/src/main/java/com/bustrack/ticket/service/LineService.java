package com.bustrack.ticket.service;

import com.bustrack.ticket.client.TrackingClient;
import com.bustrack.ticket.client.TrackingClient.LastPosition;
import com.bustrack.ticket.dto.LineRequest;
import com.bustrack.ticket.dto.NextBusResponse;
import com.bustrack.ticket.exception.NotFoundException;
import com.bustrack.ticket.model.Line;
import com.bustrack.ticket.repository.LineRepository;
import com.bustrack.tracking.grpc.Position;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LineService {

    private final LineRepository lines;
    private final TrackingClient tracking;

    public LineService(LineRepository lines, TrackingClient tracking) {
        this.lines = lines;
        this.tracking = tracking;
    }

    public List<Line> findAll() { return lines.findAll(); }

    public Line get(Long id) {
        return lines.findById(id).orElseThrow(() -> new NotFoundException("Line " + id + " not found"));
    }

    public Line create(LineRequest r) {
        return lines.save(new Line(r.code(), r.name(), r.price(), r.stopLatitude(), r.stopLongitude()));
    }

    public NextBusResponse nextBus(Long lineId) {
        Line line = get(lineId);
        LastPosition last = tracking.lastPositionOnLine(line.getCode());

        return switch (last.state()) {
            case UNAVAILABLE -> NextBusResponse.degraded(line.getId(), line.getCode(), "Real-time position unavailable");
            case NONE -> throw new NotFoundException("No active bus on line " + line.getCode());
            case FOUND -> {
                Position p = last.position();
                double meters = haversineMeters(p.getLatitude(), p.getLongitude(), line.getStopLatitude(), line.getStopLongitude());
                double speedMs = Math.max(p.getSpeedKmh(), 10.0) / 3.6;      // avoid division by ~0
                long eta = Math.round(meters / speedMs);
                yield new NextBusResponse(line.getId(), line.getCode(), true, p.getBusId(),
                        p.getLatitude(), p.getLongitude(), p.getSpeedKmh(), eta, null);
            }
        };
    }

    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
