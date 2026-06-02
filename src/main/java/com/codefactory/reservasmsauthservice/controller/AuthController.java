package com.codefactory.reservasmsauthservice.controller;

import com.codefactory.reservasmsauthservice.dto.request.ChangePasswordDTO;
import com.codefactory.reservasmsauthservice.dto.request.CreateClientRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.CreateProviderRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.LoginRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.LogoutRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.PasswordResetConfirmDTO;
import com.codefactory.reservasmsauthservice.dto.request.PasswordResetRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.RefreshTokenRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.LoginResponseDTO;
import com.codefactory.reservasmsauthservice.dto.response.MessageResponseDTO;
import com.codefactory.reservasmsauthservice.dto.response.RegistrationResponseDTO;
import com.codefactory.reservasmsauthservice.service.AuthService;
import com.codefactory.reservasmsauthservice.service.LoginService;
import com.codefactory.reservasmsauthservice.service.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Controlador de autenticación.
 * Maneja los endpoints de registro de usuarios (cliente y proveedor)
 * con verificación por email, login, refresh token, logout y gestión de contraseñas.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para registro, login, gestión de tokens y contraseñas")
public class AuthController {

    private final AuthService authService;
    private final LoginService loginService;
    private final PasswordService passwordService;

    private static final URI VERIFY_EMAIL_URI = URI.create("/api/auth/verify-email");
    private static final URI LOGIN_URI = URI.create("/api/auth/login");
    private static final URI CONFIRM_RESET_URI = URI.create("/api/auth/password-reset/confirm");

    /**
     * Registra un nuevo cliente.
     */
    @PostMapping("/register/client")
    @Operation(
        summary = "Registrar cliente",
        description = "Crea una nueva cuenta de cliente con los datos proporcionados. Se envía un email de verificación.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del cliente a registrar",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateClientRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cliente registrado exitosamente", content = @Content(schema = @Schema(implementation = RegistrationResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de registro inválidos"),
        @ApiResponse(responseCode = "409", description = "El email ya está registrado")
    })
    @SecurityRequirements
    public ResponseEntity<EntityModel<RegistrationResponseDTO>> registerClient(
            @Valid @RequestBody CreateClientRequestDTO request) {
        RegistrationResponseDTO response = authService.registerClient(request);
        EntityModel<RegistrationResponseDTO> entityModel = EntityModel.of(response,
            Link.of(VERIFY_EMAIL_URI.toString()).withRel("verify-email"),
            Link.of(LOGIN_URI.toString()).withRel("login"));
        return new ResponseEntity<>(entityModel, HttpStatus.CREATED);
    }

    /**
     * Registra un nuevo proveedor.
     */
    @PostMapping("/register/provider")
    @Operation(
        summary = "Registrar proveedor",
        description = "Crea una nueva cuenta de proveedor con los datos proporcionados. Se envía un email de verificación.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del proveedor a registrar",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateProviderRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Proveedor registrado exitosamente", content = @Content(schema = @Schema(implementation = RegistrationResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de registro inválidos"),
        @ApiResponse(responseCode = "409", description = "El email ya está registrado")
    })
    @SecurityRequirements
    public ResponseEntity<EntityModel<RegistrationResponseDTO>> registerProvider(
            @Valid @RequestBody CreateProviderRequestDTO request) {
        RegistrationResponseDTO response = authService.registerProvider(request);
        EntityModel<RegistrationResponseDTO> entityModel = EntityModel.of(response,
            Link.of(VERIFY_EMAIL_URI.toString()).withRel("verify-email"),
            Link.of(LOGIN_URI.toString()).withRel("login"));
        return new ResponseEntity<>(entityModel, HttpStatus.CREATED);
    }

    /**
     * Inicia sesión de usuario.
     */
    @PostMapping("/login")
    @Operation(
        summary = "Iniciar sesión",
        description = "Autentica un usuario con email y contraseña. Retorna tokens JWT para acceso a la API.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Credenciales de login",
            required = true,
            content = @Content(schema = @Schema(implementation = LoginRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login exitoso", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
        @ApiResponse(responseCode = "403", description = "Cuenta bloqueada o email no verificado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @SecurityRequirements
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request, HttpServletRequest httpRequest) {
        LoginResponseDTO response = loginService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Renueva el access token usando un refresh token.
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Renovar token",
        description = "Genera un nuevo access token usando un refresh token válido. El refresh token es rotado por seguridad.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Refresh token",
            required = true,
            content = @Content(schema = @Schema(implementation = RefreshTokenRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token renovado exitosamente", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "Refresh token revocado")
    })
    @SecurityRequirements
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        LoginResponseDTO response = loginService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Cierra sesión revocando el refresh token.
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Cerrar sesión",
        description = "Revoca el refresh token del usuario, invalidando la sesión activa.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Refresh token a revocar",
            required = true,
            content = @Content(schema = @Schema(implementation = LogoutRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout exitoso"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequestDTO request) {
        loginService.logout(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Solicita el restablecimiento de contraseña.
     */
    @PostMapping("/password-reset/request")
    @Operation(
        summary = "Solicitar restablecimiento de contraseña",
        description = "Envía un email con un enlace seguro para restablecer la contraseña del usuario.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Email del usuario",
            required = true,
            content = @Content(schema = @Schema(implementation = PasswordResetRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email enviado si el usuario existe"),
        @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes (rate limiting)")
    })
    @SecurityRequirements
    public ResponseEntity<EntityModel<MessageResponseDTO>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDTO request,
            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        passwordService.requestPasswordReset(request, ipAddress);
        MessageResponseDTO message = new MessageResponseDTO("Si el email existe en nuestro sistema, recibirás un enlace para restablecer tu contraseña");
        EntityModel<MessageResponseDTO> entityModel = EntityModel.of(message,
            Link.of(CONFIRM_RESET_URI.toString()).withRel("confirm-reset"));
        return ResponseEntity.ok(entityModel);
    }

    /**
     * Confirma el restablecimiento de contraseña con un token válido.
     */
    @PostMapping("/password-reset/confirm")
    @Operation(
        summary = "Confirmar restablecimiento de contraseña",
        description = "Establece la nueva contraseña usando el token recibido por email.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Token y nueva contraseña",
            required = true,
            content = @Content(schema = @Schema(implementation = PasswordResetConfirmDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente"),
        @ApiResponse(responseCode = "400", description = "Token inválido o expirado")
    })
    @SecurityRequirements
    public ResponseEntity<EntityModel<MessageResponseDTO>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmDTO request) {
        MessageResponseDTO response = passwordService.confirmPasswordReset(request);
        EntityModel<MessageResponseDTO> entityModel = EntityModel.of(response,
            Link.of(LOGIN_URI.toString()).withRel("login"));
        return ResponseEntity.ok(entityModel);
    }

    /**
     * Cambia la contraseña de un usuario autenticado.
     */
    @PostMapping("/change-password")
    @Operation(
        summary = "Cambiar contraseña",
        description = "Permite a un usuario autenticado cambiar su contraseña actual por una nueva.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Contraseña actual y nueva contraseña",
            required = true,
            content = @Content(schema = @Schema(implementation = ChangePasswordDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contraseña cambiada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Contraseña actual incorrecta"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<EntityModel<MessageResponseDTO>> changePassword(
            @Valid @RequestBody ChangePasswordDTO request) {
        MessageResponseDTO response = passwordService.changePassword(request);
        return ResponseEntity.ok(EntityModel.of(response));
    }
}