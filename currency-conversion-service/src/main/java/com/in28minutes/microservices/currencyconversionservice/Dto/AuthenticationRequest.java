package com.in28minutes.microservices.currencyconversionservice.Dto;

public class AuthenticationRequest {
    private String username;
    private String password;

    // getters and setters

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}