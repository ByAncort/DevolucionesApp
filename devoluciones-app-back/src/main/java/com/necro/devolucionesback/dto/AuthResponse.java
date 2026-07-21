package com.necro.devolucionesback.dto;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private Date issuedAt;
    private Date expiration;
    private String token;

}