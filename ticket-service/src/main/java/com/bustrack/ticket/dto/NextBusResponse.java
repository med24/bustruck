package com.bustrack.ticket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NextBusResponse(Long lineId, String lineCode, boolean realTime, String busId,
                              Double latitude, Double longitude, Double speedKmh, Long etaSeconds, String message) {

    public static NextBusResponse degraded(Long lineId, String lineCode, String message) {
        return new NextBusResponse(lineId, lineCode, false, null, null, null, null, null, message);
    }
}
