package com.example.auth.controller;

import com.example.auth.dto.SignInRequest;
import com.example.auth.dto.SignInResponse;
import com.example.auth.service.JwtService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ms1")
public class AuthenticationController {

    private final JwtService jwtService;

    public AuthenticationController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping(
            value = "/signin",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SignInResponse> signIn(@RequestBody SignInRequest request) {
        // Authenticate request and generate dynamic JWT token
        String jwtToken = jwtService.generateToken(request.getUsername());
        return ResponseEntity.ok(new SignInResponse(jwtToken));
    }
}
