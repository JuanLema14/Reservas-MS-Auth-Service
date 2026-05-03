package com.codefactory.reservasmsauthservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private UUID idUsuario;
    private String email;
    private String tipoUsuario;
    private String estado;
    private LocalDateTime fechaRegistro;
}
