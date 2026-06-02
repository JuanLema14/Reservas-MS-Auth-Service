package com.codefactory.reservasmsauthservice.service.impl;

import com.codefactory.reservasmsauthservice.client.CatalogClient;
import com.codefactory.reservasmsauthservice.dto.external.ExternalProviderDTO;
import com.codefactory.reservasmsauthservice.dto.request.CreateProviderRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.CategoryResponseDTO;
import com.codefactory.reservasmsauthservice.dto.response.ProviderResponseDTO;
import com.codefactory.reservasmsauthservice.entity.Provider;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.CategoryNotFoundException;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.mapper.ProviderMapper;
import com.codefactory.reservasmsauthservice.repository.ProviderRepository;
import com.codefactory.reservasmsauthservice.service.ProviderService;
import com.codefactory.reservasmsauthservice.service.UserAuthService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository providerRepository;
    private final ProviderMapper providerMapper;
    private final UserAuthService userAuthService;
    private final CatalogClient catalogClient;

    @Override
    @Transactional
    public ProviderResponseDTO createProvider(CreateProviderRequestDTO request) {
        // Validar email y contraseña (centralizado en UserAuthService)
        userAuthService.validateEmailAndPassword(request.getEmail(), request.getPassword());

        // Validar que la categoría existe en el Catalog Service
        try {
            CategoryResponseDTO category = catalogClient.getCategoryById(request.getIdCategoria());
            if (category == null || !Boolean.TRUE.equals(category.getActiva())) {
                throw new CategoryNotFoundException("La categoría con ID '" + request.getIdCategoria() + "' no existe o no está activa");
            }
        } catch (FeignException e) {
            throw new CategoryNotFoundException("La categoría con ID '" + request.getIdCategoria() + "' no existe en el servicio de catálogo");
        }

        Provider provider = providerMapper.toEntity(request);
        // Codificar contraseña (centralizado en UserAuthService)
        provider.setPasswordHash(userAuthService.encodePassword(request.getPassword()));

        Provider savedProvider = providerRepository.save(provider);
        return providerMapper.toDto(savedProvider);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityByEmail(String email) {
        return providerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor con email '" + email + "' no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalProviderDTO getExternalProviderById(UUID id) {
        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor con ID '" + id + "' no encontrado"));
        return ExternalProviderDTO.builder()
                .nombreComercial(provider.getNombreComercial())
                .email(provider.getEmail())
                .telefonoContacto(provider.getTelefonoContacto())
                .idCategoria(provider.getIdCategoria())
                .direccion(provider.getDireccion())
                .activo("ACTIVO".equals(provider.getEstado()))
                .tipoUsuario(provider.getTipoUsuario() != null ? provider.getTipoUsuario().name() : null)
                .build();
    }
}
