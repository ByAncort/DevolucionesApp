package com.necro.devolucionesback.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechazoRequest {
    @NotBlank(message = "El motivo de rechazo es obligatorio")
    private String motivoRechazo;
}
