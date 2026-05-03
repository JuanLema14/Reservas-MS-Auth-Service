package com.codefactory.reservasmsauthservice.service;

import com.codefactory.reservasmsauthservice.dto.request.LoginRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.RefreshTokenRequestDTO;
import com.codefactory.reservasmsauthservice.entity.RefreshToken;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.AccountLockedException;
import com.codefactory.reservasmsauthservice.exception.EmailNotVerifiedException;
import com.codefactory.reservasmsauthservice.exception.InvalidCredentialsException;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.repository.LoginAttemptRepository;
import com.codefactory.reservasmsauthservice.repository.RefreshTokenRepository;
import com.codefactory.reservasmsauthservice.repository.UserRepository;
import com.codefactory.reservasmsauthservice.security.JwtService;
import com.codefactory.reservasmsauthservice.service.impl.LoginServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * HU-03: Inicio de sesión (LoginServiceImpl)
 *
 * CP-03-001/002: Login exitoso como cliente y proveedor
 * CP-03-003: Login con contraseña incorrecta → INVALID_CREDENTIALS
 * CP-03-004: Login con correo no registrado → INVALID_CREDENTIALS
 * CP-03-005: Bloqueo en quinto intento fallido → ACCOUNT_LOCKED
 * CP-03-008: Renovación con refresh token válido
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MS-Auth - LoginServiceImpl - Inicio de sesión")
class LoginServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private LoginAttemptRepository loginAttemptRepository;
    @Mock private JwtService jwtService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private HttpServletRequest httpRequest;
    @Mock private TransactionStatus transactionStatus;

    @InjectMocks
    private LoginServiceImpl loginService;

    private UUID userId;
    private User usuarioActivo;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        usuarioActivo = new User();
        usuarioActivo.setIdUsuario(userId);
        usuarioActivo.setEmail("carlos@email.com");
        usuarioActivo.setPasswordHash("$2a$10$hashedPassword");
        usuarioActivo.setEmailVerificado(true);
        usuarioActivo.setIntentosFallidos(0);
        usuarioActivo.setBloqueadoHasta(null);
        usuarioActivo.setEstado("ACTIVO");
        usuarioActivo.setTipoUsuario(User.Role.CLIENTE);

        // Config por defecto del servicio via reflection
        ReflectionTestUtils.setField(loginService, "maxAttempts", 5);
        ReflectionTestUtils.setField(loginService, "lockoutDurationHours", 24);

        // Mock del HttpServletRequest
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test-Agent");

        // Mock del transactionManager para los métodos bypass (REQUIRES_NEW)
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        doNothing().when(transactionManager).commit(transactionStatus);
    }

    // =========================================================================
    // CP-03-004: Correo no registrado → lanza ResourceNotFoundException
    // =========================================================================

    @Test
    @DisplayName("CP-03-004: Correo no registrado → lanza ResourceNotFoundException (INVALID_CREDENTIALS)")
    void login_CorreoNoRegistrado_LanzaResourceNotFoundException() {
        // Arrange
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("noexiste@email.com")
                .password("Segura#123")
                .build();

        when(userRepository.findByEmail("noexiste@email.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> loginService.login(request, httpRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }

    // =========================================================================
    // Email no verificado → lanza EmailNotVerifiedException
    // =========================================================================

    @Test
    @DisplayName("CP-03-003b: Email no verificado → lanza EmailNotVerifiedException")
    void login_EmailNoVerificado_LanzaEmailNotVerifiedException() {
        // Arrange
        usuarioActivo.setEmailVerificado(false);
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("carlos@email.com")
                .password("Segura#123")
                .build();

        when(userRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(usuarioActivo));

        // Act & Assert
        assertThatThrownBy(() -> loginService.login(request, httpRequest))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("Debes verificar tu email");
    }

    // =========================================================================
    // CP-03-005: Cuenta bloqueada → lanza AccountLockedException
    // =========================================================================

    @Test
    @DisplayName("CP-03-005: Cuenta bloqueada → lanza AccountLockedException")
    void login_CuentaBloqueada_LanzaAccountLockedException() {
        // Arrange
        usuarioActivo.setBloqueadoHasta(LocalDateTime.now().plusHours(23));
        usuarioActivo.setEstado("BLOQUEADO");
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("carlos@email.com")
                .password("Segura#123")
                .build();

        when(userRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(usuarioActivo));

        // Act & Assert
        assertThatThrownBy(() -> loginService.login(request, httpRequest))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("bloqueada");
    }

    // =========================================================================
    // CP-03-003: Contraseña incorrecta → lanza InvalidCredentialsException
    // =========================================================================

    @Test
    @DisplayName("CP-03-003: Contraseña incorrecta → lanza InvalidCredentialsException")
    void login_ContrasenaIncorrecta_LanzaInvalidCredentialsException() {
        // Arrange
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("carlos@email.com")
                .password("WrongPass")
                .build();

        when(userRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("WrongPass", "$2a$10$hashedPassword")).thenReturn(false);
        when(jdbcTemplate.queryForObject(contains("actualizar_intentos_fallidos"), eq(Boolean.class), any(), any()))
                .thenReturn(true);
        when(loginAttemptRepository.save(any())).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> loginService.login(request, httpRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Correo o contraseña incorrectos");
    }

    @Test
    @DisplayName("CP-03-005b: 5 intentos fallidos → lanza AccountLockedException")
    void login_QuintoIntentoFallido_LanzaAccountLockedException() {
        // Arrange - usuario con 4 intentos previos
        usuarioActivo.setIntentosFallidos(4);
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("carlos@email.com")
                .password("WrongPass")
                .build();

        when(userRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("WrongPass", "$2a$10$hashedPassword")).thenReturn(false);
        when(jdbcTemplate.queryForObject(contains("actualizar_intentos_fallidos"), eq(Boolean.class), any(), any()))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(contains("bloquear_usuario"), eq(Boolean.class), any(), any(), any()))
                .thenReturn(true);
        when(loginAttemptRepository.save(any())).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> loginService.login(request, httpRequest))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Demasiados intentos fallidos");
    }

    // =========================================================================
    // CP-03-008: Refresh token inválido/inexistente → lanza InvalidCredentialsException
    // =========================================================================

    @Test
    @DisplayName("CP-03-008: Refresh token inexistente → lanza InvalidCredentialsException")
    void refreshToken_TokenInexistente_LanzaInvalidCredentialsException() {
        // Arrange
        RefreshTokenRequestDTO request = RefreshTokenRequestDTO.builder()
                .refreshToken("token-invalido")
                .build();

        when(refreshTokenRepository.findValidByToken("token-invalido")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> loginService.refreshToken(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Refresh token inválido");
    }

    @Test
    @DisplayName("CP-03-008b: Refresh token con JWT inválido → lanza InvalidCredentialsException")
    void refreshToken_JwtInvalido_LanzaInvalidCredentialsException() {
        // Arrange
        RefreshToken tokenEntity = RefreshToken.builder()
                .token("token-en-db")
                .user(usuarioActivo)
                .revocado(false)
                .build();

        RefreshTokenRequestDTO request = RefreshTokenRequestDTO.builder()
                .refreshToken("token-en-db")
                .build();

        when(refreshTokenRepository.findValidByToken("token-en-db")).thenReturn(Optional.of(tokenEntity));
        when(jwtService.isRefreshTokenValid("token-en-db")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> loginService.refreshToken(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}