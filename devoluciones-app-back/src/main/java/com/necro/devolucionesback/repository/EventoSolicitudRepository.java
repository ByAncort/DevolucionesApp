package com.necro.devolucionesback.repository;

import com.necro.devolucionesback.model.EventoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoSolicitudRepository extends JpaRepository<EventoSolicitud, Long> {
    List<EventoSolicitud> findBySolicitudIdOrderByFechaAsc(Long solicitudId);
}
