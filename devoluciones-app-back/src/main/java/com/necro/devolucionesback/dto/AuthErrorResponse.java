package com.necro.devolucionesback.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public static AuthErrorResponse of(int status, String error, String message, String path) {
        return AuthErrorResponse.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}
