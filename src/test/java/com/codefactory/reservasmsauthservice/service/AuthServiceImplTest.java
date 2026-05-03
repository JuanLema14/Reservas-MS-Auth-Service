package com.codefactory.reservasmsauthservice.service;

import com.codefactory.reservasmsauthservice.dto.request.CreateClientRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.CreateProviderRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.ClientResponseDTO;
import com.codefactory.reservasmsauthservice.dto.response.ProviderResponseDTO;
import com.codefactory.reservasmsauthservice.dto.response.RegistrationResponseDTO;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * HU-01: Registro de usuario cliente
 * HU-02: Registro de proveedor de servicios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MS-Auth - AuthServiceImpl - Registro de usuarios")
class AuthServiceImplTest {

    @Mock private ClientService clientService;
    @Mock private ProviderService providerService;
    @Mock private EmailVerificationTokenService emailVerificationTokenService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID clienteId;
    private UUID proveedorId;
    private User usuarioCliente;
    private User usuarioProveedor;
    private ClientResponseDTO clienteResponse;
    private ProviderResponseDTO proveedorResponse;

    @BeforeEach
    void setUp() {
        clienteId  = UUID.randomUUID();
        proveedorId = UUID.randomUUID();

        usuarioCliente = new User();
        usuarioCliente.setIdUsuario(clienteId);
        usuarioCliente.setEmail("carlos@email.com");
        usuarioCliente.setTipoUsuario(User.Role.CLIENTE);

        usuarioProveedor = new User();
        usuarioProveedor.setIdUsuario(proveedorId);
        usuarioProveedor.setEmail("salon@bellavida.com");
        usuarioProveedor.setTipoUsuario(User.Role.PROVEEDOR);

        clienteResponse = ClientResponseDTO.builder()
                .idUsuario(clienteId)
                .email("carlos@email.com")
                .nombre("Carlos Pérez")
                .tipoUsuario("CLIENTE")
                .build();

        proveedorResponse = ProviderResponseDTO.builder()
                .idUsuario(proveedorId)
                .email("salon@bellavida.com")
                .nombreComercial("Salón Bella Vida")
                .tipoUsuario("PROVEEDOR")
                .build();
    }

    // =========================================================================
    // HU-01: Registro de usuario cliente
    // CP-01-001: Registro exitoso con datos válidos → HTTP 201
    // =========================================================================

    @Test
    @DisplayName("CP-01-001: Registro exitoso de cliente → retorna RegistrationResponseDTO con tipo CLIENTE")
    void registerClient_DatosValidos_RetornaRegistrationResponseDTO() {
        // Arrange
        CreateClientRequestDTO request = CreateClientRequestDTO.builder()
                .email("carlos@email.com")
                .password("Segura#123")
                .nombre("Carlos Pérez")
                .telefono("3001234567")
                .build();

        when(clientService.createClient(request)).thenReturn(clienteResponse);
        when(clientService.getUserEntityByEmail("carlos@email.com")).thenReturn(usuarioCliente);
        when(emailVerificationTokenService.generateToken(usuarioCliente)).thenReturn("token-verificacion-123");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Act
        RegistrationResponseDTO resultado = authService.registerClient(request);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("carlos@email.com");
        assertThat(resultado.getUserType()).isEqualTo("CLIENTE");
        assertThat(resultado.getUserId()).isEqualTo(clienteId);
        assertThat(resultado.getMessage()).contains("verifica tu email");
        assertThat(resultado.getVerificationToken()).isEqualTo("token-verificacion-123");
    }

    @Test
    @DisplayName("CP-01-001b: Registro de cliente → envía email de verificación exactamente una vez")
    void registerClient_EnviaEmailVerificacion() {
        // Arrange
        CreateClientRequestDTO request = CreateClientRequestDTO.builder()
                .email("carlos@email.com")
                .password("Segura#123")
                .nombre("Carlos Pérez")
                .build();

        when(clientService.createClient(request)).thenReturn(clienteResponse);
        when(clientService.getUserEntityByEmail("carlos@email.com")).thenReturn(usuarioCliente);
        when(emailVerificationTokenService.generateToken(usuarioCliente)).thenReturn("token-123");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Act
        authService.registerClient(request);

        // Assert — el email de verificación se envía exactamente una vez (HU-02 del SQA)
        verify(emailService, times(1)).sendVerificationEmail(
                eq("carlos@email.com"), eq("Carlos Pérez"), eq("token-123"));
    }

    @Test
    @DisplayName("CP-01-001c: Registro de cliente → genera token de verificación exactamente una vez")
    void registerClient_GeneraTokenVerificacion() {
        // Arrange
        CreateClientRequestDTO request = CreateClientRequestDTO.builder()
                .email("carlos@email.com")
                .password("Segura#123")
                .nombre("Carlos Pérez")
                .build();

        when(clientService.createClient(request)).thenReturn(clienteResponse);
        when(clientService.getUserEntityByEmail("carlos@email.com")).thenReturn(usuarioCliente);
        when(emailVerificationTokenService.generateToken(usuarioCliente)).thenReturn("token-123");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Act
        authService.registerClient(request);

        // Assert
        verify(emailVerificationTokenService, times(1)).generateToken(usuarioCliente);
    }

    @Test
    @DisplayName("CP-01-001d: Si clientService lanza excepción → no se genera token ni se envía email")
    void registerClient_ClientServiceFalla_NoEnviaNadaMas() {
        // Arrange
        CreateClientRequestDTO request = CreateClientRequestDTO.builder()
                .email("duplicado@email.com")
                .password("Segura#123")
                .build();

        when(clientService.createClient(request)).thenThrow(new RuntimeException("Email duplicado"));

        // Act & Assert
        assertThatThrownBy(() -> authService.registerClient(request))
                .isInstanceOf(RuntimeException.class);

        verify(emailVerificationTokenService, never()).generateToken(any());
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    // =========================================================================
    // HU-02: Registro de proveedor de servicios
    // CP-02-001: Registro exitoso → estado PENDIENTE_VERIFICACION, correo enviado
    // =========================================================================

    @Test
    @DisplayName("CP-02-001: Registro exitoso de proveedor → retorna RegistrationResponseDTO con tipo PROVEEDOR")
    void registerProvider_DatosValidos_RetornaRegistrationResponseDTO() {
        // Arrange
        CreateProviderRequestDTO request = CreateProviderRequestDTO.builder()
                .email("salon@bellavida.com")
                .password("Segura#123")
                .nombreComercial("Salón Bella Vida")
                .telefonoContacto("3001234567")
                .build();

        when(providerService.createProvider(request)).thenReturn(proveedorResponse);
        when(providerService.getUserEntityByEmail("salon@bellavida.com")).thenReturn(usuarioProveedor);
        when(emailVerificationTokenService.generateToken(usuarioProveedor)).thenReturn("token-proveedor-456");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Act
        RegistrationResponseDTO resultado = authService.registerProvider(request);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("salon@bellavida.com");
        assertThat(resultado.getUserType()).isEqualTo("PROVEEDOR");
        assertThat(resultado.getUserId()).isEqualTo(proveedorId);
        assertThat(resultado.getVerificationToken()).isEqualTo("token-proveedor-456");
    }

    @Test
    @DisplayName("CP-02-001b: Registro de proveedor → envía email de verificación al correo del proveedor")
    void registerProvider_EnviaEmailVerificacion() {
        // Arrange
        CreateProviderRequestDTO request = CreateProviderRequestDTO.builder()
                .email("salon@bellavida.com")
                .password("Segura#123")
                .nombreComercial("Salón Bella Vida")
                .build();

        when(providerService.createProvider(request)).thenReturn(proveedorResponse);
        when(providerService.getUserEntityByEmail("salon@bellavida.com")).thenReturn(usuarioProveedor);
        when(emailVerificationTokenService.generateToken(usuarioProveedor)).thenReturn("token-456");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Act
        authService.registerProvider(request);

        // Assert
        verify(emailService, times(1)).sendVerificationEmail(
                eq("salon@bellavida.com"), eq("Salón Bella Vida"), eq("token-456"));
    }

    @Test
    @DisplayName("CP-02-001c: Registro de proveedor → token de verificación incluido en la respuesta")
    void registerProvider_TokenEnRespuesta() {
        // Arrange
        CreateProviderRequestDTO request = CreateProviderRequestDTO.builder()
                .email("salon@bellavida.com")
                .password("Segura#123")
                .nombreComercial("Salón Bella Vida")
                .build();

        when(providerService.createProvider(request)).thenReturn(proveedorResponse);
        when(providerService.getUserEntityByEmail("salon@bellavida.com")).thenReturn(usuarioProveedor);
        when(emailVerificationTokenService.generateToken(usuarioProveedor)).thenReturn("mi-token-unico");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Act
        RegistrationResponseDTO resultado = authService.registerProvider(request);

        // Assert
        assertThat(resultado.getVerificationToken()).isEqualTo("mi-token-unico");
    }

    @Test
    @DisplayName("CP-02-001d: Si providerService lanza excepción → no se genera token ni se envía email")
    void registerProvider_ProviderServiceFalla_NoEnviaNadaMas() {
        // Arrange
        CreateProviderRequestDTO request = CreateProviderRequestDTO.builder()
                .email("salon@bellavida.com")
                .password("Segura#123")
                .build();

        when(providerService.createProvider(request)).thenThrow(new RuntimeException("Categoría inválida"));

        // Act & Assert
        assertThatThrownBy(() -> authService.registerProvider(request))
                .isInstanceOf(RuntimeException.class);

        verify(emailVerificationTokenService, never()).generateToken(any());
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }
}
