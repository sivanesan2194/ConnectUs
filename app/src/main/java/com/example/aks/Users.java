package com.example.aks;
public class Users {
    private String id;
    private String username;

    // Constructor
    public Users() {
        // Default constructor required for Firebase
    }

    public Users(String id, String username) {
        this.id = id;
        this.username = username;
    }

    // Getter and setter methods
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
