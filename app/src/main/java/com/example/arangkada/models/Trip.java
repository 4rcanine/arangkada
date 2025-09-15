package com.example.arangkada.models;

import java.util.Date;

public class Trip {
    private String bookingId;
    private String status;
    private String passengerType;
    private long seats;
    private Date departure;
    private String destinationName;

    public Trip() {} // empty constructor for Firebase

    public Trip(String bookingId, String status, String passengerType, long seats, Date departure, String destinationName) {
        this.bookingId = bookingId;
        this.status = status;
        this.passengerType = passengerType;
        this.seats = seats;
        this.departure = departure;
        this.destinationName = destinationName;
    }

    // Getters
    public String getBookingId() { return bookingId; }
    public String getStatus() { return status; }
    public String getPassengerType() { return passengerType; }
    public long getSeats() { return seats; }
    public Date getDeparture() { return departure; }
    public String getDestinationName() { return destinationName; }
}
