package com.necro.devolucionesback.service;

import com.necro.devolucionesback.exception.UserAlreadyExistsException;
import com.necro.devolucionesback.service.jwtUtils.JwtService;
import com.necro.devolucionesback.dto.LoginRequest;
import com.necro.devolucionesback.dto.AuthResponse;
import com.necro.devolucionesback.dto.RegisterRequest;
import com.necro.devolucionesback.model.User;
import com.necro.devolucionesback.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.management.relation.RoleNotFoundException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

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

    public AuthResponse createUser(@Valid RegisterRequest request) {
        Objects.requireNonNull(request, "RegisterRequest cannot be null");
        this.validateUserDoesNotExist(request.getEmail());
        try {
            User savedUser = registerAndSaveUser(request);
            return buildAuthResponse(savedUser);

        }catch (Exception e) {
            log.error("Unexpected error during user registration", e);
            throw new ServiceException("Registration failed due to unexpected error", e);
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusMillis(jwtExpirationMs);
        Long tenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.emptyList()
        );

        String token = jwtService.getToken(userDetails, tenantId);

        return AuthResponse.builder()
                .token(token)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .build();
    }

    private User registerAndSaveUser(RegisterRequest request) {
        User user = registerUser(request);
        return userRepository.save(user);
    }

    public User registerUser(RegisterRequest userRequest) {

        if (userRequest == null) {
            throw new IllegalArgumentException("RegisterRequest no puede ser nulo");
        }
        return User.builder()
                .username(userRequest.getUsername())
                .email(userRequest.getEmail())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .build();
    }

    private void validateUserDoesNotExist( String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email ya existe: " + email);
        }
    }
}
