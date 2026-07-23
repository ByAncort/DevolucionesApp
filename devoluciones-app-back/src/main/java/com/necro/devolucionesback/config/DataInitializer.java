package com.necro.devolucionesback.config;

import com.necro.devolucionesback.model.Role;
import com.necro.devolucionesback.model.User;
import com.necro.devolucionesback.repository.RoleRepository;
import com.necro.devolucionesback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Role supervisorRole = roleRepository.findByName("SUPERVISOR")
                .orElseThrow(() -> new RuntimeException("SUPERVISOR role not found"));

        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@devoluciones.local")
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Set.of(supervisorRole))
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created (admin / admin123)");
        }
    }
}
