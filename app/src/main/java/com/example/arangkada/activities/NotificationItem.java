package com.example.arangkada.activities;

public class NotificationItem {
    private String title;
    private String message;
    private String timestamp;
    private String type;
    private String tripName;
    private String tripDate;
    private String tripFare;

    public NotificationItem(String title, String message, String timestamp, String type,
                            String tripName, String tripDate, String tripFare) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.tripName = tripName;
        this.tripDate = tripDate;
        this.tripFare = tripFare;
    }

    // Old constructor (for backward compatibility)
    public NotificationItem(String title, String message, String timestamp, String type) {
        this(title, message, timestamp, type, "", "", "");
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public String getTripName() { return tripName; }
    public String getTripDate() { return tripDate; }
    public String getTripFare() { return tripFare; }
}
