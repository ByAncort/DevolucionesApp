package com.necro.devolucionesback.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambioEstadoRequest {

    @NotBlank(message = "La accion es obligatoria")
    private String accion;

    private String motivoRechazo;
}
