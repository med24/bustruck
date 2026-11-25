package com.bustrack.tracking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A bus with its last known position. */
@Entity
@Table(name = "buses")
public class Bus {

    @Id
    private String busId;
    private String lineCode;
    private double latitude;
    private double longitude;
    private double speedKmh;
    private long lastTimestamp;

    protected Bus() {}

    public Bus(String busId) { this.busId = busId; }

    public void update(String lineCode, double lat, double lon, double speed, long ts) {
        this.lineCode = lineCode;
        this.latitude = lat;
        this.longitude = lon;
        this.speedKmh = speed;
        this.lastTimestamp = ts;
    }

    public String getBusId() { return busId; }
    public String getLineCode() { return lineCode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getSpeedKmh() { return speedKmh; }
    public long getLastTimestamp() { return lastTimestamp; }
}
