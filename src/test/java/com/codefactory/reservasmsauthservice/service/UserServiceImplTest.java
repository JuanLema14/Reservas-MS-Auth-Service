package com.codefactory.reservasmsauthservice.service;

import com.codefactory.reservasmsauthservice.dto.response.UserResponseDTO;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.mapper.UserMapper;
import com.codefactory.reservasmsauthservice.repository.UserRepository;
import com.codefactory.reservasmsauthservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * UserServiceImpl — consultas generales de usuario
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MS-Auth - UserServiceImpl - Consultas de usuario")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User usuario;
    private UserResponseDTO usuarioResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        usuario = new User();
        usuario.setIdUsuario(userId);
        usuario.setEmail("carlos@email.com");
        usuario.setTipoUsuario(User.Role.CLIENTE);
        usuario.setEstado("ACTIVO");

        usuarioResponse = UserResponseDTO.builder()
                .idUsuario(userId)
                .email("carlos@email.com")
                .tipoUsuario("CLIENTE")
                .estado("ACTIVO")
                .build();
    }

    // =========================================================================
    // findByEmail
    // =========================================================================

    @Test
    @DisplayName("findByEmail: email registrado → retorna UserResponseDTO")
    void findByEmail_EmailRegistrado_RetornaUserResponseDTO() {
        // Arrange
        when(userRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(usuario));
        when(userMapper.toDto(usuario)).thenReturn(usuarioResponse);

        // Act
        UserResponseDTO resultado = userService.findByEmail("carlos@email.com");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("carlos@email.com");
        assertThat(resultado.getTipoUsuario()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("findByEmail: email no registrado → lanza ResourceNotFoundException")
    void findByEmail_EmailNoRegistrado_LanzaResourceNotFoundException() {
        // Arrange
        when(userRepository.findByEmail("noexiste@email.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findByEmail("noexiste@email.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrado");

        verify(userMapper, never()).toDto(any());
    }

    // =========================================================================
    // existsByEmail
    // =========================================================================

    @Test
    @DisplayName("existsByEmail: email existente → retorna true")
    void existsByEmail_EmailExistente_RetornaTrue() {
        // Arrange
        when(userRepository.existsByEmail("carlos@email.com")).thenReturn(true);

        // Act
        boolean resultado = userService.existsByEmail("carlos@email.com");

        // Assert
        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("existsByEmail: email no existente → retorna false")
    void existsByEmail_EmailNoExistente_RetornaFalse() {
        // Arrange
        when(userRepository.existsByEmail("nuevo@email.com")).thenReturn(false);

        // Act
        boolean resultado = userService.existsByEmail("nuevo@email.com");

        // Assert
        assertThat(resultado).isFalse();
    }
}
