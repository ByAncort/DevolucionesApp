package com.necro.devolucionesback.repository;

import com.necro.devolucionesback.model.Estado;
import com.necro.devolucionesback.model.Origen;
import com.necro.devolucionesback.model.Solicitud;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SolicitudSpec {

    public static Specification<Solicitud> conFiltros(
            Estado estado,
            String rut,
            Origen origen,
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }

            if (rut != null && !rut.isBlank()) {
                predicates.add(cb.equal(root.get("rutCliente"), rut));
            }

            if (origen != null) {
                predicates.add(cb.equal(root.get("origen"), origen));
            }

            if (fechaInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), fechaInicio.atStartOfDay()));
            }

            if (fechaFin != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"), fechaFin.atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
