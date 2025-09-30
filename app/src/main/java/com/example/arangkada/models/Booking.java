package com.example.arangkada.models;

import com.google.firebase.Timestamp;

public class Booking {
    private String bookingId;
    private String status;
    private String passengerType;
    private int passengerCount;
    private Timestamp departure;
    private String destinationId;
    private String tripId;
    private String userId;
    private int totalFare;

    public Booking() {}

    public Booking(String bookingId,
                   String status,
                   String passengerType,
                   int passengerCount,
                   Timestamp departure,
                   String destinationId,
                   String tripId,
                   String userId,
                   int totalFare) {
        this.bookingId = bookingId;
        this.status = status;
        this.passengerType = passengerType;
        this.passengerCount = passengerCount;
        this.departure = departure;
        this.destinationId = destinationId;
        this.tripId = tripId;
        this.userId = userId;
        this.totalFare = totalFare;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPassengerType() { return passengerType; }
    public void setPassengerType(String passengerType) { this.passengerType = passengerType; }

    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }

    public Timestamp getDeparture() { return departure; }
    public void setDeparture(Timestamp departure) { this.departure = departure; }

    public String getDestinationId() { return destinationId; }
    public void setDestinationId(String destinationId) { this.destinationId = destinationId; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getTotalFare() { return totalFare; }
    public void setTotalFare(int totalFare) { this.totalFare = totalFare; }
}
