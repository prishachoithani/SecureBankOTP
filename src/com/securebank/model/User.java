package com.securebank.model;

/**
 * Represents a bank customer.
 * Demonstrates basic encapsulation (private fields + public getters/setters).
 */
public class User {
    private String userId;
    private String name;
    private String registeredDevice; // simulates "known device" for fraud checks
    private String phoneNumber;

    public User(String userId, String name, String phoneNumber, String registeredDevice) {
        this.userId = userId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.registeredDevice = registeredDevice;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getRegisteredDevice() {
        return registeredDevice;
    }

    @Override
    public String toString() {
        return "User{id='" + userId + "', name='" + name + "'}";
    }
}
