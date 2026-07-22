package com.necro.devolucionesback.dto;

import com.necro.devolucionesback.model.Estado;
import com.necro.devolucionesback.model.Solicitud;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public record SolicitudResponseDTO(
        long id,
        String folio,
        String rutCliente,
        String nombreCliente,
        Double monto,

        // Aplanamos el Banco para no arrastrar la entidad completa
        Long bancoDestinoId,
        String bancoDestinoNombre,

        String cuentaDestino,
        Estado estado,
        String motivoRechazo,

        // Datos de auditoría simplificados
        String creadoPorNombre,
        String actualizadoPorNombre,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
    public static SolicitudResponseDTO fromEntity(Solicitud solicitud) {
    return new SolicitudResponseDTO(
            solicitud.getId(),
            solicitud.getFolio(),
            solicitud.getRutCliente(),
            solicitud.getNombreCliente(),
            solicitud.getMonto(),

            solicitud.getBancoDestino() != null ? solicitud.getBancoDestino().getId() : null,
            solicitud.getBancoDestino() != null ? solicitud.getBancoDestino().getNombre_banco() : null,

            solicitud.getCuentaDestino(),
            solicitud.getEstado(),
            solicitud.getMotivoRechazo(),

            solicitud.getCreatedBy() != null ? solicitud.getCreatedBy().getUsername() : "Sistema",
            solicitud.getUpdatedBy() != null ? solicitud.getUpdatedBy().getUsername() : "Sistema",
            solicitud.getCreatedAt(),
            solicitud.getUpdatedAt()
    );
}

}
