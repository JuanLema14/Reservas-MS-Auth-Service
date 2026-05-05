package com.codefactory.reservasmsauthservice.service.impl;

import com.codefactory.reservasmsauthservice.dto.request.ChangePasswordDTO;
import com.codefactory.reservasmsauthservice.dto.request.PasswordResetConfirmDTO;
import com.codefactory.reservasmsauthservice.dto.request.PasswordResetRequestDTO;
import com.codefactory.reservasmsauthservice.dto.response.MessageResponseDTO;
import com.codefactory.reservasmsauthservice.entity.PasswordResetToken;
import com.codefactory.reservasmsauthservice.entity.RefreshToken;
import com.codefactory.reservasmsauthservice.entity.User;
import com.codefactory.reservasmsauthservice.exception.IncorrectPasswordException;
import com.codefactory.reservasmsauthservice.exception.InvalidPasswordException;
import com.codefactory.reservasmsauthservice.exception.InvalidResetTokenException;
import com.codefactory.reservasmsauthservice.exception.ResourceNotFoundException;
import com.codefactory.reservasmsauthservice.exception.SamePasswordException;
import com.codefactory.reservasmsauthservice.repository.PasswordResetTokenRepository;
import com.codefactory.reservasmsauthservice.repository.RefreshTokenRepository;
import com.codefactory.reservasmsauthservice.repository.UserRepository;
import com.codefactory.reservasmsauthservice.service.EmailService;
import com.codefactory.reservasmsauthservice.service.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implementation of PasswordService.
 * Handles password reset (forgot password) and password change (authenticated).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordServiceImpl implements PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d");
    private static final String DEFAULT_USER_NAME = "Usuario";

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDTO request, String ipAddress) {
        String email = request.getEmail();

        // Find user by email - if not found, still return success for security
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // Invalidate any existing tokens for this user
            List<PasswordResetToken> existingTokens = passwordResetTokenRepository
                    .findByUser_IdUsuarioAndUsadoFalseOrderByCreatedAtDesc(user.getIdUsuario());
            existingTokens.forEach(token -> {
                token.setUsado(true);
                token.setFechaUso(LocalDateTime.now());
            });
            passwordResetTokenRepository.saveAll(existingTokens);

            // Generate new token
            String tokenValue = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(tokenValue)
                    .expiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS))
                    .ipAddress(ipAddress)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            // Get user name for email
            String userName = getUserName(user);

            // Send email with reset link
            emailService.sendPasswordResetEmail(email, userName, tokenValue);

            log.info("Password reset requested for user: {}", email);
        } else {
            // Log for security monitoring but don't reveal anything to the user
            log.info("Password reset requested for non-existent email: {}", email);
        }
    }

    @Override
    @Transactional
    public MessageResponseDTO confirmPasswordReset(PasswordResetConfirmDTO request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        // Validate password format
        validatePasswordFormat(newPassword);

        // Find valid token
        PasswordResetToken resetToken = passwordResetTokenRepository.findValidByToken(token)
                .orElseThrow(() -> new InvalidResetTokenException("Token inválido, expirado o ya usado"));

        // Mark token as used
        resetToken.setUsado(true);
        resetToken.setFechaUso(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        // Update user password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens for this user
        revokeAllRefreshTokens(user.getIdUsuario());

        // Get user name for email
        String userName = getUserName(user);

        // Send confirmation email
        emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), userName);

        log.info("Password reset confirmed for user: {}", user.getEmail());

        return new MessageResponseDTO("Contraseña restablecida exitosamente");
    }

    @Override
    @Transactional
    public MessageResponseDTO changePassword(ChangePasswordDTO request) {
        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();

        // Get authenticated user email from SecurityContext
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("No estás autenticado");
        }

        String email = authentication.getName();

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validate password format
        validatePasswordFormat(newPassword);

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IncorrectPasswordException("La contraseña actual es incorrecta");
        }

        // Check if new password is same as current
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new SamePasswordException("La nueva contraseña no puede ser igual a la actual");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all refresh tokens for this user
        revokeAllRefreshTokens(user.getIdUsuario());

        // Get user name for email
        String userName = getUserName(user);

        // Send confirmation email
        emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), userName);

        log.info("Password changed for user: {}", user.getEmail());

        return new MessageResponseDTO("Contraseña cambiada exitosamente");
    }

    /**
     * Validates password format according to fixed requirements.
     * Password must have at least 8 characters, one uppercase, one lowercase, and one number.
     */
    private void validatePasswordFormat(String password) {
        List<String> errors = new java.util.ArrayList<>();

        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add(String.format("al menos %d caracteres", MIN_PASSWORD_LENGTH));
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("una letra mayúscula");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("una letra minúscula");
        }

        if (!NUMBER_PATTERN.matcher(password).find()) {
            errors.add("un número");
        }

        if (!errors.isEmpty()) {
            throw new InvalidPasswordException("La contraseña debe contener: " + String.join(", ", errors));
        }
    }

    /**
     * Revokes all refresh tokens for a user.
     */
    private void revokeAllRefreshTokens(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUser_IdUsuarioAndRevocadoFalse(userId);
        tokens.forEach(token -> {
            token.setRevocado(true);
            token.setFechaRevocacion(LocalDateTime.now());
        });
        refreshTokenRepository.saveAll(tokens);
        log.info("Revoked {} refresh tokens for user: {}", tokens.size(), userId);
    }

    /**
     * Gets the user's name based on their type (client or provider).
     */
    private String getUserName(User user) {
        if (user.getTipoUsuario() == User.Role.CLIENTE) {
            return userRepository.findClientNameByUserId(user.getIdUsuario()).orElse(DEFAULT_USER_NAME);
        } else if (user.getTipoUsuario() == User.Role.PROVEEDOR) {
            return userRepository.findProviderNameByUserId(user.getIdUsuario()).orElse(DEFAULT_USER_NAME);
        }
        return DEFAULT_USER_NAME;
    }
}