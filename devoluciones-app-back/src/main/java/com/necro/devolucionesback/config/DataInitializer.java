package com.necro.devolucionesback.config;

import com.necro.devolucionesback.model.Banco;
import com.necro.devolucionesback.model.Role;
import com.necro.devolucionesback.model.User;
import com.necro.devolucionesback.repository.BancoRepository;
import com.necro.devolucionesback.repository.RoleRepository;
import com.necro.devolucionesback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BancoRepository bancoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedBancos();
        seedAdminUser();
        seedTestUsers();
    }

    private void seedBancos() {
        List<String> bancos = List.of(
                "BANCO CHILE", "BANCO SANTANDER", "BANCO STATE",
                "BANCO ITAÚ", "BANCO BCI", "BANCO SCOTIABANK",
                "BANCO RIPLEY", "BANCO FALABELLA", "BANCO CONSORCIO",
                "BANCO SECURITY", "BANCO COPEUCH", "BANCO DESCONTAR"
        );
        for (String nombre : bancos) {
            if (bancoRepository.findByNombreBancoIgnoreCase(nombre).isEmpty()) {
                bancoRepository.save(Banco.builder().nombreBanco(nombre).build());
                log.info("Created banco: {}", nombre);
            }
        }
    }

    private void seedAdminUser() {
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

    private void seedTestUsers() {
        Role analistaRole = roleRepository.findByName("ANALISTA")
                .orElseThrow(() -> new RuntimeException("ANALISTA role not found"));
        Role supervisorRole = roleRepository.findByName("SUPERVISOR")
                .orElseThrow(() -> new RuntimeException("SUPERVISOR role not found"));

        if (!userRepository.existsByUsername("analista1")) {
            User analista = User.builder()
                    .username("analista1")
                    .email("analista1@devoluciones.local")
                    .password(passwordEncoder.encode("123456"))
                    .roles(Set.of(analistaRole))
                    .build();
            userRepository.save(analista);
            log.info("Test user created (analista1 / 123456)");
        }

        if (!userRepository.existsByUsername("supervisor1")) {
            User supervisor = User.builder()
                    .username("supervisor1")
                    .email("supervisor1@devoluciones.local")
                    .password(passwordEncoder.encode("123456"))
                    .roles(Set.of(supervisorRole))
                    .build();
            userRepository.save(supervisor);
            log.info("Test user created (supervisor1 / 123456)");
        }
    }
}
