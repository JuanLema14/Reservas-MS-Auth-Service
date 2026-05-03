package com.codefactory.reservasmsauthservice.service;

import com.codefactory.reservasmsauthservice.entity.EmailVerificationToken;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.InvalidVerificationTokenException;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.repository.EmailVerificationTokenRepository;
import com.codefactory.reservasmsauthservice.service.impl.EmailVerificationTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * EmailVerificationTokenServiceImpl
 * Cubre la lógica de generación y validación de tokens de verificación de email
 * utilizada por HU-01 y HU-02.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MS-Auth - EmailVerificationTokenServiceImpl - Tokens de verificación")
class EmailVerificationTokenServiceImplTest {

    @Mock private EmailVerificationTokenRepository tokenRepository;

    @InjectMocks
    private EmailVerificationTokenServiceImpl tokenService;

    private UUID userId;
    private User usuario;
    private EmailVerificationToken tokenValido;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        usuario = new User();
        usuario.setIdUsuario(userId);
        usuario.setEmail("carlos@email.com");

        tokenValido = new EmailVerificationToken();
        tokenValido.setToken("token-uuid-valido");
        tokenValido.setUser(usuario);
        tokenValido.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        tokenValido.setUsado(false);
    }

    // =========================================================================
    // generateToken
    // =========================================================================

    @Test
    @DisplayName("generateToken: genera token UUID, elimina el anterior y lo persiste")
    void generateToken_UsuarioValido_GeneraYPersistToken() {
        // Arrange
        doNothing().when(tokenRepository).deleteByUser_IdUsuario(userId);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenReturn(tokenValido);

        // Act
        String resultado = tokenService.generateToken(usuario);

        // Assert
        assertThat(resultado).isNotNull().isNotBlank();
        verify(tokenRepository, times(1)).deleteByUser_IdUsuario(userId);
        verify(tokenRepository, times(1)).save(any(EmailVerificationToken.class));
    }

    @Test
    @DisplayName("generateToken: el token generado tiene 24h de expiración")
    void generateToken_TokenTiene24HorasExpiracion() {
        // Arrange
        doNothing().when(tokenRepository).deleteByUser_IdUsuario(userId);
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        when(tokenRepository.save(captor.capture())).thenReturn(tokenValido);

        // Act
        tokenService.generateToken(usuario);

        // Assert
        EmailVerificationToken tokenGuardado = captor.getValue();
        assertThat(tokenGuardado.getFechaExpiracion())
                .isAfter(LocalDateTime.now().plusHours(23))
                .isBefore(LocalDateTime.now().plusHours(25));
        assertThat(tokenGuardado.getUsado()).isFalse();
    }

    @Test
    @DisplayName("generateToken: cada llamada genera un token diferente (UUID único)")
    void generateToken_CadaLlamadaGeneraTokenDistinto() {
        // Arrange
        doNothing().when(tokenRepository).deleteByUser_IdUsuario(any());
        when(tokenRepository.save(any())).thenReturn(tokenValido);

        // Act
        String token1 = tokenService.generateToken(usuario);
        String token2 = tokenService.generateToken(usuario);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }

    // =========================================================================
    // validateToken
    // =========================================================================

    @Test
    @DisplayName("validateToken: token válido → retorna EmailVerificationToken")
    void validateToken_TokenValido_RetornaToken() {
        // Arrange
        when(tokenRepository.findValidByToken("token-uuid-valido"))
                .thenReturn(Optional.of(tokenValido));

        // Act
        EmailVerificationToken resultado = tokenService.validateToken("token-uuid-valido");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getToken()).isEqualTo("token-uuid-valido");
        assertThat(resultado.getUsado()).isFalse();
    }

    @Test
    @DisplayName("validateToken: token inválido/expirado → lanza InvalidVerificationTokenException")
    void validateToken_TokenInvalido_LanzaInvalidVerificationTokenException() {
        // Arrange
        when(tokenRepository.findValidByToken("token-expirado")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tokenService.validateToken("token-expirado"))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessageContaining("inválido");
    }

    // =========================================================================
    // confirmToken
    // =========================================================================

    @Test
    @DisplayName("confirmToken: token válido no usado → lo marca como usado y retorna true")
    void confirmToken_TokenValido_MarcaComoUsadoRetornaTrue() {
        // Arrange
        when(tokenRepository.findValidByToken("token-uuid-valido"))
                .thenReturn(Optional.of(tokenValido));
        when(tokenRepository.save(tokenValido)).thenReturn(tokenValido);

        // Act
        boolean resultado = tokenService.confirmToken("token-uuid-valido");

        // Assert
        assertThat(resultado).isTrue();
        assertThat(tokenValido.getUsado()).isTrue();
        verify(tokenRepository, times(1)).save(tokenValido);
    }

    @Test
    @DisplayName("confirmToken: token ya usado → lanza InvalidVerificationTokenException")
    void confirmToken_TokenYaUsado_LanzaInvalidVerificationTokenException() {
        // Arrange
        tokenValido.setUsado(true);
        when(tokenRepository.findValidByToken("token-ya-usado"))
                .thenReturn(Optional.of(tokenValido));

        // Act & Assert
        assertThatThrownBy(() -> tokenService.confirmToken("token-ya-usado"))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessageContaining("ya ha sido utilizado");
    }

    // =========================================================================
    // getActiveTokenByUserId
    // =========================================================================

    @Test
    @DisplayName("getActiveTokenByUserId: token activo existente → retorna token")
    void getActiveTokenByUserId_TokenActivo_RetornaToken() {
        // Arrange
        when(tokenRepository.findByUser_IdUsuarioAndUsadoFalse(userId))
                .thenReturn(Optional.of(tokenValido));

        // Act
        EmailVerificationToken resultado = tokenService.getActiveTokenByUserId(userId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getUsado()).isFalse();
    }

    @Test
    @DisplayName("getActiveTokenByUserId: sin token activo → lanza ResourceNotFoundException")
    void getActiveTokenByUserId_SinTokenActivo_LanzaResourceNotFoundException() {
        // Arrange
        when(tokenRepository.findByUser_IdUsuarioAndUsadoFalse(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tokenService.getActiveTokenByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("token de verificación activo");
    }

    // =========================================================================
    // deleteTokensByUserId
    // =========================================================================

    @Test
    @DisplayName("deleteTokensByUserId: delega correctamente en el repositorio")
    void deleteTokensByUserId_DelegaEnRepositorio() {
        // Arrange
        doNothing().when(tokenRepository).deleteByUser_IdUsuario(userId);

        // Act
        tokenService.deleteTokensByUserId(userId);

        // Assert
        verify(tokenRepository, times(1)).deleteByUser_IdUsuario(userId);
    }
}
