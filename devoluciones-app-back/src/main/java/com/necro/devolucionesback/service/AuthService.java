package com.necro.devolucionesback.service;

import com.necro.devolucionesback.dto.AuthResponse;
import com.necro.devolucionesback.dto.LoginRequest;
import com.necro.devolucionesback.model.User;
import jakarta.validation.Valid;

public class AuthService {
    public static AuthResponse login(@Valid LoginRequest request) {
        try {
            User user = new User();
        }
    }
}
