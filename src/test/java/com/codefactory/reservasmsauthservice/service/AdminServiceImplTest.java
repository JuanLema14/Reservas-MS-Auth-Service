package com.codefactory.reservasmsauthservice.service;

import com.codefactory.reservasmsauthservice.dto.request.CreateAdminRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.AdminResponseDTO;
import com.codefactory.reservasmsauthservice.entity.Admin;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.mapper.AdminMapper;
import com.codefactory.reservasmsauthservice.repository.AdminRepository;
import com.codefactory.reservasmsauthservice.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias - MS-AUTH-SERVICE
 * AdminServiceImpl — gestión de administradores
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MS-Auth - AdminServiceImpl - Gestión de administradores")
class AdminServiceImplTest {

    @Mock private AdminRepository adminRepository;
    @Mock private AdminMapper adminMapper;
    @Mock private UserAuthService userAuthService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private UUID adminId;
    private UUID creadoPorId;
    private Admin adminEntity;
    private AdminResponseDTO adminResponse;
    private CreateAdminRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        creadoPorId = UUID.randomUUID();

        createRequest = new CreateAdminRequestDTO();
        createRequest.setEmail("admin@empresa.com");
        createRequest.setPassword("Admin#123");
        createRequest.setNombreCompleto("Admin Principal");
        createRequest.setTelefono("3001234567");

        adminEntity = new Admin();
        adminEntity.setIdUsuario(adminId);
        adminEntity.setEmail("admin@empresa.com");
        adminEntity.setNombreCompleto("Admin Principal");
        adminEntity.setTipoUsuario(User.Role.ADMIN);
        adminEntity.setActivo(true);

        adminResponse = AdminResponseDTO.builder()
                .idUsuario(adminId)
                .email("admin@empresa.com")
                .nombreCompleto("Admin Principal")
                .tipoUsuario("ADMIN")
                .build();
    }

    // =========================================================================
    // createAdmin
    // =========================================================================

    @Test
    @DisplayName("createAdmin: datos válidos → retorna AdminResponseDTO")
    void createAdmin_DatosValidos_RetornaAdminResponseDTO() {
        // Arrange
        doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
        when(adminRepository.existsByEmail("admin@empresa.com")).thenReturn(false);
        when(adminMapper.toEntity(createRequest)).thenReturn(adminEntity);
        when(userAuthService.encodePassword("Admin#123")).thenReturn("$2a$10$hash");
        when(adminRepository.save(adminEntity)).thenReturn(adminEntity);
        when(adminMapper.toDto(adminEntity)).thenReturn(adminResponse);

        // Act
        AdminResponseDTO resultado = adminService.createAdmin(createRequest, creadoPorId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("admin@empresa.com");
        assertThat(resultado.getTipoUsuario()).isEqualTo("ADMIN");
        verify(adminRepository, times(1)).save(adminEntity);
    }

    @Test
    @DisplayName("createAdmin: email duplicado → lanza IllegalArgumentException sin guardar")
    void createAdmin_EmailDuplicado_LanzaIllegalArgumentException() {
        // Arrange
        doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
        when(adminRepository.existsByEmail("admin@empresa.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> adminService.createAdmin(createRequest, creadoPorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un administrador");

        verify(adminRepository, never()).save(any());
    }

    // =========================================================================
    // initializeFirstAdmin
    // =========================================================================

    @Test
    @DisplayName("initializeFirstAdmin: sin admins previos → crea primer admin")
    void initializeFirstAdmin_SinAdminsPrevios_CreaAdmin() {
        // Arrange
        when(adminRepository.findAll()).thenReturn(Collections.emptyList());
        doNothing().when(userAuthService).validateEmailAndPassword(any(), any());
        when(adminMapper.toEntity(createRequest)).thenReturn(adminEntity);
        when(userAuthService.encodePassword("Admin#123")).thenReturn("$2a$10$hash");
        when(adminRepository.save(adminEntity)).thenReturn(adminEntity);
        when(adminMapper.toDto(adminEntity)).thenReturn(adminResponse);

        // Act
        AdminResponseDTO resultado = adminService.initializeFirstAdmin(createRequest);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(adminEntity.getCreadoPor()).isNull();
        verify(adminRepository, times(1)).save(adminEntity);
    }

    @Test
    @DisplayName("initializeFirstAdmin: ya existen admins → lanza IllegalStateException")
    void initializeFirstAdmin_YaExistenAdmins_LanzaIllegalStateException() {
        // Arrange
        when(adminRepository.findAll()).thenReturn(List.of(adminEntity));

        // Act & Assert
        assertThatThrownBy(() -> adminService.initializeFirstAdmin(createRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existen administradores");

        verify(adminRepository, never()).save(any());
    }

    // =========================================================================
    // getAllAdmins
    // =========================================================================

    @Test
    @DisplayName("getAllAdmins: retorna lista de AdminResponseDTO")
    void getAllAdmins_RetornaLista() {
        // Arrange
        when(adminRepository.findAll()).thenReturn(List.of(adminEntity));
        when(adminMapper.toDto(adminEntity)).thenReturn(adminResponse);

        // Act
        List<AdminResponseDTO> resultado = adminService.getAllAdmins();

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEmail()).isEqualTo("admin@empresa.com");
    }

    // =========================================================================
    // getAdminById
    // =========================================================================

    @Test
    @DisplayName("getAdminById: ID existente → retorna AdminResponseDTO")
    void getAdminById_IdExistente_RetornaAdminResponseDTO() {
        // Arrange
        when(adminRepository.findById(adminId)).thenReturn(Optional.of(adminEntity));
        when(adminMapper.toDto(adminEntity)).thenReturn(adminResponse);

        // Act
        AdminResponseDTO resultado = adminService.getAdminById(adminId);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("admin@empresa.com");
    }

    @Test
    @DisplayName("getAdminById: ID no existente → lanza ResourceNotFoundException")
    void getAdminById_IdNoExistente_LanzaResourceNotFoundException() {
        // Arrange
        UUID idDesconocido = UUID.randomUUID();
        when(adminRepository.findById(idDesconocido)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.getAdminById(idDesconocido))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }

    // =========================================================================
    // deactivateAdmin / activateAdmin
    // =========================================================================

    @Test
    @DisplayName("deactivateAdmin: admin existente → lo desactiva")
    void deactivateAdmin_AdminExistente_LoDesactiva() {
        // Arrange
        when(adminRepository.findById(adminId)).thenReturn(Optional.of(adminEntity));
        when(adminRepository.save(adminEntity)).thenReturn(adminEntity);

        // Act
        adminService.deactivateAdmin(adminId);

        // Assert
        assertThat(adminEntity.getActivo()).isFalse();
        verify(adminRepository, times(1)).save(adminEntity);
    }

    @Test
    @DisplayName("activateAdmin: admin existente → lo activa")
    void activateAdmin_AdminExistente_LoActiva() {
        // Arrange
        adminEntity.setActivo(false);
        when(adminRepository.findById(adminId)).thenReturn(Optional.of(adminEntity));
        when(adminRepository.save(adminEntity)).thenReturn(adminEntity);

        // Act
        adminService.activateAdmin(adminId);

        // Assert
        assertThat(adminEntity.getActivo()).isTrue();
        verify(adminRepository, times(1)).save(adminEntity);
    }

    // =========================================================================
    // getUserEntityByEmail
    // =========================================================================

    @Test
    @DisplayName("getUserEntityByEmail: email existente → retorna User")
    void getUserEntityByEmail_EmailExistente_RetornaUser() {
        // Arrange
        when(adminRepository.findByEmail("admin@empresa.com")).thenReturn(Optional.of(adminEntity));

        // Act
        User resultado = adminService.getUserEntityByEmail("admin@empresa.com");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("admin@empresa.com");
    }

    @Test
    @DisplayName("getUserEntityByEmail: email no existente → lanza ResourceNotFoundException")
    void getUserEntityByEmail_EmailNoExistente_LanzaResourceNotFoundException() {
        // Arrange
        when(adminRepository.findByEmail("noexiste@email.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminService.getUserEntityByEmail("noexiste@email.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }
}
