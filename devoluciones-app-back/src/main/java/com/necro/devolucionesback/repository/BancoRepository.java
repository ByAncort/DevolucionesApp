package com.necro.devolucionesback.repository;

import com.necro.devolucionesback.model.Banco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BancoRepository extends JpaRepository<Banco, Long> {
    Optional<Banco> findByNombreBancoIgnoreCase(String nombreBanco);
}
