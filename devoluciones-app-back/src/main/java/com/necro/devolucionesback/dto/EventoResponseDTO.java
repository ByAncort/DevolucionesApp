package com.necro.devolucionesback.dto;

import com.necro.devolucionesback.model.Estado;
import com.necro.devolucionesback.model.EventoSolicitud;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoResponseDTO {
    private Long id;
    private Estado estadoOrigen;
    private Estado estadoDestino;
    private String usuario;
    private LocalDateTime fecha;
    private String comentario;

    public static EventoResponseDTO fromEntity(EventoSolicitud evento) {
        return EventoResponseDTO.builder()
                .id(evento.getId())
                .estadoOrigen(evento.getEstadoOrigen())
                .estadoDestino(evento.getEstadoDestino())
                .usuario(evento.getUsuario().getUsername())
                .fecha(evento.getFecha())
                .comentario(evento.getComentario())
                .build();
    }
}
