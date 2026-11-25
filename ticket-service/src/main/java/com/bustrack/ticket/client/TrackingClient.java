package com.bustrack.ticket.client;

import com.bustrack.tracking.grpc.LineRequest;
import com.bustrack.tracking.grpc.Position;
import com.bustrack.tracking.grpc.TrackingServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** gRPC client of tracking-service (unary call with a deadline + circuit breaker fallback). */
@Component
public class TrackingClient {

    private static final Logger log = LoggerFactory.getLogger(TrackingClient.class);

    public enum State { FOUND, NONE, UNAVAILABLE }

    public record LastPosition(State state, Position position) {}

    @GrpcClient("tracking")
    private TrackingServiceGrpc.TrackingServiceBlockingStub stub;

    @Value("${bustrack.tracking.deadline-ms:2000}")
    private long deadlineMs;

    @CircuitBreaker(name = "tracking", fallbackMethod = "lastPositionFallback")
    public LastPosition lastPositionOnLine(String lineCode) {
        try {
            Position p = stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .getLineLastPosition(LineRequest.newBuilder().setLineCode(lineCode).build());
            return new LastPosition(State.FOUND, p);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                return new LastPosition(State.NONE, null);
            }
            throw e;
        }
    }

    public LastPosition lastPositionFallback(String lineCode, Throwable t) {
        log.warn("tracking-service unavailable for line {}: {}", lineCode, t.toString());
        return new LastPosition(State.UNAVAILABLE, null);
    }
}
