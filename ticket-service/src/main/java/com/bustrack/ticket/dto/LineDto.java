package com.bustrack.ticket.dto;

import com.bustrack.ticket.model.Line;
import java.math.BigDecimal;

public record LineDto(Long id, String code, String name, BigDecimal price, double stopLatitude, double stopLongitude) {
    public static LineDto from(Line l) {
        return new LineDto(l.getId(), l.getCode(), l.getName(), l.getPrice(), l.getStopLatitude(), l.getStopLongitude());
    }
}
