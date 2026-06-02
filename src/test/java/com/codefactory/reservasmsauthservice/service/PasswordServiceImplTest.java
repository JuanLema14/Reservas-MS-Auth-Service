package com.codefactory.reservasmsauthservice.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.codefactory.reservasmsauthservice.dto.request.ChangePasswordDTO;
import com.codefactory.reservasmsauthservice.dto.request.PasswordResetConfirmDTO;
import com.codefactory.reservasmsauthservice.dto.request.PasswordResetRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.MessageResponseDTO;
import com.codefactory.reservasmsauthservice.entity.PasswordResetToken;
import com.codefactory.reservasmsauthservice.entity.RefreshToken;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.IncorrectPasswordException;
import com.codefactory.reservasmsauthservice.exception.InvalidPasswordException;
import com.codefactory.reservasmsauthservice.exception.InvalidResetTokenException;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.exception.SamePasswordException;
import com.codefactory.reservasmsauthservice.repository.PasswordResetTokenRepository;
import com.codefactory.reservasmsauthservice.repository.RefreshTokenRepository;
import com.codefactory.reservasmsauthservice.repository.UserRepository;
import com.codefactory.reservasmsauthservice.service.impl.PasswordServiceImpl;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * PasswordServiceImpl — restablecimiento y cambio de contraseña
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MS-Auth - PasswordServiceImpl - Gestión de contraseñas")
class PasswordServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordResetTokenRepository passwordResetTokenRepository;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private EmailService emailService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private PasswordServiceImpl passwordService;

  private UUID userId;
  private User usuario;
  private PasswordResetToken resetToken;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();

    usuario = new User();
    usuario.setIdUsuario(userId);
    usuario.setEmail("carlos@email.com");
    usuario.setPasswordHash("$2a$10$hashActual");
    usuario.setTipoUsuario(User.Role.CLIENTE);

    resetToken =
      PasswordResetToken
        .builder()
        .idResetToken(UUID.randomUUID())
        .user(usuario)
        .token("reset-token-valido")
        .expiryDate(LocalDateTime.now().plusHours(24))
        .usado(false)
        .build();

    when(refreshTokenRepository.findByUser_IdUsuarioAndRevocadoFalse(any()))
      .thenReturn(Collections.emptyList());
    when(userRepository.findClientNameByUserId(any()))
      .thenReturn(Optional.of("Carlos Pérez"));
    doNothing()
      .when(emailService)
      .sendPasswordChangeConfirmationEmail(anyString(), anyString());
    doNothing()
      .when(emailService)
      .sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // =========================================================================
  // requestPasswordReset
  // =========================================================================

  @Test
  @DisplayName(
    "requestPasswordReset: email registrado → genera token y envía correo"
  )
  void requestPasswordReset_EmailRegistrado_GeneraTokenYEnviaCorreo() {
    // Arrange
    PasswordResetRequestDTO request = new PasswordResetRequestDTO();
    request.setEmail("carlos@email.com");

    when(userRepository.findByEmail("carlos@email.com"))
      .thenReturn(Optional.of(usuario));
    when(
      passwordResetTokenRepository.findByUser_IdUsuarioAndUsadoFalseOrderByCreatedAtDesc(
        userId
      )
    )
      .thenReturn(Collections.emptyList());
    when(passwordResetTokenRepository.save(any())).thenReturn(resetToken);

    // Act
    passwordService.requestPasswordReset(request, "127.0.0.1");

    // Assert
    verify(passwordResetTokenRepository, times(1))
      .save(any(PasswordResetToken.class));
    verify(emailService, times(1))
      .sendPasswordResetEmail(eq("carlos@email.com"), anyString(), anyString());
  }

  @Test
  @DisplayName(
    "requestPasswordReset: email no registrado → no falla (seguridad) y no envía correo"
  )
  void requestPasswordReset_EmailNoRegistrado_NoFallaNoEnviaCorreo() {
    // Arrange
    PasswordResetRequestDTO request = new PasswordResetRequestDTO();
    request.setEmail("noexiste@email.com");

    when(userRepository.findByEmail("noexiste@email.com"))
      .thenReturn(Optional.empty());

    // Act — no debe lanzar excepción
    assertThatNoException()
      .isThrownBy(() ->
        passwordService.requestPasswordReset(request, "127.0.0.1")
      );

    // Assert
    verify(emailService, never())
      .sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  // =========================================================================
  // confirmPasswordReset
  // =========================================================================

  @Test
  @DisplayName(
    "confirmPasswordReset: token válido y contraseña válida → restablece contraseña"
  )
  void confirmPasswordReset_TokenValidoContrasenaValida_RestablaceContrasena() {
    // Arrange
    PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
    request.setToken("reset-token-valido");
    request.setNewPassword("NuevaSegura#1");

    when(passwordResetTokenRepository.findValidByToken("reset-token-valido"))
      .thenReturn(Optional.of(resetToken));
    when(passwordEncoder.encode("NuevaSegura#1"))
      .thenReturn("$2a$10$nuevoHash");
    when(userRepository.save(usuario)).thenReturn(usuario);

    // Act
    MessageResponseDTO resultado = passwordService.confirmPasswordReset(
      request
    );

    // Assert
    assertThat(resultado.getMessage()).contains("exitosamente");
    assertThat(resetToken.getUsado()).isTrue();
    verify(userRepository, times(1)).save(usuario);
  }

  @Test
  @DisplayName(
    "confirmPasswordReset: token inválido → lanza InvalidResetTokenException"
  )
  void confirmPasswordReset_TokenInvalido_LanzaInvalidResetTokenException() {
    // Arrange
    PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
    request.setToken("token-invalido");
    request.setNewPassword("NuevaSegura#1");

    when(passwordResetTokenRepository.findValidByToken("token-invalido"))
      .thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> passwordService.confirmPasswordReset(request))
      .isInstanceOf(InvalidResetTokenException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName(
    "confirmPasswordReset: contraseña sin mayúscula → lanza InvalidPasswordException"
  )
  void confirmPasswordReset_ContrasenaSinMayuscula_LanzaInvalidPasswordException() {
    // Arrange
    PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
    request.setToken("reset-token-valido");
    request.setNewPassword("sinmayuscula1#");

    // Act & Assert
    assertThatThrownBy(() -> passwordService.confirmPasswordReset(request))
      .isInstanceOf(InvalidPasswordException.class)
      .hasMessageContaining("mayúscula");
  }

  @Test
  @DisplayName(
    "confirmPasswordReset: contraseña menor a 8 caracteres → lanza InvalidPasswordException"
  )
  void confirmPasswordReset_ContrasenaCorta_LanzaInvalidPasswordException() {
    // Arrange
    PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
    request.setToken("reset-token-valido");
    request.setNewPassword("Ab1#");

    // Act & Assert
    assertThatThrownBy(() -> passwordService.confirmPasswordReset(request))
      .isInstanceOf(InvalidPasswordException.class)
      .hasMessageContaining("caracteres");
  }

  // =========================================================================
  // changePassword
  // =========================================================================

  @Test
  @DisplayName(
    "changePassword: usuario autenticado con contraseña correcta → cambia contraseña"
  )
  void changePassword_UsuarioAutenticadoContrasenaCorrecta_CambiaContrasena() {
    // Arrange
    SecurityContextHolder
      .getContext()
      .setAuthentication(
        new UsernamePasswordAuthenticationToken(
          "carlos@email.com",
          null,
          List.of()
        )
      );

    ChangePasswordDTO request = new ChangePasswordDTO();
    request.setCurrentPassword("Segura#123");
    request.setNewPassword("NuevaSegura#1");

    when(userRepository.findByEmail("carlos@email.com"))
      .thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("Segura#123", "$2a$10$hashActual"))
      .thenReturn(true);
    when(passwordEncoder.matches("NuevaSegura#1", "$2a$10$hashActual"))
      .thenReturn(false);
    when(passwordEncoder.encode("NuevaSegura#1"))
      .thenReturn("$2a$10$nuevoHash");
    when(userRepository.save(usuario)).thenReturn(usuario);

    // Act
    MessageResponseDTO resultado = passwordService.changePassword(request);

    // Assert
    assertThat(resultado.getMessage()).contains("exitosamente");
    verify(userRepository, times(1)).save(usuario);
  }

  @Test
  @DisplayName(
    "changePassword: contraseña actual incorrecta → lanza IncorrectPasswordException"
  )
  void changePassword_ContrasenaActualIncorrecta_LanzaIncorrectPasswordException() {
    // Arrange
    SecurityContextHolder
      .getContext()
      .setAuthentication(
        new UsernamePasswordAuthenticationToken(
          "carlos@email.com",
          null,
          List.of()
        )
      );

    ChangePasswordDTO request = new ChangePasswordDTO();
    request.setCurrentPassword("ContrasenaMal#1");
    request.setNewPassword("NuevaSegura#1");

    when(userRepository.findByEmail("carlos@email.com"))
      .thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("ContrasenaMal#1", "$2a$10$hashActual"))
      .thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> passwordService.changePassword(request))
      .isInstanceOf(IncorrectPasswordException.class)
      .hasMessageContaining("incorrecta");
  }

  @Test
  @DisplayName(
    "changePassword: nueva contraseña igual a la actual → lanza SamePasswordException"
  )
  void changePassword_NuevaContrasenaIgualActual_LanzaSamePasswordException() {
    // Arrange
    SecurityContextHolder
      .getContext()
      .setAuthentication(
        new UsernamePasswordAuthenticationToken(
          "carlos@email.com",
          null,
          List.of()
        )
      );

    ChangePasswordDTO request = new ChangePasswordDTO();
    request.setCurrentPassword("Segura#123");
    request.setNewPassword("Segura#123");

    when(userRepository.findByEmail("carlos@email.com"))
      .thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("Segura#123", "$2a$10$hashActual"))
      .thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> passwordService.changePassword(request))
      .isInstanceOf(SamePasswordException.class)
      .hasMessageContaining("igual");
  }

  @Test
  @DisplayName(
    "changePassword: sin autenticación → lanza AuthenticationCredentialsNotFoundException"
  )
  void changePassword_SinAutenticacion_LanzaAuthenticationException() {
    // Arrange — contexto vacío (sin autenticar)
    SecurityContextHolder.clearContext();

    ChangePasswordDTO request = new ChangePasswordDTO();
    request.setCurrentPassword("Segura#123");
    request.setNewPassword("NuevaSegura#1");

    // Act & Assert
    assertThatThrownBy(() -> passwordService.changePassword(request))
      .isInstanceOf(
        org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class
      );
  }

  // =========================================================================
  // validatePasswordFormat — ramas sin minúscula y sin número
  // =========================================================================

  @Test
  @DisplayName(
    "confirmPasswordReset: contraseña sin minúscula → lanza InvalidPasswordException"
  )
  void confirmPasswordReset_ContrasenaSinMinuscula_LanzaInvalidPasswordException() {
    // Arrange
    PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
    request.setToken("reset-token-valido");
    request.setNewPassword("SINMINUSCULA1");

    // Act & Assert
    assertThatThrownBy(() -> passwordService.confirmPasswordReset(request))
      .isInstanceOf(InvalidPasswordException.class)
      .hasMessageContaining("minúscula");
  }

  @Test
  @DisplayName(
    "confirmPasswordReset: contraseña sin número → lanza InvalidPasswordException"
  )
  void confirmPasswordReset_ContrasenaSinNumero_LanzaInvalidPasswordException() {
    // Arrange
    PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
    request.setToken("reset-token-valido");
    request.setNewPassword("SinNumeroMayus#");

    // Act & Assert
    assertThatThrownBy(() -> passwordService.confirmPasswordReset(request))
      .isInstanceOf(InvalidPasswordException.class)
      .hasMessageContaining("número");
  }

  // =========================================================================
  // revokeAllRefreshTokens — rama con tokens reales (forEach)
  // =========================================================================

  @Test
  @DisplayName(
    "confirmPasswordReset: token válido → revoca refresh tokens activos del usuario"
  )
  void confirmPasswordReset_TokenValido_RevocaRefreshTokens() {
    // Arrange
    PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
    request.setToken("reset-token-valido");
    request.setNewPassword("NuevaSegura#1");

    RefreshToken refreshToken = RefreshToken
      .builder()
      .idRefreshToken(UUID.randomUUID())
      .user(usuario)
      .revocado(false)
      .build();

    when(passwordResetTokenRepository.findValidByToken("reset-token-valido"))
      .thenReturn(Optional.of(resetToken));
    when(passwordEncoder.encode("NuevaSegura#1"))
      .thenReturn("$2a$10$nuevoHash");
    when(userRepository.save(usuario)).thenReturn(usuario);
    when(refreshTokenRepository.findByUser_IdUsuarioAndRevocadoFalse(userId))
      .thenReturn(List.of(refreshToken));
    when(refreshTokenRepository.saveAll(anyList()))
      .thenReturn(List.of(refreshToken));

    // Act
    passwordService.confirmPasswordReset(request);

    // Assert — el refresh token quedó revocado
    assertThat(refreshToken.getRevocado()).isTrue();
    assertThat(refreshToken.getFechaRevocacion()).isNotNull();
    verify(refreshTokenRepository, times(1)).saveAll(anyList());
  }

  // =========================================================================
  // getUserName — rama PROVEEDOR
  // =========================================================================

  @Test
  @DisplayName(
    "requestPasswordReset: usuario PROVEEDOR → usa nombre comercial en el email"
  )
  void requestPasswordReset_UsuarioProveedor_UsaNombreComercialEnEmail() {
    // Arrange
    usuario.setTipoUsuario(User.Role.PROVEEDOR);

    PasswordResetRequestDTO request = new PasswordResetRequestDTO();
    request.setEmail("salon@bellavida.com");

    when(userRepository.findByEmail("salon@bellavida.com"))
      .thenReturn(Optional.of(usuario));
    when(
      passwordResetTokenRepository.findByUser_IdUsuarioAndUsadoFalseOrderByCreatedAtDesc(
        userId
      )
    )
      .thenReturn(Collections.emptyList());
    when(passwordResetTokenRepository.save(any())).thenReturn(resetToken);
    when(userRepository.findProviderNameByUserId(userId))
      .thenReturn(Optional.of("Salón Bella Vida"));

    // Act
    passwordService.requestPasswordReset(request, "127.0.0.1");

    // Assert — se envió el email con el nombre del proveedor
    verify(emailService, times(1))
      .sendPasswordResetEmail(
        eq("salon@bellavida.com"),
        eq("Salón Bella Vida"),
        anyString()
      );
  }

  @Test
  void requestPasswordReset_DebeInvalidarTokensAnteriores() {
    PasswordResetToken oldToken = PasswordResetToken
      .builder()
      .token("old")
      .usado(false)
      .build();

    when(userRepository.findByEmail(usuario.getEmail()))
      .thenReturn(Optional.of(usuario));

    when(
      passwordResetTokenRepository.findByUser_IdUsuarioAndUsadoFalseOrderByCreatedAtDesc(
        userId
      )
    )
      .thenReturn(List.of(oldToken));

    PasswordResetRequestDTO request = new PasswordResetRequestDTO();
    request.setEmail(usuario.getEmail());

    passwordService.requestPasswordReset(request, "127.0.0.1");

    assertThat(oldToken.getUsado()).isTrue();
    assertThat(oldToken.getFechaUso()).isNotNull();

    verify(passwordResetTokenRepository).saveAll(anyList());
  }

  @Test
  void changePassword_UsuarioNoExiste_LanzaResourceNotFoundException() {
    SecurityContextHolder
      .getContext()
      .setAuthentication(
        new UsernamePasswordAuthenticationToken(
          "noexiste@email.com",
          null,
          List.of()
        )
      );

    ChangePasswordDTO request = new ChangePasswordDTO();
    request.setCurrentPassword("Actual123");
    request.setNewPassword("Nueva123A");

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> passwordService.changePassword(request))
      .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void changePassword_AnonymousUser_LanzaAuthenticationException() {
    SecurityContextHolder
      .getContext()
      .setAuthentication(
        new UsernamePasswordAuthenticationToken("anonymousUser", null)
      );

    ChangePasswordDTO request = new ChangePasswordDTO();
    request.setCurrentPassword("Actual123");
    request.setNewPassword("Nueva123A");

    assertThatThrownBy(() -> passwordService.changePassword(request))
      .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }
}
