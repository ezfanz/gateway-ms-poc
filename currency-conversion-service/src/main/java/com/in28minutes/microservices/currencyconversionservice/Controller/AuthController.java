package com.in28minutes.microservices.currencyconversionservice;

import com.in28minutes.microservices.currencyconversionservice.Dto.AuthenticationRequest;
import com.in28minutes.microservices.currencyconversionservice.Dto.AuthenticationResponse;
import com.in28minutes.microservices.currencyconversionservice.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest authRequest) {
        AuthenticationResponse authResponse = authService.authenticate(authRequest);
        return ResponseEntity.ok(authResponse);
    }
}
