package com.necro.devolucionesback.config.jwtService;

import com.necro.devolucionesback.dto.LoginRequest;
import com.necro.devolucionesback.dto.AuthResponse;
import com.necro.devolucionesback.model.User;
import com.necro.devolucionesback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${auth.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    public AuthResponse login(LoginRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
            String token = jwtService.getToken(userDetails, tenantId);

            return AuthResponse.builder()
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .token(token)
                    .build();

        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }
    }
}
