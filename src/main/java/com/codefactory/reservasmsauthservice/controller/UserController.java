package com.codefactory.reservasmsauthservice.controller;

import com.codefactory.reservasmsauthservice.dto.external.ExternalClientDTO;
import com.codefactory.reservasmsauthservice.dto.external.ExternalProviderDTO;
import com.codefactory.reservasmsauthservice.service.ClientService;
import com.codefactory.reservasmsauthservice.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints para obtener información de usuarios (clientes y proveedores)")
public class UserController {

    private final ClientService clientService;
    private final ProviderService providerService;

    @GetMapping("/clients/{id}")
    @Operation(
        summary = "Obtener cliente por ID",
        description = "Retorna la información de un cliente por su ID. Usado por otros microservicios."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(schema = @Schema(implementation = ExternalClientDTO.class))),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<EntityModel<ExternalClientDTO>> getClientById(@PathVariable("id") UUID id) {
        ExternalClientDTO client = clientService.getExternalClientById(id);
        EntityModel<ExternalClientDTO> entityModel = EntityModel.of(client,
            linkTo(methodOn(UserController.class).getClientById(id)).withSelfRel());
        return ResponseEntity.ok(entityModel);
    }

    @GetMapping("/providers/{id}")
    @Operation(
        summary = "Obtener proveedor por ID",
        description = "Retorna la información de un proveedor por su ID. Usado por otros microservicios."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Proveedor encontrado", content = @Content(schema = @Schema(implementation = ExternalProviderDTO.class))),
        @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<EntityModel<ExternalProviderDTO>> getProviderById(@PathVariable("id") UUID id) {
        ExternalProviderDTO provider = providerService.getExternalProviderById(id);
        EntityModel<ExternalProviderDTO> entityModel = EntityModel.of(provider,
            linkTo(methodOn(UserController.class).getProviderById(id)).withSelfRel());
        return ResponseEntity.ok(entityModel);
    }
}