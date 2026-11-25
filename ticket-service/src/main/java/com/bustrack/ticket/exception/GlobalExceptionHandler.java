package com.bustrack.ticket.exception;

import com.bustrack.ticket.dto.ErrorDto;
import com.bustrack.ticket.model.TicketStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDto> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorDto.of("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(TicketStateException.class)
    public ResponseEntity<ErrorDto> conflict(TicketStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorDto.of("INVALID_TICKET_STATE", e.getMessage()));
    }

    @ExceptionHandler(PaymentRejectedException.class)
    public ResponseEntity<ErrorDto> rejected(PaymentRejectedException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ErrorDto.of(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(PaymentUnavailableException.class)
    public ResponseEntity<ErrorDto> unavailable(PaymentUnavailableException e) {
        ErrorDto body = new ErrorDto("PAYMENT_UNAVAILABLE", e.getMessage(), Instant.now(),
                e.getTicketId(), TicketStatus.PENDING.name());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ErrorDto.of("VALIDATION_ERROR", msg));
    }
}
