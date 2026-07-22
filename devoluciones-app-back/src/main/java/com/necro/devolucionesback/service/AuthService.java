package com.necro.devolucionesback.service;

import com.necro.devolucionesback.dto.AuthResponse;
import com.necro.devolucionesback.dto.LoginRequest;
import com.necro.devolucionesback.model.User;
import com.necro.devolucionesback.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    @Value("${auth.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    public  AuthResponse login(@Valid LoginRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(()-> new BadCredentialsException("Invalid email"));

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            request.getPassword()
                    )
            );
        }catch (Exception e){

        }
        return new AuthResponse();
    }
}
