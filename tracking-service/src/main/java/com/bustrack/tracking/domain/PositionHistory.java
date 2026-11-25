package com.bustrack.tracking.domain;

import jakarta.persistence.*;

/** One GPS point received from a bus (history). */
@Entity
@Table(name = "position_history", indexes = @Index(name = "idx_pos_bus_ts", columnList = "busId,timestampEpochMs"))
public class PositionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String busId;
    private String lineCode;
    private double latitude;
    private double longitude;
    private double speedKmh;
    private long timestampEpochMs;

    protected PositionHistory() {}

    public PositionHistory(String busId, String lineCode, double lat, double lon, double speed, long ts) {
        this.busId = busId;
        this.lineCode = lineCode;
        this.latitude = lat;
        this.longitude = lon;
        this.speedKmh = speed;
        this.timestampEpochMs = ts;
    }

    public Long getId() { return id; }
    public String getBusId() { return busId; }
    public String getLineCode() { return lineCode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getSpeedKmh() { return speedKmh; }
    public long getTimestampEpochMs() { return timestampEpochMs; }
}
