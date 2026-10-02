package com.example.aks;

public class Message {
    public String senderId;
    public String senderName;
    public String text;
    public long timestamp;

    public Message() {} // Required for Firebase

    public Message(String senderId, String senderName, String text, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }
}
