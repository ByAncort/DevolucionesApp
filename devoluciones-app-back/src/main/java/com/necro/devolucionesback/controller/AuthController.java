package com.necro.devolucionesback.controller;

import com.necro.devolucionesback.dto.AuthResponse;
import com.necro.devolucionesback.dto.LoginRequest;
import com.necro.devolucionesback.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/v1/auth/")
public class AuthController {

    @PostMapping("login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestMapping LoginRequest request){
        return ResponseEntity.ok().body(AuthService.login(request));
    }
}
