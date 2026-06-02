package com.codefactory.reservasmsauthservice.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.codefactory.reservasmsauthservice.client.CatalogClient;
import com.codefactory.reservasmsauthservice.dto.request.CreateProviderRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.CategoryResponseDTO;
import com.codefactory.reservasmsauthservice.dto.response.ProviderResponseDTO;
import com.codefactory.reservasmsauthservice.entity.Provider;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.CategoryNotFoundException;
import com.codefactory.reservasmsauthservice.exception.EmailAlreadyExistsException;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.mapper.ProviderMapper;
import com.codefactory.reservasmsauthservice.repository.ProviderRepository;
import com.codefactory.reservasmsauthservice.service.impl.ProviderServiceImpl;
import feign.FeignException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * HU-02: Registro de proveedor de servicios (ProviderServiceImpl)
 *
 * CP-02-001: Registro exitoso de proveedor
 * CP-02-002: Registro fallido sin categoría válida
 * CP-02-005: Correo duplicado entre roles diferentes
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MS-Auth - ProviderServiceImpl - Creación de proveedores")
class ProviderServiceImplTest {

  @Mock
  private ProviderRepository providerRepository;

  @Mock
  private ProviderMapper providerMapper;

  @Mock
  private UserAuthService userAuthService;

  @Mock
  private CatalogClient catalogClient;

  @InjectMocks
  private ProviderServiceImpl providerService;

  private UUID proveedorId;
  private UUID categoriaId;
  private CreateProviderRequestDTO request;
  private Provider proveedorEntity;
  private ProviderResponseDTO proveedorResponse;
  private CategoryResponseDTO categoriaActiva;

  @BeforeEach
  void setUp() {
    proveedorId = UUID.randomUUID();
    categoriaId = UUID.randomUUID();

    request =
      CreateProviderRequestDTO
        .builder()
        .email("salon@bellavida.com")
        .password("Segura#123")
        .nombreComercial("Salón Bella Vida")
        .idCategoria(categoriaId)
        .direccion("Calle 10 #20-30")
        .telefonoContacto("3001234567")
        .build();

    proveedorEntity = new Provider();
    proveedorEntity.setIdUsuario(proveedorId);
    proveedorEntity.setEmail("salon@bellavida.com");
    proveedorEntity.setNombreComercial("Salón Bella Vida");
    proveedorEntity.setIdCategoria(categoriaId);
    proveedorEntity.setTipoUsuario(User.Role.PROVEEDOR);

    proveedorResponse =
      ProviderResponseDTO
        .builder()
        .idUsuario(proveedorId)
        .email("salon@bellavida.com")
        .nombreComercial("Salón Bella Vida")
        .tipoUsuario("PROVEEDOR")
        .build();

    categoriaActiva = new CategoryResponseDTO();
    categoriaActiva.setActiva(true);
  }

  // =========================================================================
  // CP-02-001: Registro exitoso
  // =========================================================================

  @Test
  @DisplayName(
    "CP-02-001: Crear proveedor con datos válidos → retorna ProviderResponseDTO"
  )
  void createProvider_DatosValidos_RetornaProviderResponseDTO() {
    // Arrange
    doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
    when(catalogClient.getCategoryById(categoriaId))
      .thenReturn(categoriaActiva);
    when(providerMapper.toEntity(request)).thenReturn(proveedorEntity);
    when(userAuthService.encodePassword("Segura#123"))
      .thenReturn("$2a$10$hash");
    when(providerRepository.save(proveedorEntity)).thenReturn(proveedorEntity);
    when(providerMapper.toDto(proveedorEntity)).thenReturn(proveedorResponse);

    // Act
    ProviderResponseDTO resultado = providerService.createProvider(request);

    // Assert
    assertThat(resultado).isNotNull();
    assertThat(resultado.getEmail()).isEqualTo("salon@bellavida.com");
    assertThat(resultado.getTipoUsuario()).isEqualTo("PROVEEDOR");
    verify(providerRepository, times(1)).save(proveedorEntity);
  }

  @Test
  @DisplayName(
    "CP-02-001b: Crear proveedor → contraseña hasheada antes de guardar"
  )
  void createProvider_ContrasenaHasheadaAntesDeGuardar() {
    // Arrange
    doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
    when(catalogClient.getCategoryById(categoriaId))
      .thenReturn(categoriaActiva);
    when(providerMapper.toEntity(request)).thenReturn(proveedorEntity);
    when(userAuthService.encodePassword("Segura#123"))
      .thenReturn("$2a$10$hash");
    when(providerRepository.save(proveedorEntity)).thenReturn(proveedorEntity);
    when(providerMapper.toDto(proveedorEntity)).thenReturn(proveedorResponse);

    // Act
    providerService.createProvider(request);

    // Assert
    verify(userAuthService, times(1)).encodePassword("Segura#123");
    assertThat(proveedorEntity.getPasswordHash()).isEqualTo("$2a$10$hash");
  }

  // =========================================================================
  // CP-02-002: Categoría inválida → lanza CategoryNotFoundException
  // =========================================================================

  @Test
  @DisplayName(
    "CP-02-002: Categoría no encontrada (FeignException) → lanza CategoryNotFoundException"
  )
  void createProvider_CategoriaNoEncontrada_LanzaCategoryNotFoundException() {
    // Arrange
    doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
    when(catalogClient.getCategoryById(categoriaId))
      .thenThrow(mock(FeignException.class));

    // Act & Assert
    assertThatThrownBy(() -> providerService.createProvider(request))
      .isInstanceOf(CategoryNotFoundException.class)
      .hasMessageContaining("no existe en el servicio de catálogo");

    verify(providerRepository, never()).save(any());
  }

  @Test
  @DisplayName(
    "CP-02-002b: Categoría inactiva → lanza CategoryNotFoundException"
  )
  void createProvider_CategoriaInactiva_LanzaCategoryNotFoundException() {
    // Arrange
    CategoryResponseDTO categoriaInactiva = new CategoryResponseDTO();
    categoriaInactiva.setActiva(false);

    doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
    when(catalogClient.getCategoryById(categoriaId))
      .thenReturn(categoriaInactiva);

    // Act & Assert
    assertThatThrownBy(() -> providerService.createProvider(request))
      .isInstanceOf(CategoryNotFoundException.class)
      .hasMessageContaining("no existe o no está activa");

    verify(providerRepository, never()).save(any());
  }

  @Test
  @DisplayName("CP-02-002c: Categoría nula → lanza CategoryNotFoundException")
  void createProvider_CategoriaNula_LanzaCategoryNotFoundException() {
    // Arrange
    doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
    when(catalogClient.getCategoryById(categoriaId)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> providerService.createProvider(request))
      .isInstanceOf(CategoryNotFoundException.class);

    verify(providerRepository, never()).save(any());
  }

  // =========================================================================
  // CP-02-005: Correo duplicado → lanza EmailAlreadyExistsException
  // =========================================================================

  @Test
  @DisplayName(
    "CP-02-005: Correo ya en uso → lanza EmailAlreadyExistsException sin llegar a guardar"
  )
  void createProvider_CorreoDuplicado_LanzaExcepcionSinGuardar() {
    // Arrange
    doThrow(
      new EmailAlreadyExistsException("El correo electrónico ya está en uso")
    )
      .when(userAuthService)
      .validateEmailAndPassword("salon@bellavida.com", "Segura#123");

    // Act & Assert
    assertThatThrownBy(() -> providerService.createProvider(request))
      .isInstanceOf(EmailAlreadyExistsException.class);

    verify(catalogClient, never()).getCategoryById(any());
    verify(providerRepository, never()).save(any());
  }

  // =========================================================================
  // getUserEntityByEmail
  // =========================================================================

  @Test
  @DisplayName(
    "getUserEntityByEmail: email existente → retorna entidad Provider"
  )
  void getUserEntityByEmail_EmailExistente_RetornaProvider() {
    // Arrange
    when(providerRepository.findByEmail("salon@bellavida.com"))
      .thenReturn(Optional.of(proveedorEntity));

    // Act
    User resultado = providerService.getUserEntityByEmail(
      "salon@bellavida.com"
    );

    // Assert
    assertThat(resultado).isNotNull();
    assertThat(resultado.getEmail()).isEqualTo("salon@bellavida.com");
  }

  @Test
  @DisplayName(
    "getUserEntityByEmail: email no existente → lanza ResourceNotFoundException"
  )
  void getUserEntityByEmail_EmailNoExistente_LanzaResourceNotFoundException() {
    // Arrange
    when(providerRepository.findByEmail("noexiste@email.com"))
      .thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() ->
        providerService.getUserEntityByEmail("noexiste@email.com")
      )
      .isInstanceOf(ResourceNotFoundException.class)
      .hasMessageContaining("no encontrado");
  }

  // =========================================================================
  // getExternalProviderById
  // =========================================================================

  @Test
  @DisplayName(
    "getExternalProviderById: ID existente → retorna ExternalProviderDTO"
  )
  void getExternalProviderById_IdExistente_RetornaExternalProviderDTO() {
    // Arrange
    proveedorEntity.setDireccion("Calle 10 #20-30");
    proveedorEntity.setTelefonoContacto("3001234567");
    when(providerRepository.findById(proveedorId))
      .thenReturn(Optional.of(proveedorEntity));

    // Act
    var resultado = providerService.getExternalProviderById(proveedorId);

    // Assert
    assertThat(resultado).isNotNull();
    assertThat(resultado.getEmail()).isEqualTo("salon@bellavida.com");
    assertThat(resultado.getNombreComercial()).isEqualTo("Salón Bella Vida");
  }

  @Test
  @DisplayName(
    "getExternalProviderById: ID no existente → lanza ResourceNotFoundException"
  )
  void getExternalProviderById_IdNoExistente_LanzaResourceNotFoundException() {
    // Arrange
    UUID idDesconocido = UUID.randomUUID();
    when(providerRepository.findById(idDesconocido))
      .thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() ->
        providerService.getExternalProviderById(idDesconocido)
      )
      .isInstanceOf(ResourceNotFoundException.class)
      .hasMessageContaining("no encontrado");
  }

  @Test
  @DisplayName(
    "getExternalProviderById: proveedor con estado diferente a ACTIVO → activo=false"
  )
  void getExternalProviderById_EstadoDiferenteActivo_RetornaActivoFalse() {
    // Arrange
    proveedorEntity.setEstado("INACTIVO");
    when(providerRepository.findById(proveedorId))
      .thenReturn(Optional.of(proveedorEntity));

    // Act
    var resultado = providerService.getExternalProviderById(proveedorId);

    // Assert
    assertThat(resultado).isNotNull();
    assertThat(resultado.getActivo()).isFalse();
  }

  @Test
  @DisplayName(
    "getExternalProviderById: proveedor con tipoUsuario nulo → tipoUsuario null en DTO"
  )
  void getExternalProviderById_TipoUsuarioNulo_RetornaTipoUsuarioNull() {
    // Arrange
    proveedorEntity.setTipoUsuario(null);
    when(providerRepository.findById(proveedorId))
      .thenReturn(Optional.of(proveedorEntity));

    // Act
    var resultado = providerService.getExternalProviderById(proveedorId);

    // Assert
    assertThat(resultado).isNotNull();
    assertThat(resultado.getTipoUsuario()).isNull();
  }

  @Test
  @DisplayName(
    "CP-02-002d: Categoría nula con mensaje específico → lanza CategoryNotFoundException"
  )
  void createProvider_CategoriaNula_ConMensajeEspecifico() {
    // Arrange
    doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
    when(catalogClient.getCategoryById(categoriaId)).thenReturn(null);

    // Act & Assert
    assertThatThrownBy(() -> providerService.createProvider(request))
      .isInstanceOf(CategoryNotFoundException.class)
      .hasMessageContaining("no existe o no está activa");

    verify(providerRepository, never()).save(any());
  }

  @Test
  @DisplayName("getExternalProviderById: verifica todos los campos del DTO")
  void getExternalProviderById_VerificaTodosLosCampos() {
    // Arrange
    proveedorEntity.setDireccion("Calle 10 #20-30");
    proveedorEntity.setTelefonoContacto("3001234567");
    proveedorEntity.setEstado("ACTIVO");
    when(providerRepository.findById(proveedorId))
      .thenReturn(Optional.of(proveedorEntity));

    // Act
    var resultado = providerService.getExternalProviderById(proveedorId);

    // Assert
    assertThat(resultado.getNombreComercial()).isEqualTo("Salón Bella Vida");
    assertThat(resultado.getEmail()).isEqualTo("salon@bellavida.com");
    assertThat(resultado.getTelefonoContacto()).isEqualTo("3001234567");
    assertThat(resultado.getIdCategoria()).isEqualTo(categoriaId);
    assertThat(resultado.getDireccion()).isEqualTo("Calle 10 #20-30");
    assertThat(resultado.getActivo()).isTrue();
    assertThat(resultado.getTipoUsuario()).isEqualTo("PROVEEDOR");
  }
}
