package com.codefactory.reservasmsauthservice.service.impl;

import com.codefactory.reservasmsauthservice.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Implementation of EmailService.
 * Sends verification emails using JavaMailSender and Thymeleaf templates.
 * Email sending is ASYNC to not block the main request thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${platform.name}")
    private String appName;

    @Value("${email.username}")
    private String emailUsername;

    @Override
    @Async
    public void sendVerificationEmail(String to, String name, String token) {
        log.info("[ASYNC] Iniciando envio de email de verificacion a: {}", to);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Verifica tu correo electrónico - " + appName);
            helper.setFrom(emailUsername);

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("verificationUrl", frontendUrl + "/verify?token=" + token);
            context.setVariable("appName", appName);

            // Process HTML template
            String htmlContent = templateEngine.process("email-verification", context);
            helper.setText(htmlContent, true);

            // Send email
            javaMailSender.send(message);
            log.info("[ASYNC] Email de verificacion enviado exitosamente a: {}", to);

        } catch (Exception e) {
            log.error("[ASYNC] Error al enviar email de verificacion a: {}", to, e);
            // Don't throw - just log the error
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String name, String token) {
        log.info("[ASYNC] Iniciando envio de email de reset de password a: {}", to);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Restablecer tu contraseña - " + appName);
            helper.setFrom(emailUsername);

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("resetUrl", frontendUrl + "/reset-password?token=" + token);
            context.setVariable("appName", appName);

            // Process HTML template
            String htmlContent = templateEngine.process("password-reset", context);
            helper.setText(htmlContent, true);

            // Send email
            javaMailSender.send(message);
            log.info("[ASYNC] Email de reset de password enviado exitosamente a: {}", to);

        } catch (Exception e) {
            log.error("[ASYNC] Error al enviar email de reset de password a: {}", to, e);
            // Don't throw - just log the error
        }
    }

    @Override
    @Async
    public void sendPasswordChangeConfirmationEmail(String to, String name) {
        log.info("[ASYNC] Iniciando envio de email de confirmacion de cambio de password a: {}", to);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Tu contraseña ha sido cambiada - " + appName);
            helper.setFrom(emailUsername);

            // Prepare Thymeleaf context
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("appName", appName);
            context.setVariable("supportEmail", "soporte@plataformareservas.com");

            // Process HTML template
            String htmlContent = templateEngine.process("password-change-confirmation", context);
            helper.setText(htmlContent, true);

            // Send email
            javaMailSender.send(message);
            log.info("[ASYNC] Email de confirmacion de cambio de password enviado exitosamente a: {}", to);

        } catch (Exception e) {
            log.error("[ASYNC] Error al enviar email de confirmacion de cambio de password a: {}", to, e);
            // Don't throw - just log the error
        }
    }
}
