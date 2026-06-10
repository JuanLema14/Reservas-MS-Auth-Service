package com.codefactory.reservasmsauthservice.acceptance.steps;

import com.codefactory.reservasmsauthservice.client.CatalogClient;
import com.codefactory.reservasmsauthservice.dto.request.CreateProviderRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.CategoryResponseDTO;
import com.codefactory.reservasmsauthservice.dto.response.ProviderResponseDTO;
import com.codefactory.reservasmsauthservice.entity.Provider;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.CategoryNotFoundException;
import com.codefactory.reservasmsauthservice.exception.EmailAlreadyExistsException;
import com.codefactory.reservasmsauthservice.mapper.ProviderMapper;
import com.codefactory.reservasmsauthservice.repository.ProviderRepository;
import com.codefactory.reservasmsauthservice.service.EmailService;
import com.codefactory.reservasmsauthservice.service.ProviderService;
import com.codefactory.reservasmsauthservice.service.UserAuthService;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class RegistroProveedorSteps {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private CatalogClient catalogClient;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProviderMapper providerMapper; // campo faltante — necesario para los stubs del happy path

    private CreateProviderRequestDTO request;
    private ProviderResponseDTO response;
    private Exception capturedException;
    private UUID categoriaId;

    private void executeCreateProvider() {
        try {
            response = providerService.createProvider(request);
            InicioSesionSteps.sharedResponse = response;
            InicioSesionSteps.sharedException = null;
        } catch (Exception e) {
            capturedException = e;
            InicioSesionSteps.sharedException = e;
            InicioSesionSteps.sharedResponse = null;
        }
    }

    @Dado("existe la categoría {string} activa")
    public void existeCategoriaActiva(String categoria) {
        if (categoriaId == null) categoriaId = UUID.randomUUID();
        CategoryResponseDTO categoryDTO = CategoryResponseDTO.builder()
            .idCategoria(categoriaId)
            .nombreCategoria(categoria)
            .activa(true)
            .build();
        when(catalogClient.getCategoryById(categoriaId)).thenReturn(categoryDTO);
        when(catalogClient.getCategoryById(any(UUID.class))).thenReturn(categoryDTO);
    }

    @Dado("que el proveedor no tiene cuenta en la plataforma")
    public void proveedorNoTieneCuenta() {
        if (categoriaId == null) categoriaId = UUID.randomUUID();
        if (request == null) request = new CreateProviderRequestDTO();
        reset(providerRepository, userAuthService, catalogClient, emailService, providerMapper);

        when(providerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        doNothing().when(userAuthService).validateEmailAndPassword(anyString(), anyString());
        when(userAuthService.encodePassword(anyString())).thenReturn("$2a$10$hashedPassword");
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // Re-stub catalogClient después del reset
        CategoryResponseDTO categoryDTO = CategoryResponseDTO.builder()
            .idCategoria(categoriaId)
            .nombreCategoria("Belleza y Spa")
            .activa(true)
            .build();
        when(catalogClient.getCategoryById(any(UUID.class))).thenReturn(categoryDTO);

        // Stub del mapper: toEntity retornaba null por defecto (es @MockBean), causando NPE
        Provider mockEntity = new Provider();
        mockEntity.setIdUsuario(UUID.randomUUID());
        mockEntity.setEmail("salon@bellavida.com");
        mockEntity.setNombreComercial("Salon Bella Vida");
        mockEntity.setIdCategoria(categoriaId);
        mockEntity.setTipoUsuario(User.Role.PROVEEDOR);
        when(providerMapper.toEntity(any(CreateProviderRequestDTO.class))).thenReturn(mockEntity);
        when(providerRepository.save(any(Provider.class))).thenReturn(mockEntity);

        ProviderResponseDTO mockResponse = ProviderResponseDTO.builder()
            .idUsuario(mockEntity.getIdUsuario())
            .email(mockEntity.getEmail())
            .nombreComercial(mockEntity.getNombreComercial())
            .tipoUsuario("PROVEEDOR")
            .build();
        when(providerMapper.toDto(any(Provider.class))).thenReturn(mockResponse);
    }

    @Dado("que el proveedor está completando el formulario de registro")
    public void proveedorCompletandoFormulario() {
        if (categoriaId == null) categoriaId = UUID.randomUUID();
        request = new CreateProviderRequestDTO();
        capturedException = null;
        response = null;
        InicioSesionSteps.sharedResponse = null;
        InicioSesionSteps.sharedException = null;
    }

    @Dado("que existe la categoría {string} pero está inactiva")
    public void existeCategoriaInactiva(String categoria) {
        if (categoriaId == null) categoriaId = UUID.randomUUID();
        CategoryResponseDTO categoryDTO = CategoryResponseDTO.builder()
            .idCategoria(categoriaId)
            .nombreCategoria(categoria)
            .activa(false)
            .build();
        when(catalogClient.getCategoryById(categoriaId)).thenReturn(categoryDTO);
        when(catalogClient.getCategoryById(any(UUID.class))).thenReturn(categoryDTO);
    }

    @Dado("que ya existe un cliente con el correo {string}")
    public void existeClienteConCorreo(String email) {
        reset(userAuthService);
        doThrow(new EmailAlreadyExistsException("El correo electrónico ya está en uso"))
            .when(userAuthService).validateEmailAndPassword(eq(email), anyString());
    }

    @Cuando("ingresa el nombre del negocio {string}")
    public void ingresaNombreNegocio(String nombre) {
        if (request == null) request = new CreateProviderRequestDTO();
        request.setNombreComercial(nombre);
    }

    @Cuando("ingresa su correo {string}")
    public void ingresaCorreo(String email) {
        if (request == null) request = new CreateProviderRequestDTO();
        request.setEmail(email);
    }

    @Cuando("selecciona la categoría de servicio {string}")
    public void seleccionaCategoriaServicio(String categoria) {
        if (request == null) request = new CreateProviderRequestDTO();
        request.setIdCategoria(categoriaId != null ? categoriaId : UUID.randomUUID());
    }

    @Cuando("ingresa su número de contacto {string}")
    public void ingresaNumeroContacto(String telefono) {
        if (request == null) request = new CreateProviderRequestDTO();
        request.setTelefonoContacto(telefono);
    }

    @Cuando("ingresa su dirección {string}")
    public void ingresaDireccion(String direccion) {
        if (request == null) request = new CreateProviderRequestDTO();
        request.setDireccion(direccion);
        // Activar pendingRegistration para que haceClicEn("Registrarse") ejecute el registro del proveedor
        InicioSesionSteps.pendingRegistration = this::executeCreateProvider;
    }

    @Cuando("no selecciona ninguna categoría de servicio")
    public void noSeleccionaCategoria() {
        if (request == null) request = new CreateProviderRequestDTO();
        request.setIdCategoria(null);
        doThrow(new CategoryNotFoundException("Debes seleccionar una categoría de servicio"))
            .when(catalogClient).getCategoryById(null);
        // Activar pendingRegistration para que haceClicEn("Registrarse") ejecute el registro
        InicioSesionSteps.pendingRegistration = this::executeCreateProvider;
    }

    @Cuando("selecciona una categoría inexistente {string}")
    public void seleccionaCategoriaInexistente(String categoria) {
        if (request == null) request = new CreateProviderRequestDTO();
        request.setIdCategoria(UUID.randomUUID());
        doThrow(new CategoryNotFoundException(
            "La categoría seleccionada no existe o no está activa"
        )).when(catalogClient).getCategoryById(any(UUID.class));
        // Activar pendingRegistration para que haceClicEn("Registrarse") ejecute el registro
        InicioSesionSteps.pendingRegistration = this::executeCreateProvider;
    }

    @Cuando("el proveedor intenta registrarse con esa categoría")
    public void proveedorIntentaRegistrarseCategoriaInactiva() {
        request = CreateProviderRequestDTO.builder()
            .email("test@proveedor.com")
            .password("Segura#123")
            .nombreComercial("Test Provider")
            .idCategoria(categoriaId)
            .direccion("Test Address")
            .telefonoContacto("3001234567")
            .build();
        executeCreateProvider();
    }

    @Cuando("el proveedor intenta registrarse con ese mismo correo")
    public void proveedorIntentaRegistrarseMismoCorreo() {
        request = CreateProviderRequestDTO.builder()
            .email("salon@bellavida.com")
            .password("Segura#123")
            .nombreComercial("Test Provider")
            .idCategoria(categoriaId)
            .direccion("Test Address")
            .telefonoContacto("3001234567")
            .build();
        executeCreateProvider();
    }

    @Entonces("el sistema crea la cuenta del proveedor")
    public void sistemaCreaCuentaProveedor() {
        if (response == null && InicioSesionSteps.sharedResponse instanceof ProviderResponseDTO) {
            response = (ProviderResponseDTO) InicioSesionSteps.sharedResponse;
        }
        assertThat(response).isNotNull();
        verify(providerRepository, times(1)).save(any(Provider.class));
    }

    @Entonces("envía un correo de verificación a {string}")
    public void enviaCorreoVerificacion(String email) {
        verify(emailService, times(1)).sendVerificationEmail(eq(email), anyString(), anyString());
    }

    @Entonces("no crea la cuenta del proveedor")
    public void noCreaCuentaProveedor() {
        assertThat(response).isNull();
        verify(providerRepository, never()).save(any(Provider.class));
    }
}