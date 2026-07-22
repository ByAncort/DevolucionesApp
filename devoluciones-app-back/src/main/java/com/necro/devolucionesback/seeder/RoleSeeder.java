package com.necro.devolucionesback.seeder;

import com.necro.devolucionesback.model.Role;
import com.necro.devolucionesback.repository.RoleRepository;
import com.necro.devolucionesback.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RoleSeeder implements CommandLineRunner {
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRoles();
    }

    private void seedRoles() {
        List<String> roleNames = List.of("SUPERVISOR", "ANALISTA");

        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName).orElseGet(() -> {
                Role newRole = roleRepository.save(Role.builder()
                        .name(roleName)
                        .build());
                log.info("Created role: {}", roleName);
                return newRole;
            });
        }
    }
}
