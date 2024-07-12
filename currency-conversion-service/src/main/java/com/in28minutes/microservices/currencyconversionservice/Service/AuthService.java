package com.in28minutes.microservices.currencyconversionservice.Service;

import com.in28minutes.microservices.currencyconversionservice.Dto.AuthenticationRequest;
import com.in28minutes.microservices.currencyconversionservice.Dto.AuthenticationResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public AuthenticationResponse authenticate(AuthenticationRequest authRequest) {
        // Implement your authentication logic here
        // For example, check the username and password from a database

        if ("user".equals(authRequest.getUsername()) && "password".equals(authRequest.getPassword())) {
            return new AuthenticationResponse("Bearer some-jwt-token");
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }
}