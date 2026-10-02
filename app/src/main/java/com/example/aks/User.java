package com.example.aks;

public class User {

    private String id;

    private String userId;
    private String username;

    public User() {
        // Default constructor
    }

    public User(String username) {
        this.username = username;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User(String id, String username) {
        this.id = id;
        this.username = username;
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

