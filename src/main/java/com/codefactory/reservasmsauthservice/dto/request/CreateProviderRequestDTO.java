package com.codefactory.reservasmsauthservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProviderRequestDTO {

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email es inválido")
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
             message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número")
    private String password;

    @NotBlank(message = "El nombre comercial no puede estar vacío")
    private String nombreComercial;

    @NotNull(message = "La categoría es obligatoria")
    private UUID idCategoria;

    @NotBlank(message = "La dirección no puede estar vacía")
    private String direccion;

    @NotBlank(message = "El teléfono de contacto no puede estar vacío")
    @Pattern(regexp = "^\\d+$", message = "El teléfono solo debe contener números")
    private String telefonoContacto;
}
