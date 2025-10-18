package com.example.arangkada.activities;

public class NotificationItem {
    private final String title;
    private final String message;
    private final String timestamp;
    private final String type; // "confirmed", "rejected", "info"

    public NotificationItem(String title, String message, String timestamp, String type) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public String getType() { return type; }
}
