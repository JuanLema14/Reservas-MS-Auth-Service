package com.codefactory.reservasmsauthservice.service;

import com.codefactory.reservasmsauthservice.dto.request.CreateClientRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.ClientResponseDTO;
import com.codefactory.reservasmsauthservice.entity.Client;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.EmailAlreadyExistsException;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.mapper.ClientMapper;
import com.codefactory.reservasmsauthservice.repository.ClientRepository;
import com.codefactory.reservasmsauthservice.service.impl.ClientServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * HU-01: Registro de usuario cliente (ClientServiceImpl)
 *
 * CP-01-001: Registro exitoso con datos válidos
 * CP-01-002: Registro fallido por correo duplicado
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MS-Auth - ClientServiceImpl - Creación de clientes")
class ClientServiceImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private ClientMapper clientMapper;
    @Mock private UserAuthService userAuthService;

    @InjectMocks
    private ClientServiceImpl clientService;

    private UUID clienteId;
    private CreateClientRequestDTO request;
    private Client clienteEntity;
    private ClientResponseDTO clienteResponse;

    @BeforeEach
    void setUp() {
        clienteId = UUID.randomUUID();

        request = CreateClientRequestDTO.builder()
                .email("carlos@email.com")
                .password("Segura#123")
                .nombre("Carlos Pérez")
                .telefono("3001234567")
                .build();

        clienteEntity = new Client();
        clienteEntity.setIdUsuario(clienteId);
        clienteEntity.setEmail("carlos@email.com");
        clienteEntity.setNombre("Carlos Pérez");
        clienteEntity.setTipoUsuario(User.Role.CLIENTE);

        clienteResponse = ClientResponseDTO.builder()
                .idUsuario(clienteId)
                .email("carlos@email.com")
                .nombre("Carlos Pérez")
                .tipoUsuario("CLIENTE")
                .build();
    }

    // =========================================================================
    // CP-01-001: Registro exitoso
    // =========================================================================

    @Test
    @DisplayName("CP-01-001: Crear cliente con datos válidos → retorna ClientResponseDTO")
    void createClient_DatosValidos_RetornaClientResponseDTO() {
        // Arrange
        doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
        when(clientMapper.toEntity(request)).thenReturn(clienteEntity);
        when(userAuthService.encodePassword("Segura#123")).thenReturn("$2a$10$hash");
        when(clientRepository.save(clienteEntity)).thenReturn(clienteEntity);
        when(clientMapper.toDto(clienteEntity)).thenReturn(clienteResponse);

        // Act
        ClientResponseDTO resultado = clientService.createClient(request);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("carlos@email.com");
        assertThat(resultado.getTipoUsuario()).isEqualTo("CLIENTE");
        verify(clientRepository, times(1)).save(clienteEntity);
    }

    @Test
    @DisplayName("CP-01-001b: Crear cliente → la contraseña queda hasheada antes de guardar")
    void createClient_ContrasenaHasheadaAntesDeGuardar() {
        // Arrange
        doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
        when(clientMapper.toEntity(request)).thenReturn(clienteEntity);
        when(userAuthService.encodePassword("Segura#123")).thenReturn("$2a$10$hash");
        when(clientRepository.save(clienteEntity)).thenReturn(clienteEntity);
        when(clientMapper.toDto(clienteEntity)).thenReturn(clienteResponse);

        // Act
        clientService.createClient(request);

        // Assert — encodePassword se invocó y el hash se asignó al entity
        verify(userAuthService, times(1)).encodePassword("Segura#123");
        assertThat(clienteEntity.getPasswordHash()).isEqualTo("$2a$10$hash");
    }

    // =========================================================================
    // CP-01-002: Correo duplicado → lanza EmailAlreadyExistsException
    // =========================================================================

    @Test
    @DisplayName("CP-01-002: Correo duplicado → lanza EmailAlreadyExistsException sin llegar a guardar")
    void createClient_CorreoDuplicado_LanzaExcepcionSinGuardar() {
        // Arrange
        doThrow(new EmailAlreadyExistsException("El correo electrónico ya está en uso"))
                .when(userAuthService).validateEmailAndPassword("carlos@email.com", "Segura#123");

        // Act & Assert
        assertThatThrownBy(() -> clientService.createClient(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("ya está en uso");

        verify(clientRepository, never()).save(any());
    }

    // =========================================================================
    // getUserEntityByEmail
    // =========================================================================

    @Test
    @DisplayName("getUserEntityByEmail: email existente → retorna entidad User")
    void getUserEntityByEmail_EmailExistente_RetornaUser() {
        // Arrange
        when(clientRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(clienteEntity));

        // Act
        User resultado = clientService.getUserEntityByEmail("carlos@email.com");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("carlos@email.com");
    }

    @Test
    @DisplayName("getUserEntityByEmail: email no existente → lanza ResourceNotFoundException")
    void getUserEntityByEmail_EmailNoExistente_LanzaResourceNotFoundException() {
        // Arrange
        when(clientRepository.findByEmail("noexiste@email.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> clientService.getUserEntityByEmail("noexiste@email.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }

    // =========================================================================
    // getExternalClientById
    // =========================================================================

    @Test
    @DisplayName("getExternalClientById: ID existente → retorna ExternalClientDTO")
    void getExternalClientById_IdExistente_RetornaExternalClientDTO() {
        // Arrange
        clienteEntity.setTelefono("3001234567");
        clienteEntity.setEmailVerificado(true);
        when(clientRepository.findById(clienteId)).thenReturn(Optional.of(clienteEntity));

        // Act
        var resultado = clientService.getExternalClientById(clienteId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("carlos@email.com");
        assertThat(resultado.getNombre()).isEqualTo("Carlos Pérez");
    }

    @Test
    @DisplayName("getExternalClientById: ID no existente → lanza ResourceNotFoundException")
    void getExternalClientById_IdNoExistente_LanzaResourceNotFoundException() {
        // Arrange
        UUID idDesconocido = UUID.randomUUID();
        when(clientRepository.findById(idDesconocido)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> clientService.getExternalClientById(idDesconocido))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }
}
