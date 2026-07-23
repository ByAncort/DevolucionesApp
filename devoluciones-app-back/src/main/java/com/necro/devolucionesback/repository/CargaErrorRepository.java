package com.necro.devolucionesback.repository;

import com.necro.devolucionesback.model.CargaError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CargaErrorRepository extends JpaRepository<CargaError, Long> {
    List<CargaError> findByCargaIdOrderByNumFilaAsc(Long cargaId);
}
