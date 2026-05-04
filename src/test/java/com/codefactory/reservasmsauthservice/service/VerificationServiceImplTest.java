package com.codefactory.reservasmsauthservice.service;

import com.codefactory.reservasmsauthservice.dto.response.VerificationResponseDTO;
import com.codefactory.reservasmsauthservice.entity.Client;
import com.codefactory.reservasmsauthservice.entity.EmailVerificationToken;
import com.codefactory.reservasmsauthservice.entity.Provider;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.repository.ClientRepository;
import com.codefactory.reservasmsauthservice.repository.EmailVerificationTokenRepository;
import com.codefactory.reservasmsauthservice.repository.ProviderRepository;
import com.codefactory.reservasmsauthservice.repository.UserRepository;
import com.codefactory.reservasmsauthservice.service.impl.VerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * VerificationServiceImpl — verificación de email y reenvío de token
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MS-Auth - VerificationServiceImpl - Verificación de email")
class VerificationServiceImplTest {

    @Mock private EmailVerificationTokenService emailVerificationTokenService;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private ClientRepository clientRepository;
    @Mock private ProviderRepository providerRepository;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    private UUID userId;
    private User usuario;
    private Client clienteEntity;
    private Provider proveedorEntity;
    private EmailVerificationToken tokenValido;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        usuario = new User();
        usuario.setIdUsuario(userId);
        usuario.setEmail("carlos@email.com");
        usuario.setEmailVerificado(false);
        usuario.setTipoUsuario(User.Role.CLIENTE);

        clienteEntity = new Client();
        clienteEntity.setIdUsuario(userId);
        clienteEntity.setEmail("carlos@email.com");
        clienteEntity.setNombre("Carlos Pérez");

        proveedorEntity = new Provider();
        proveedorEntity.setIdUsuario(userId);
        proveedorEntity.setEmail("salon@bellavida.com");
        proveedorEntity.setNombreComercial("Salón Bella Vida");

        tokenValido = new EmailVerificationToken();
        tokenValido.setToken("token-valido-uuid");
        tokenValido.setUser(usuario);
        tokenValido.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        tokenValido.setUsado(false);
    }

    // =========================================================================
    // verifyEmail
    // =========================================================================

    @Test
    @DisplayName("verifyEmail: token válido → marca email verificado y retorna respuesta exitosa")
    void verifyEmail_TokenValido_RetornaRespuestaExitosa() {
        // Arrange
        when(emailVerificationTokenService.validateToken("token-valido-uuid"))
                .thenReturn(tokenValido);
        when(emailVerificationTokenService.confirmToken("token-valido-uuid")).thenReturn(true);
        when(userRepository.save(usuario)).thenReturn(usuario);

        // Act
        VerificationResponseDTO resultado = verificationService.verifyEmail("token-valido-uuid");

        // Assert
        assertThat(resultado.getSuccess()).isTrue();
        assertThat(resultado.getEmail()).isEqualTo("carlos@email.com");
        assertThat(resultado.getUserId()).isEqualTo(userId);
        assertThat(usuario.getEmailVerificado()).isTrue();
        verify(userRepository, times(1)).save(usuario);
    }

    @Test
    @DisplayName("verifyEmail: token inválido → propaga excepción del tokenService")
    void verifyEmail_TokenInvalido_PropagaExcepcion() {
        // Arrange
        when(emailVerificationTokenService.validateToken("token-invalido"))
                .thenThrow(new com.codefactory.reservasmsauthservice.exception.InvalidVerificationTokenException(
                        "Token inválido", true));

        // Act & Assert
        assertThatThrownBy(() -> verificationService.verifyEmail("token-invalido"))
                .isInstanceOf(com.codefactory.reservasmsauthservice.exception.InvalidVerificationTokenException.class);

        verify(userRepository, never()).save(any());
    }

    // =========================================================================
    // resendVerificationToken
    // =========================================================================

    @Test
    @DisplayName("resendVerificationToken: usuario CLIENTE no verificado → reenvía token exitosamente")
    void resendVerificationToken_ClienteNoVerificado_ReenviaToken() {
        // Arrange
        when(userRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(usuario));
        doNothing().when(emailVerificationTokenRepository).deleteByUser_IdUsuario(userId);
        when(emailVerificationTokenService.generateToken(usuario)).thenReturn("nuevo-token-uuid");
        when(clientRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(clienteEntity));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Act
        VerificationResponseDTO resultado = verificationService.resendVerificationToken("carlos@email.com");

        // Assert
        assertThat(resultado.getSuccess()).isTrue();
        assertThat(resultado.getMessage()).contains("reenviado");
        verify(emailService, times(1)).sendVerificationEmail("carlos@email.com", "Carlos Pérez", "nuevo-token-uuid");
    }

    @Test
    @DisplayName("resendVerificationToken: usuario PROVEEDOR no verificado → reenvía token exitosamente")
    void resendVerificationToken_ProveedorNoVerificado_ReenviaToken() {
        // Arrange
        usuario.setEmail("salon@bellavida.com");
        usuario.setTipoUsuario(User.Role.PROVEEDOR);
        usuario.setEmailVerificado(false);

        when(userRepository.findByEmail("salon@bellavida.com")).thenReturn(Optional.of(usuario));
        doNothing().when(emailVerificationTokenRepository).deleteByUser_IdUsuario(userId);
        when(emailVerificationTokenService.generateToken(usuario)).thenReturn("nuevo-token-uuid");
        when(providerRepository.findByEmail("salon@bellavida.com")).thenReturn(Optional.of(proveedorEntity));
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Act
        VerificationResponseDTO resultado = verificationService.resendVerificationToken("salon@bellavida.com");

        // Assert
        assertThat(resultado.getSuccess()).isTrue();
        verify(emailService, times(1))
                .sendVerificationEmail("salon@bellavida.com", "Salón Bella Vida", "nuevo-token-uuid");
    }

    @Test
    @DisplayName("resendVerificationToken: email ya verificado → retorna respuesta informativa sin reenviar")
    void resendVerificationToken_EmailYaVerificado_RetornaRespuestaInformativa() {
        // Arrange
        usuario.setEmailVerificado(true);
        when(userRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(usuario));

        // Act
        VerificationResponseDTO resultado = verificationService.resendVerificationToken("carlos@email.com");

        // Assert
        assertThat(resultado.getSuccess()).isFalse();
        assertThat(resultado.getMessage()).contains("ya ha sido verificado");
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("resendVerificationToken: email no registrado → lanza ResourceNotFoundException")
    void resendVerificationToken_EmailNoRegistrado_LanzaResourceNotFoundException() {
        // Arrange
        when(userRepository.findByEmail("noexiste@email.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> verificationService.resendVerificationToken("noexiste@email.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }
}
