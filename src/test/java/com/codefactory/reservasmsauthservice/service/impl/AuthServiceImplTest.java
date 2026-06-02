package com.codefactory.reservasmsauthservice.service.impl;

import com.codefactory.reservasmsauthservice.dto.request.CreateClientRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.CreateProviderRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.ClientResponseDTO;
import com.codefactory.reservasmsauthservice.dto.response.ProviderResponseDTO;
import com.codefactory.reservasmsauthservice.dto.response.RegistrationResponseDTO;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.service.ClientService;
import com.codefactory.reservasmsauthservice.service.EmailService;
import com.codefactory.reservasmsauthservice.service.EmailVerificationTokenService;
import com.codefactory.reservasmsauthservice.service.ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para AuthServiceImpl.
 * Utiliza mocks de repositories y EmailService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ClientService clientService;

    @Mock
    private ProviderService providerService;

    @Mock
    private EmailVerificationTokenService emailVerificationTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password123";
    private static final String TEST_NAME = "Test User";
    private static final String TEST_COMMERCIAL_NAME = "Test Business";
    private static final String VERIFICATION_TOKEN = "verification-token-123";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setIdUsuario(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setPasswordHash(TEST_PASSWORD);
        testUser.setTipoUsuario(User.Role.CLIENTE);
        testUser.setEmailVerificado(false);
        testUser.setEstado("ACTIVO");
        testUser.setFechaRegistro(LocalDateTime.now());
    }

    private CreateClientRequestDTO createClientRequest() {
        CreateClientRequestDTO request = new CreateClientRequestDTO();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);
        request.setNombre(TEST_NAME);
        request.setTelefono("1234567890");
        return request;
    }

    private CreateProviderRequestDTO createProviderRequest() {
        CreateProviderRequestDTO request = new CreateProviderRequestDTO();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);
        request.setNombreComercial(TEST_COMMERCIAL_NAME);
        request.setIdCategoria(UUID.randomUUID());
        request.setDireccion("Test Address");
        request.setTelefonoContacto("1234567890");
        return request;
    }

    @Nested
    @DisplayName("registerClient")
    class RegisterClientTests {

        @Test
        @DisplayName("Debe registrar cliente exitosamente")
        void registerClient_Success() {
            // Given
            CreateClientRequestDTO request = createClientRequest();

            ClientResponseDTO clientResponse = new ClientResponseDTO();
            clientResponse.setIdUsuario(TEST_USER_ID);
            clientResponse.setEmail(TEST_EMAIL);
            clientResponse.setNombre(TEST_NAME);
            clientResponse.setTipoUsuario("CLIENTE");

            when(clientService.createClient(any(CreateClientRequestDTO.class))).thenReturn(clientResponse);
            when(clientService.getUserEntityByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(emailVerificationTokenService.generateToken(any(User.class))).thenReturn(VERIFICATION_TOKEN);

            // When
            RegistrationResponseDTO response = authService.registerClient(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getVerificationToken()).isEqualTo(VERIFICATION_TOKEN);
            assertThat(response.getUserType()).isEqualTo("CLIENTE");
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getMessage()).contains("verifica tu email");

            verify(clientService).createClient(request);
            verify(emailVerificationTokenService).generateToken(testUser);
            verify(emailService).sendVerificationEmail(eq(TEST_EMAIL), eq(TEST_NAME), eq(VERIFICATION_TOKEN));
        }

        @Test
        @DisplayName("Debe generar token de verificación para cliente")
        void registerClient_GeneratesVerificationToken() {
            // Given
            CreateClientRequestDTO request = createClientRequest();

            ClientResponseDTO clientResponse = new ClientResponseDTO();
            clientResponse.setIdUsuario(TEST_USER_ID);
            clientResponse.setEmail(TEST_EMAIL);
            clientResponse.setNombre(TEST_NAME);

            when(clientService.createClient(any(CreateClientRequestDTO.class))).thenReturn(clientResponse);
            when(clientService.getUserEntityByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(emailVerificationTokenService.generateToken(any(User.class))).thenReturn(VERIFICATION_TOKEN);

            // When
            authService.registerClient(request);

            // Then
            verify(emailVerificationTokenService).generateToken(testUser);
        }

        @Test
        @DisplayName("Debe enviar email de verificación")
        void registerClient_SendsVerificationEmail() {
            // Given
            CreateClientRequestDTO request = createClientRequest();

            ClientResponseDTO clientResponse = new ClientResponseDTO();
            clientResponse.setIdUsuario(TEST_USER_ID);
            clientResponse.setEmail(TEST_EMAIL);
            clientResponse.setNombre(TEST_NAME);

            when(clientService.createClient(any(CreateClientRequestDTO.class))).thenReturn(clientResponse);
            when(clientService.getUserEntityByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(emailVerificationTokenService.generateToken(any(User.class))).thenReturn(VERIFICATION_TOKEN);

            // When
            authService.registerClient(request);

            // Then
            verify(emailService).sendVerificationEmail(TEST_EMAIL, TEST_NAME, VERIFICATION_TOKEN);
        }
    }

    @Nested
    @DisplayName("registerProvider")
    class RegisterProviderTests {

        @Test
        @DisplayName("Debe registrar proveedor exitosamente")
        void registerProvider_Success() {
            // Given
            CreateProviderRequestDTO request = createProviderRequest();

            ProviderResponseDTO providerResponse = new ProviderResponseDTO();
            providerResponse.setIdUsuario(TEST_USER_ID);
            providerResponse.setEmail(TEST_EMAIL);
            providerResponse.setNombreComercial(TEST_COMMERCIAL_NAME);
            providerResponse.setTipoUsuario("PROVEEDOR");

            when(providerService.createProvider(any(CreateProviderRequestDTO.class))).thenReturn(providerResponse);
            when(providerService.getUserEntityByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(emailVerificationTokenService.generateToken(any(User.class))).thenReturn(VERIFICATION_TOKEN);

            // When
            RegistrationResponseDTO response = authService.registerProvider(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getVerificationToken()).isEqualTo(VERIFICATION_TOKEN);
            assertThat(response.getUserType()).isEqualTo("PROVEEDOR");
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(response.getMessage()).contains("verifica tu email");

            verify(providerService).createProvider(request);
            verify(emailVerificationTokenService).generateToken(testUser);
            verify(emailService).sendVerificationEmail(eq(TEST_EMAIL), eq(TEST_COMMERCIAL_NAME), eq(VERIFICATION_TOKEN));
        }

        @Test
        @DisplayName("Debe generar token de verificación para proveedor")
        void registerProvider_GeneratesVerificationToken() {
            // Given
            CreateProviderRequestDTO request = createProviderRequest();

            ProviderResponseDTO providerResponse = new ProviderResponseDTO();
            providerResponse.setIdUsuario(TEST_USER_ID);
            providerResponse.setEmail(TEST_EMAIL);
            providerResponse.setNombreComercial(TEST_COMMERCIAL_NAME);

            when(providerService.createProvider(any(CreateProviderRequestDTO.class))).thenReturn(providerResponse);
            when(providerService.getUserEntityByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(emailVerificationTokenService.generateToken(any(User.class))).thenReturn(VERIFICATION_TOKEN);

            // When
            authService.registerProvider(request);

            // Then
            verify(emailVerificationTokenService).generateToken(testUser);
        }

        @Test
        @DisplayName("Debe enviar email de verificación con nombre comercial")
        void registerProvider_SendsVerificationEmailWithCommercialName() {
            // Given
            CreateProviderRequestDTO request = createProviderRequest();

            ProviderResponseDTO providerResponse = new ProviderResponseDTO();
            providerResponse.setIdUsuario(TEST_USER_ID);
            providerResponse.setEmail(TEST_EMAIL);
            providerResponse.setNombreComercial(TEST_COMMERCIAL_NAME);

            when(providerService.createProvider(any(CreateProviderRequestDTO.class))).thenReturn(providerResponse);
            when(providerService.getUserEntityByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(emailVerificationTokenService.generateToken(any(User.class))).thenReturn(VERIFICATION_TOKEN);

            // When
            authService.registerProvider(request);

            // Then
            verify(emailService).sendVerificationEmail(TEST_EMAIL, TEST_COMMERCIAL_NAME, VERIFICATION_TOKEN);
        }
    }

    @Nested
    @DisplayName("Verificación de Email")
    class EmailVerificationTests {

        @Test
        @DisplayName("Debe incluir mensaje informativo en respuesta de registro")
        void registerClient_IncludesInformativeMessage() {
            // Given
            CreateClientRequestDTO request = createClientRequest();

            ClientResponseDTO clientResponse = new ClientResponseDTO();
            clientResponse.setIdUsuario(TEST_USER_ID);
            clientResponse.setEmail(TEST_EMAIL);
            clientResponse.setNombre(TEST_NAME);

            when(clientService.createClient(any(CreateClientRequestDTO.class))).thenReturn(clientResponse);
            when(clientService.getUserEntityByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(emailVerificationTokenService.generateToken(any(User.class))).thenReturn(VERIFICATION_TOKEN);

            // When
            RegistrationResponseDTO response = authService.registerClient(request);

            // Then
            assertThat(response.getMessage()).isNotNull();
            assertThat(response.getMessage()).contains("verifica tu email");
            assertThat(response.getMessage()).contains("24 horas");
        }

        @Test
        @DisplayName("Debe usar email del cliente en respuesta")
        void registerClient_ReturnsCorrectEmail() {
            // Given
            CreateClientRequestDTO request = createClientRequest();

            ClientResponseDTO clientResponse = new ClientResponseDTO();
            clientResponse.setIdUsuario(TEST_USER_ID);
            clientResponse.setEmail(TEST_EMAIL);
            clientResponse.setNombre(TEST_NAME);

            when(clientService.createClient(any(CreateClientRequestDTO.class))).thenReturn(clientResponse);
            when(clientService.getUserEntityByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(emailVerificationTokenService.generateToken(any(User.class))).thenReturn(VERIFICATION_TOKEN);

            // When
            RegistrationResponseDTO response = authService.registerClient(request);

            // Then
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Debe retornar userId correcto del cliente creado")
        void registerClient_ReturnsCorrectUserId() {
            // Given
            CreateClientRequestDTO request = createClientRequest();

            ClientResponseDTO clientResponse = new ClientResponseDTO();
            clientResponse.setIdUsuario(TEST_USER_ID);
            clientResponse.setEmail(TEST_EMAIL);
            clientResponse.setNombre(TEST_NAME);

            when(clientService.createClient(any(CreateClientRequestDTO.class))).thenReturn(clientResponse);
            when(clientService.getUserEntityByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(emailVerificationTokenService.generateToken(any(User.class))).thenReturn(VERIFICATION_TOKEN);

            // When
            RegistrationResponseDTO response = authService.registerClient(request);

            // Then
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
        }
    }
}