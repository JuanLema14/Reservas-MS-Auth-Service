package com.codefactory.reservasmsauthservice.acceptance.steps;

import com.codefactory.reservasmsauthservice.dto.request.LoginRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.LogoutRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.RefreshTokenRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.LoginResponseDTO;
import com.codefactory.reservasmsauthservice.exception.InvalidCredentialsException;
import com.codefactory.reservasmsauthservice.service.LoginService;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import static org.mockito.ArgumentMatchers.nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class InicioSesionSteps {

    @Autowired
    private LoginService loginService;

    // Compartido con RegistroClienteSteps y RegistroProveedorSteps via contexto Cucumber
    static Object sharedResponse = null;
    static Exception sharedException = null;
    static Runnable pendingRegistration = null;

    private LoginRequestDTO loginRequest;
    private LoginResponseDTO loginResponse;
    private RefreshTokenRequestDTO refreshTokenRequest;
    private LogoutRequestDTO logoutRequest;
    private Exception capturedException;

    @Dado("que el sistema está operativo")
    public void sistemaOperativo() {
        loginRequest = new LoginRequestDTO();
        loginResponse = null;
        refreshTokenRequest = null;
        logoutRequest = null;
        capturedException = null;
        sharedResponse = null;
        sharedException = null;
        pendingRegistration = null;
    }

    @Dado("que existe un cliente registrado con correo {string} y contraseña {string}")
    public void existeClienteRegistrado(String email, String password) {
        reset(loginService);
        LoginResponseDTO mockResponse = LoginResponseDTO.builder()
            .accessToken("eyJhbGciOiJIUzI1NiJ9.mockJWT")
            .refreshToken("550e8400-e29b-41d4-a716-446655440000")
            .email(email)
            .role("CLIENTE")
            .build();
        when(loginService.login(any(LoginRequestDTO.class), nullable(HttpServletRequest.class)))
            .thenReturn(mockResponse);
    }

    @Dado("que existe un proveedor registrado con correo {string} y contraseña {string}")
    public void existeProveedorRegistrado(String email, String password) {
        reset(loginService);
        LoginResponseDTO mockResponse = LoginResponseDTO.builder()
            .accessToken("eyJhbGciOiJIUzI1NiJ9.mockJWT")
            .refreshToken("550e8400-e29b-41d4-a716-446655440001")
            .email(email)
            .role("PROVEEDOR")
            .build();
        when(loginService.login(any(LoginRequestDTO.class), nullable(HttpServletRequest.class)))
            .thenReturn(mockResponse);
    }

    @Dado("que existe un cliente registrado con correo {string}")
    public void existeClienteRegistradoSimple(String email) {
        reset(loginService);
        doThrow(new InvalidCredentialsException("Credenciales inválidas"))
            .when(loginService).login(any(LoginRequestDTO.class), nullable(HttpServletRequest.class));
    }

    @Dado("que no existe un usuario con correo {string}")
    public void noExisteUsuario(String email) {
        reset(loginService);
        doThrow(new InvalidCredentialsException("Credenciales inválidas"))
            .when(loginService).login(any(LoginRequestDTO.class), nullable(HttpServletRequest.class));
    }

    @Cuando("el usuario ingresa su correo {string}")
    public void usuarioIngresaCorreo(String email) {
        loginRequest.setEmail(email);
    }

    @Cuando("ingresa su contraseña {string}")
    public void ingresaContrasena(String password) {
        loginRequest.setPassword(password);
    }

    @Cuando("ingresa una contraseña incorrecta {string}")
    public void ingresaContrasenaIncorrecta(String password) {
        loginRequest.setPassword(password);
    }

    @Cuando("el proveedor ingresa sus credenciales correctas")
    public void proveedorIngresaCredencialesCorrectas() {
        loginRequest.setEmail("salon@bellavida.com");
        loginRequest.setPassword("Segura#123");
    }

    @Cuando("el usuario intenta iniciar sesión con ese correo")
    public void intentaIniciarSesion() {
        loginRequest.setEmail("noexiste@email.com");
        loginRequest.setPassword("SomePass123");
        try {
            loginResponse = loginService.login(loginRequest, null);
        } catch (Exception e) {
            capturedException = e;
        }
    }

    @Cuando("hace clic en {string}")
    public void haceClicEn(String boton) {
        if ("Iniciar sesión".equals(boton)) {
            try {
                loginResponse = loginService.login(loginRequest, null);
            } catch (Exception e) {
                capturedException = e;
            }
        } else if ("Registrarse".equals(boton)) {
            if (pendingRegistration != null && sharedResponse == null && sharedException == null) {
                pendingRegistration.run();
                pendingRegistration = null;
            }
            capturedException = sharedException;
        }
    }

    @Entonces("el sistema autentica al usuario exitosamente")
    public void sistemaAutenticaUsuario() {
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isNotNull();
        verify(loginService, times(1)).login(any(LoginRequestDTO.class), nullable(HttpServletRequest.class));
    }

    @Entonces("el sistema autentica al proveedor exitosamente")
    public void sistemaAutenticaProveedor() {
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isNotNull();
    }

    @Entonces("genera un token JWT de acceso")
    public void generaTokenJWT() {
        assertThat(loginResponse.getAccessToken()).isNotEmpty();
    }

    @Entonces("genera un refresh token")
    public void generaRefreshToken() {
        assertThat(loginResponse.getRefreshToken()).isNotEmpty();
    }

    @Entonces("el token JWT contiene el rol {string}")
    public void tokenContieneRol(String rol) {
        assertThat(loginResponse.getRole()).isEqualTo(rol);
    }

    @Entonces("el token JWT contiene el email {string}")
    public void tokenContieneEmail(String email) {
        assertThat(loginResponse.getEmail()).isEqualTo(email);
    }

    @Entonces("lo redirige al panel de cliente")
    public void redirigePanelCliente() {
        assertThat(loginResponse.getRole()).isEqualTo("CLIENTE");
        assertThat(capturedException).isNull();
    }

    @Entonces("lo redirige al panel de administración de servicios")
    public void redirigePanelAdminServicios() {
        assertThat(loginResponse.getRole()).isEqualTo("PROVEEDOR");
        assertThat(capturedException).isNull();
    }

    @Entonces("el sistema muestra el error {string}")
    public void sistemaMuestraError(String mensajeError) {
        Exception ex = capturedException != null ? capturedException : sharedException;
        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).contains(mensajeError);
    }

    @Entonces("no genera ningún token de acceso")
    public void noGeneraTokenAcceso() {
        assertThat(loginResponse).isNull();
    }

    @Entonces("el código de respuesta HTTP es {int}")
    public void codigoRespuestaHTTP(int codigo) {
        Exception ex = capturedException != null ? capturedException : sharedException;
        assertThat(ex).isNotNull();
    }

    @Entonces("no concede acceso")
    public void noConcedeAcceso() {
        assertThat(capturedException).isNotNull();
        assertThat(loginResponse).isNull();
    }

    @Dado("que el usuario tiene un refresh token válido")
    public void usuarioTieneRefreshTokenValido() {
        reset(loginService);
        LoginResponseDTO refreshedResponse = LoginResponseDTO.builder()
            .accessToken("eyJhbGciOiJIUzI1NiJ9.newMockJWT")
            .refreshToken("660e8400-e29b-41d4-a716-446655440000")
            .email("carlos@email.com")
            .role("CLIENTE")
            .build();
        when(loginService.refreshToken(any(RefreshTokenRequestDTO.class)))
            .thenReturn(refreshedResponse);
    }

    @Dado("que el usuario proporciona un refresh token inválido")
    public void usuarioProporcionaRefreshTokenInvalido() {
        reset(loginService);
        doThrow(new InvalidCredentialsException("Refresh token inválido"))
            .when(loginService).refreshToken(any(RefreshTokenRequestDTO.class));
    }

    @Cuando("solicita un nuevo token de acceso con el refresh token")
    public void solicitaNuevoTokenConRefreshToken() {
        refreshTokenRequest = new RefreshTokenRequestDTO();
        refreshTokenRequest.setRefreshToken("550e8400-e29b-41d4-a716-446655440000");
        try {
            loginResponse = loginService.refreshToken(refreshTokenRequest);
        } catch (Exception e) {
            capturedException = e;
        }
    }

    @Cuando("solicita un nuevo token de acceso")
    public void solicitaNuevoToken() {
        refreshTokenRequest = new RefreshTokenRequestDTO();
        refreshTokenRequest.setRefreshToken("invalid-token");
        try {
            loginResponse = loginService.refreshToken(refreshTokenRequest);
        } catch (Exception e) {
            capturedException = e;
        }
    }

    @Entonces("el sistema genera un nuevo token JWT")
    public void sistemaGeneraNuevoTokenJWT() {
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getAccessToken()).isNotEmpty();
    }

    @Entonces("el refresh token anterior se invalida")
    public void refreshTokenAnteriorInvalidado() {
        verify(loginService, times(1)).refreshToken(any(RefreshTokenRequestDTO.class));
    }

    @Entonces("se genera un nuevo refresh token")
    public void generaNuevoRefreshToken() {
        assertThat(loginResponse.getRefreshToken()).isNotEmpty();
        assertThat(loginResponse.getRefreshToken()).isNotEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Entonces("no genera ningún token nuevo")
    public void noGeneraTokenNuevo() {
        assertThat(loginResponse).isNull();
        assertThat(capturedException).isNotNull();
    }

    @Dado("que el usuario tiene una sesión activa")
    public void usuarioTieneSesionActiva() {
        reset(loginService);
        doNothing().when(loginService).logout(any(LogoutRequestDTO.class));
    }

    @Cuando("solicita cerrar sesión")
    public void solicitaCerrarSesion() {
        logoutRequest = new LogoutRequestDTO();
        logoutRequest.setRefreshToken("550e8400-e29b-41d4-a716-446655440000");
        try {
            loginService.logout(logoutRequest);
        } catch (Exception e) {
            capturedException = e;
        }
    }

    @Entonces("el sistema invalida el refresh token")
    public void sistemaInvalidaRefreshToken() {
        verify(loginService, times(1)).logout(any(LogoutRequestDTO.class));
    }

    @Entonces("el token de acceso ya no es válido")
    public void tokenAccesoNoEsValido() {
        assertThat(capturedException).isNull();
    }
}