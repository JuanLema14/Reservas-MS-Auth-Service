package com.codefactory.reservasmsauthservice.controller;

import com.codefactory.reservasmsauthservice.dto.request.CreateAdminRequestDTO;
import com.codefactory.reservasmsauthservice.dto.request.UpdateAdminRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.AdminResponseDTO;
import com.codefactory.reservasmsauthservice.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/auth/admins")
@RequiredArgsConstructor
@Tag(name = "Administradores", description = "Endpoints para gestión de administradores del sistema")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/initialize")
    @Operation(
        summary = "Inicializar primer administrador",
        description = "Crea el primer administrador del sistema. Solo funciona cuando no existen administradores. No requiere autenticación.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del primer administrador",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateAdminRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Primer administrador creado exitosamente", content = @Content(schema = @Schema(implementation = AdminResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de administrador inválidos o email ya existe"),
        @ApiResponse(responseCode = "409", description = "Ya existen administradores en el sistema")
    })
    public ResponseEntity<EntityModel<AdminResponseDTO>> initializeFirstAdmin(
            @Valid @RequestBody CreateAdminRequestDTO request) {
        AdminResponseDTO response = adminService.initializeFirstAdmin(request);
        EntityModel<AdminResponseDTO> entityModel = EntityModel.of(response,
            linkTo(methodOn(AdminController.class).getAdminById(response.getIdUsuario())).withSelfRel(),
            linkTo(methodOn(AdminController.class).getAllAdmins()).withRel("all-admins"));
        return new ResponseEntity<>(entityModel, HttpStatus.CREATED);
    }

    @PostMapping
    @Operation(
        summary = "Crear administrador",
        description = "Crea un nuevo administrador. Requiere rol de ADMIN. El campo creadoPor debe ser el UUID del admin que crea este registro.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del administrador a crear",
            required = true,
            content = @Content(schema = @Schema(implementation = CreateAdminRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Administrador creado exitosamente", content = @Content(schema = @Schema(implementation = AdminResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de administrador inválidos o email ya existe"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tiene rol de ADMIN")
    })
    public ResponseEntity<EntityModel<AdminResponseDTO>> createAdmin(
            @Valid @RequestBody CreateAdminRequestDTO request,
            @Parameter(description = "UUID del administrador que crea este registro", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam UUID creadoPor) {
        AdminResponseDTO response = adminService.createAdmin(request, creadoPor);
        EntityModel<AdminResponseDTO> entityModel = EntityModel.of(response,
            linkTo(methodOn(AdminController.class).getAdminById(response.getIdUsuario())).withSelfRel(),
            linkTo(methodOn(AdminController.class).getAllAdmins()).withRel("all-admins"));
        return new ResponseEntity<>(entityModel, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(
        summary = "Obtener todos los administradores",
        description = "Retorna una lista con todos los administradores del sistema. Requiere rol de ADMIN."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de administradores retornada exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tiene rol de ADMIN")
    })
    public ResponseEntity<CollectionModel<EntityModel<AdminResponseDTO>>> getAllAdmins() {
        List<AdminResponseDTO> admins = adminService.getAllAdmins();
        
        List<EntityModel<AdminResponseDTO>> adminModels = admins.stream()
            .map(admin -> EntityModel.of(admin,
                linkTo(methodOn(AdminController.class).getAdminById(admin.getIdUsuario())).withSelfRel(),
                linkTo(methodOn(AdminController.class).deactivateAdmin(admin.getIdUsuario())).withRel("deactivate"),
                linkTo(methodOn(AdminController.class).updateAdmin(admin.getIdUsuario(), null)).withRel("update")))
            .collect(Collectors.toList());
        
        CollectionModel<EntityModel<AdminResponseDTO>> collectionModel = CollectionModel.of(adminModels,
            linkTo(methodOn(AdminController.class).getAllAdmins()).withSelfRel());
        
        return ResponseEntity.ok(collectionModel);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obtener administrador por ID",
        description = "Retorna los detalles de un administrador específico por su UUID. Requiere rol de ADMIN."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Administrador encontrado exitosamente", content = @Content(schema = @Schema(implementation = AdminResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Formato de UUID inválido"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tiene rol de ADMIN"),
        @ApiResponse(responseCode = "404", description = "Administrador no encontrado")
    })
    public ResponseEntity<EntityModel<AdminResponseDTO>> getAdminById(
            @Parameter(description = "UUID del administrador", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        AdminResponseDTO admin = adminService.getAdminById(id);
        EntityModel<AdminResponseDTO> entityModel = EntityModel.of(admin,
            linkTo(methodOn(AdminController.class).getAdminById(id)).withSelfRel(),
            linkTo(methodOn(AdminController.class).getAllAdmins()).withRel("all-admins"),
            linkTo(methodOn(AdminController.class).updateAdmin(id, null)).withRel("update"),
            linkTo(methodOn(AdminController.class).deactivateAdmin(id)).withRel("deactivate"));
        return ResponseEntity.ok(entityModel);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar administrador",
        description = "Actualiza los datos de un administrador existente. Requiere rol de ADMIN.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos actualizados del administrador",
            required = true,
            content = @Content(schema = @Schema(implementation = UpdateAdminRequestDTO.class))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Administrador actualizado exitosamente", content = @Content(schema = @Schema(implementation = AdminResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos de administrador inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tiene rol de ADMIN"),
        @ApiResponse(responseCode = "404", description = "Administrador no encontrado")
    })
    public ResponseEntity<EntityModel<AdminResponseDTO>> updateAdmin(
            @Parameter(description = "UUID del administrador", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAdminRequestDTO request) {
        AdminResponseDTO admin = adminService.updateAdmin(id, request);
        EntityModel<AdminResponseDTO> entityModel = EntityModel.of(admin,
            linkTo(methodOn(AdminController.class).getAdminById(id)).withSelfRel(),
            linkTo(methodOn(AdminController.class).getAllAdmins()).withRel("all-admins"));
        return ResponseEntity.ok(entityModel);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Desactivar administrador",
        description = "Desactiva un administrador existente (soft delete). Requiere rol de ADMIN."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Administrador desactivado exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tiene rol de ADMIN"),
        @ApiResponse(responseCode = "404", description = "Administrador no encontrado")
    })
    public ResponseEntity<Void> deactivateAdmin(
            @Parameter(description = "UUID del administrador", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        adminService.deactivateAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @Operation(
        summary = "Activar administrador",
        description = "Reactiva un administrador desactivado. Requiere rol de ADMIN."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Administrador activado exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "No tiene rol de ADMIN"),
        @ApiResponse(responseCode = "404", description = "Administrador no encontrado")
    })
    public ResponseEntity<Void> activateAdmin(
            @Parameter(description = "UUID del administrador", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        adminService.activateAdmin(id);
        return ResponseEntity.noContent().build();
    }
}