package com.example.arangkada.models;

import java.util.Date;

public class Trip {
    private String bookingId;
    private String status;
    private String passengerType;
    private long seats;
    private Date departure;
    private String destinationName;
    private String plateNumber; // 👈 new field
    private double totalFare;

    public Trip() {} // empty constructor for Firebase

    public Trip(String bookingId, String status, String passengerType, long seats, Date departure, String destinationName, String plateNumber, double totalFare) {
        this.bookingId = bookingId;
        this.status = status;
        this.passengerType = passengerType;
        this.seats = seats;
        this.departure = departure;
        this.destinationName = destinationName;
        this.plateNumber = plateNumber;
    }

    // Getters
    public String getBookingId() { return bookingId; }
    public String getStatus() { return status; }
    public String getPassengerType() { return passengerType; }
    public long getSeats() { return seats; }
    public Date getDeparture() { return departure; }
    public String getDestinationName() { return destinationName; }
    public String getPlateNumber() { return plateNumber; }

    // Setters (optional, useful if updating later)
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setStatus(String status) { this.status = status; }
    public void setPassengerType(String passengerType) { this.passengerType = passengerType; }
    public void setSeats(long seats) { this.seats = seats; }
    public void setDeparture(Date departure) { this.departure = departure; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public double getTotalFare() { return totalFare; }
    public void setTotalFare(double totalFare) { this.totalFare = totalFare; }
}
