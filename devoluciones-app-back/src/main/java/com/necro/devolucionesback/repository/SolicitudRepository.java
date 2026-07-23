package com.necro.devolucionesback.repository;

import com.necro.devolucionesback.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long>, JpaSpecificationExecutor<Solicitud> {
    @Query("SELECT COUNT(s) FROM Solicitud s WHERE YEAR(s.createdAt) = :anio")
    long countByAnio(@Param("anio") int anio);

    @Query(value = "SELECT nextval('folio_seq')", nativeQuery = true)
    long nextFolio();
}
