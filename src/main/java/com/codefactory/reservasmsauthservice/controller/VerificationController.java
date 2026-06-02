package com.codefactory.reservasmsauthservice.controller;

import com.codefactory.reservasmsauthservice.dto.request.VerifyEmailRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.VerificationResponseDTO;
import com.codefactory.reservasmsauthservice.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * Controlador de verificación de email.
 * Maneja los endpoints para verificar emails y reenviar tokens de verificación.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Verificación", description = "Endpoints para verificación de email y reenvío de tokens")
public class VerificationController {

    private final VerificationService verificationService;

    /**
     * Verifica el email del usuario usando el token recibido.
     */
    @PostMapping("/verify-email")
    @Operation(
        summary = "Verificar email",
        description = "Verifica el email del usuario usando el token recibido por email tras el registro.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Token de verificación",
            required = true,
            content = @Content(schema = @Schema(implementation = VerifyEmailRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verificado exitosamente", content = @Content(schema = @Schema(implementation = VerificationResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Token inválido o expirado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @SecurityRequirements
    public ResponseEntity<EntityModel<VerificationResponseDTO>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequestDTO request) {
        VerificationResponseDTO response = verificationService.verifyEmail(request.getToken());
        EntityModel<VerificationResponseDTO> entityModel = EntityModel.of(response,
            linkTo(methodOn(VerificationController.class).resendVerificationEmail(null)).withRel("resend-verification-email"));
        return ResponseEntity.ok(entityModel);
    }

    /**
     * Reenvía un nuevo token de verificación al email del usuario.
     */
    @PostMapping("/resend-verification-email")
    @Operation(
        summary = "Reenviar email de verificación",
        description = "Envía un nuevo token de verificación al email del usuario. Útil si el token anterior ha expirado."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email de verificación reenviado exitosamente", content = @Content(schema = @Schema(implementation = VerificationResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "400", description = "Email ya verificado")
    })
    @SecurityRequirements
    public ResponseEntity<EntityModel<VerificationResponseDTO>> resendVerificationEmail(
        @Parameter(description = "Email del usuario", required = true, example = "usuario@ejemplo.com")
        @RequestParam String email) {
        VerificationResponseDTO response = verificationService.resendVerificationToken(email);
        EntityModel<VerificationResponseDTO> entityModel = EntityModel.of(response,
            linkTo(methodOn(VerificationController.class).verifyEmail(null)).withRel("verify-email"));
        return ResponseEntity.ok(entityModel);
    }
}