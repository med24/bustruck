package com.bustrack.ticket.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lines")
public class Line {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    /** Reference stop used to compute the ETA of the next bus. */
    private double stopLatitude;
    private double stopLongitude;

    protected Line() {}

    public Line(String code, String name, BigDecimal price, double stopLatitude, double stopLongitude) {
        this.code = code;
        this.name = name;
        this.price = price;
        this.stopLatitude = stopLatitude;
        this.stopLongitude = stopLongitude;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public double getStopLatitude() { return stopLatitude; }
    public double getStopLongitude() { return stopLongitude; }
}
