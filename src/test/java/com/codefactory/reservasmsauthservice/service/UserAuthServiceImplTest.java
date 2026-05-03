package com.codefactory.reservasmsauthservice.service;

import com.codefactory.reservasmsauthservice.exception.EmailAlreadyExistsException;
import com.codefactory.reservasmsauthservice.repository.UserRepository;
import com.codefactory.reservasmsauthservice.service.impl.UserAuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * UserAuthServiceImpl — lógica central de validación compartida por HU-01 y HU-02
 *
 * CP-01-002: Correo duplicado → EMAIL_ALREADY_EXISTS
 * CP-01-008: Cobertura de ramas del método validateEmailAndPassword
 * CP-02-005: Correo duplicado entre roles distintos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MS-Auth - UserAuthServiceImpl - Validación de email y contraseña")
class UserAuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAuthServiceImpl userAuthService;

    // =========================================================================
    // CP-01-002 / CP-02-005: Correo duplicado → lanza EmailAlreadyExistsException
    // =========================================================================

    @Test
    @DisplayName("CP-01-002: Correo ya registrado → lanza EmailAlreadyExistsException")
    void validateEmailAndPassword_CorreoDuplicado_LanzaEmailAlreadyExistsException() {
        // Arrange
        when(userRepository.existsByEmail("carlos@email.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userAuthService.validateEmailAndPassword("carlos@email.com", "Segura#123"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("correo electrónico ya está en uso");
    }

    @Test
    @DisplayName("CP-02-005: Correo en uso por otro rol → misma excepción EmailAlreadyExistsException")
    void validateEmailAndPassword_CorreoEnUsoPorOtroRol_LanzaEmailAlreadyExistsException() {
        // Arrange - el repositorio unificado detecta cualquier duplicado sin importar el rol
        when(userRepository.existsByEmail("carlos@email.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userAuthService.validateEmailAndPassword("carlos@email.com", "OtraPass#1"))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    // =========================================================================
    // CP-01-008: Cobertura de ramas - correo nuevo → no lanza excepción
    // =========================================================================

    @Test
    @DisplayName("CP-01-008 (rama falsa): Correo nuevo → no lanza excepción")
    void validateEmailAndPassword_CorreoNuevo_NoLanzaExcepcion() {
        // Arrange
        when(userRepository.existsByEmail("nuevo@email.com")).thenReturn(false);

        // Act & Assert
        assertThatCode(() -> userAuthService.validateEmailAndPassword("nuevo@email.com", "Segura#123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-01-008 (rama verdadera): Correo duplicado interrumpe el flujo antes de encodear")
    void validateEmailAndPassword_CorreoDuplicado_NuncaLlamaAlEncoder() {
        // Arrange
        when(userRepository.existsByEmail("duplicado@email.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userAuthService.validateEmailAndPassword("duplicado@email.com", "Segura#123"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        // El encoder nunca debe ser invocado si el email ya existe
        verify(passwordEncoder, never()).encode(any());
    }

    // =========================================================================
    // encodePassword: el password queda hasheado (CP-01-001b)
    // =========================================================================

    @Test
    @DisplayName("CP-01-001b: encodePassword → delega en PasswordEncoder y retorna hash")
    void encodePassword_DelegaEnPasswordEncoder() {
        // Arrange
        when(passwordEncoder.encode("Segura#123")).thenReturn("$2a$10$hashedPassword");

        // Act
        String resultado = userAuthService.encodePassword("Segura#123");

        // Assert
        assertThat(resultado).isEqualTo("$2a$10$hashedPassword");
        verify(passwordEncoder, times(1)).encode("Segura#123");
    }

    @Test
    @DisplayName("CP-01-001b: El hash devuelto no es igual a la contraseña original")
    void encodePassword_HashDistintoAlOriginal() {
        // Arrange
        when(passwordEncoder.encode("Segura#123")).thenReturn("$2a$10$diferenteAlOriginal");

        // Act
        String hash = userAuthService.encodePassword("Segura#123");

        // Assert
        assertThat(hash).isNotEqualTo("Segura#123");
    }
}
