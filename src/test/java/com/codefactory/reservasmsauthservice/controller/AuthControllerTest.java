package com.codefactory.reservasmsauthservice.controller;

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
import com.codefactory.reservasmsauthservice.exception.GlobalExceptionHandler;
import com.codefactory.reservasmsauthservice.security.JwtAuthenticationEntryPoint;
import com.codefactory.reservasmsauthservice.security.JwtAuthenticationFilter;
import com.codefactory.reservasmsauthservice.security.JwtService;
import com.codefactory.reservasmsauthservice.service.AuthService;
import com.codefactory.reservasmsauthservice.service.LoginService;
import com.codefactory.reservasmsauthservice.service.PasswordService;
import com.codefactory.reservasmsauthservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitarios para AuthController.
 * Utiliza @WebMvcTest con @MockBean para simular servicios.
 */
@WebMvcTest(controllers = AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private LoginService loginService;

    @MockBean
    private PasswordService passwordService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password123";
    private static final String TEST_NAME = "Test User";
    private static final String TEST_TOKEN = "verification-token-123";
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("POST /api/auth/register/client")
    class RegisterClientTests {

        @Test
        @DisplayName("Debe registrar cliente exitosamente y retornar 201")
        void registerClient_Success() throws Exception {
            // Given - use setters since CreateClientRequestDTO has @Data only
            CreateClientRequestDTO request = new CreateClientRequestDTO();
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);
            request.setNombre(TEST_NAME);
            request.setTelefono("1234567890");

            RegistrationResponseDTO response = RegistrationResponseDTO.builder()
                    .verificationToken(TEST_TOKEN)
                    .email(TEST_EMAIL)
                    .userType("CLIENTE")
                    .userId(TEST_USER_ID)
                    .message("Por favor, verifica tu email para activar tu cuenta.")
                    .build();

            when(authService.registerClient(any(CreateClientRequestDTO.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/register/client")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.verificationToken").value(TEST_TOKEN))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.userType").value("CLIENTE"))
                    .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()));
        }

        @Test
        @DisplayName("Debe retornar 400 para email inválido")
        void registerClient_InvalidEmail() throws Exception {
            // Given
            CreateClientRequestDTO request = new CreateClientRequestDTO();
            request.setEmail("invalid-email");
            request.setPassword(TEST_PASSWORD);
            request.setNombre(TEST_NAME);
            request.setTelefono("1234567890");

            // When & Then
            mockMvc.perform(post("/api/auth/register/client")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 para contraseña débil")
        void registerClient_WeakPassword() throws Exception {
            // Given
            CreateClientRequestDTO request = new CreateClientRequestDTO();
            request.setEmail(TEST_EMAIL);
            request.setPassword("weak");
            request.setNombre(TEST_NAME);
            request.setTelefono("1234567890");

            // When & Then
            mockMvc.perform(post("/api/auth/register/client")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 para teléfono inválido")
        void registerClient_InvalidPhone() throws Exception {
            // Given
            CreateClientRequestDTO request = new CreateClientRequestDTO();
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);
            request.setNombre(TEST_NAME);
            request.setTelefono("abc123");

            // When & Then
            mockMvc.perform(post("/api/auth/register/client")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register/provider")
    class RegisterProviderTests {

        @Test
        @DisplayName("Debe registrar proveedor exitosamente y retornar 201")
        void registerProvider_Success() throws Exception {
            // Given - use setters since CreateProviderRequestDTO has @Data only
            CreateProviderRequestDTO request = new CreateProviderRequestDTO();
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);
            request.setNombreComercial("Test Business");
            request.setIdCategoria(UUID.randomUUID());
            request.setDireccion("Test Address");
            request.setTelefonoContacto("1234567890");

            RegistrationResponseDTO response = RegistrationResponseDTO.builder()
                    .verificationToken(TEST_TOKEN)
                    .email(TEST_EMAIL)
                    .userType("PROVEEDOR")
                    .userId(TEST_USER_ID)
                    .message("Por favor, verifica tu email para activar tu cuenta.")
                    .build();

            when(authService.registerProvider(any(CreateProviderRequestDTO.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/register/provider")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.verificationToken").value(TEST_TOKEN))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.userType").value("PROVEEDOR"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Debe hacer login exitosamente y retornar tokens")
        void login_Success() throws Exception {
            // Given - LoginRequestDTO has @Builder
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .build();

            LoginResponseDTO response = LoginResponseDTO.builder()
                    .accessToken("access-token-123")
                    .refreshToken("refresh-token-123")
                    .role("CLIENTE")
                    .userId(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .build();

            when(loginService.login(any(LoginRequestDTO.class), any())).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"))
                    .andExpect(jsonPath("$.role").value("CLIENTE"))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL));
        }

        @Test
        @DisplayName("Debe retornar 400 para email vacío")
        void login_EmptyEmail() throws Exception {
            // Given
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .email("")
                    .password(TEST_PASSWORD)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 para contraseña vacía")
        void login_EmptyPassword() throws Exception {
            // Given
            LoginRequestDTO request = LoginRequestDTO.builder()
                    .email(TEST_EMAIL)
                    .password("")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Debe refrescar token exitosamente")
        void refreshToken_Success() throws Exception {
            // Given - RefreshTokenRequestDTO has @Builder
            RefreshTokenRequestDTO request = RefreshTokenRequestDTO.builder()
                    .refreshToken("refresh-token-123")
                    .build();

            LoginResponseDTO response = LoginResponseDTO.builder()
                    .accessToken("new-access-token")
                    .refreshToken("new-refresh-token")
                    .role("CLIENTE")
                    .userId(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .build();

            when(loginService.refreshToken(any(RefreshTokenRequestDTO.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
        }

        @Test
        @DisplayName("Debe retornar 400 para refresh token vacío")
        void refreshToken_EmptyToken() throws Exception {
            // Given
            RefreshTokenRequestDTO request = RefreshTokenRequestDTO.builder()
                    .refreshToken("")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Debe hacer logout exitosamente")
        void logout_Success() throws Exception {
            // Given - LogoutRequestDTO has @Builder
            LogoutRequestDTO request = LogoutRequestDTO.builder()
                    .refreshToken("refresh-token-123")
                    .build();

            doNothing().when(loginService).logout(any(LogoutRequestDTO.class));

            // When & Then
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Debe retornar 400 para refresh token vacío")
        void logout_EmptyToken() throws Exception {
            // Given
            LogoutRequestDTO request = LogoutRequestDTO.builder()
                    .refreshToken("")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/password-reset/request")
    class PasswordResetRequestTests {

        @Test
        @DisplayName("Debe solicitar reset de contraseña exitosamente")
        void requestPasswordReset_Success() throws Exception {
            // Given - use setter since PasswordResetRequestDTO has @Data only
            PasswordResetRequestDTO request = new PasswordResetRequestDTO();
            request.setEmail(TEST_EMAIL);

            doNothing().when(passwordService).requestPasswordReset(any(PasswordResetRequestDTO.class), anyString());

            // When & Then
            mockMvc.perform(post("/api/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Debe retornar 400 para email inválido")
        void requestPasswordReset_InvalidEmail() throws Exception {
            // Given
            PasswordResetRequestDTO request = new PasswordResetRequestDTO();
            request.setEmail("invalid-email");

            // When & Then
            mockMvc.perform(post("/api/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/password-reset/confirm")
    class PasswordResetConfirmTests {

        @Test
        @DisplayName("Debe confirmar reset de contraseña exitosamente")
        void confirmPasswordReset_Success() throws Exception {
            // Given - use setters since PasswordResetConfirmDTO has @Data only
            PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
            request.setToken("reset-token-123");
            request.setNewPassword("NewPassword123");

            // MessageResponseDTO has constructor that accepts message
            MessageResponseDTO response = new MessageResponseDTO("Contraseña restablecida exitosamente");

            when(passwordService.confirmPasswordReset(any(PasswordResetConfirmDTO.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Contraseña restablecida exitosamente"));
        }

        @Test
        @DisplayName("Debe retornar 400 para token vacío")
        void confirmPasswordReset_EmptyToken() throws Exception {
            // Given
            PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
            request.setToken("");
            request.setNewPassword("NewPassword123");

            // When & Then
            mockMvc.perform(post("/api/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 para contraseña muy corta")
        void confirmPasswordReset_ShortPassword() throws Exception {
            // Given
            PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
            request.setToken("reset-token-123");
            request.setNewPassword("short");

            // When & Then
            mockMvc.perform(post("/api/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}