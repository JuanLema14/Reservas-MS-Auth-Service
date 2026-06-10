package com.codefactory.reservasmsauthservice.service.impl;

import com.codefactory.reservasmsauthservice.dto.external.ExternalClientDTO;
import com.codefactory.reservasmsauthservice.dto.request.CreateClientRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.ClientResponseDTO;
import com.codefactory.reservasmsauthservice.entity.Client;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.BusinessException;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.mapper.ClientMapper;
import com.codefactory.reservasmsauthservice.repository.ClientRepository;
import com.codefactory.reservasmsauthservice.service.ClientService;
import com.codefactory.reservasmsauthservice.service.UserAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final UserAuthService userAuthService;

    @Override
    @Transactional
    public ClientResponseDTO createClient(CreateClientRequestDTO request) {
        // AGREGAR validación:
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new BusinessException("El nombre es obligatorio");
        }
        userAuthService.validateEmailAndPassword(request.getEmail(), request.getPassword());
    

        Client client = clientMapper.toEntity(request);
        // Codificar contraseña (centralizado en UserAuthService)
        client.setPasswordHash(userAuthService.encodePassword(request.getPassword()));
        
        Client savedClient = clientRepository.save(client);
        return clientMapper.toDto(savedClient);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityByEmail(String email) {
        return clientRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente con email '" + email + "' no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public ExternalClientDTO getExternalClientById(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente con ID '" + id + "' no encontrado"));
        return ExternalClientDTO.builder()
                .nombre(client.getNombre())
                .email(client.getEmail())
                .telefono(client.getTelefono())
                .emailVerificado(client.getEmailVerificado())
                .activo("ACTIVO".equals(client.getEstado()))
                .tipoUsuario(client.getTipoUsuario() != null ? client.getTipoUsuario().name() : null)
                .build();
    }
}
