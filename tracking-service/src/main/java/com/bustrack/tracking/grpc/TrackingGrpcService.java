package com.bustrack.tracking.grpc;

import com.bustrack.tracking.domain.Bus;
import com.bustrack.tracking.domain.BusRepository;
import com.bustrack.tracking.service.TrackingService;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@GrpcService
public class TrackingGrpcService extends TrackingServiceGrpc.TrackingServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(TrackingGrpcService.class);

    private final BusRepository buses;
    private final TrackingService tracking;
    private final long intervalSeconds;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public TrackingGrpcService(BusRepository buses, TrackingService tracking,
                               @Value("${tracking.stream.interval-seconds:2}") long intervalSeconds) {
        this.buses = buses;
        this.tracking = tracking;
        this.intervalSeconds = intervalSeconds;
    }

    /** Unary */
    @Override
    public void getLastPosition(BusRequest request, StreamObserver<Position> responseObserver) {
        buses.findById(request.getBusId()).ifPresentOrElse(
                bus -> {
                    responseObserver.onNext(TrackingService.toProto(bus));
                    responseObserver.onCompleted();
                },
                () -> responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Unknown bus " + request.getBusId()).asRuntimeException()));
    }

    /** Unary: freshest position among the buses of a line */
    @Override
    public void getLineLastPosition(LineRequest request, StreamObserver<Position> responseObserver) {
        buses.findFirstByLineCodeOrderByLastTimestampDesc(request.getLineCode()).ifPresentOrElse(
                bus -> {
                    responseObserver.onNext(TrackingService.toProto(bus));
                    responseObserver.onCompleted();
                },
                () -> responseObserver.onError(Status.NOT_FOUND
                        .withDescription("No bus on line " + request.getLineCode()).asRuntimeException()));
    }

    /** Server streaming: pushes the position of every bus of the line every N seconds until the client cancels. */
    @Override
    public void streamLinePositions(LineRequest request, StreamObserver<Position> responseObserver) {
        ServerCallStreamObserver<Position> out = (ServerCallStreamObserver<Position>) responseObserver;
        AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();
        out.setOnCancelHandler(() -> {
            ScheduledFuture<?> f = task.get();
            if (f != null) f.cancel(false);
            log.info("Stream cancelled for line {}", request.getLineCode());
        });

        task.set(scheduler.scheduleAtFixedRate(() -> {
            try {
                if (out.isCancelled()) {
                    ScheduledFuture<?> f = task.get();
                    if (f != null) f.cancel(false);
                    return;
                }
                for (Bus bus : buses.findByLineCode(request.getLineCode())) {
                    out.onNext(TrackingService.toProto(bus));
                }
            } catch (Exception e) {
                log.warn("Stream error on line {}: {}", request.getLineCode(), e.toString());
                ScheduledFuture<?> f = task.get();
                if (f != null) f.cancel(false);
                out.onError(Status.INTERNAL.withDescription("Stream failed").asRuntimeException());
            }
        }, 100, intervalSeconds * 1000, TimeUnit.MILLISECONDS));
    }

    /** Client streaming: buses / simulator push positions, a single ack is returned at the end. */
    @Override
    public StreamObserver<Position> reportPositions(StreamObserver<ReportAck> responseObserver) {
        return new StreamObserver<>() {
            private long count;
            private boolean failed;

            @Override
            public void onNext(Position p) {
                if (failed) return;
                if (p.getBusId().isBlank()) {
                    failed = true;
                    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("bus_id is required").asRuntimeException());
                    return;
                }
                tracking.record(p);
                count++;
            }

            @Override
            public void onError(Throwable t) {
                log.warn("ReportPositions client error: {}", t.toString());
            }

            @Override
            public void onCompleted() {
                if (failed) return;
                responseObserver.onNext(ReportAck.newBuilder().setReceivedCount(count).build());
                responseObserver.onCompleted();
            }
        };
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
