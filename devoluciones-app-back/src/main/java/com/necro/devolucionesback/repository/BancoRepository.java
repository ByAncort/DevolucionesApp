package com.necro.devolucionesback.repository;

import com.necro.devolucionesback.dto.SolicitudResponseDTO;
import com.necro.devolucionesback.model.Banco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BancoRepository extends JpaRepository<Banco, Long> {
}
