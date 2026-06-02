package com.codefactory.reservasmsauthservice.config;

import com.codefactory.reservasmsauthservice.security.JwtAuthenticationEntryPoint;
import com.codefactory.reservasmsauthservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;
        private final JwtAuthenticationEntryPoint authenticationEntryPoint;
        private final CorsConfigurationSource corsConfigurationSource;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .authorizeHttpRequests(auth -> auth
                                                // Endpoints públicos
                                                .requestMatchers("/api/", "/api/version").permitAll()
                                                // Endpoints de autenticación públicos (registro, login, refresh,
                                                // password-reset)
                                                .requestMatchers("/api/auth/register/**").permitAll()
                                                .requestMatchers("/api/auth/login").permitAll()
                                                .requestMatchers("/api/auth/refresh").permitAll()
                                                .requestMatchers("/api/auth/password-reset/**").permitAll()
                                                .requestMatchers("/api/auth/verify-email/**").permitAll()

                                                // Swagger/OpenAPI - Documentación pública
                                                .requestMatchers("/swagger-ui.html").permitAll()
                                                .requestMatchers("/swagger-ui/**").permitAll()
                                                .requestMatchers("/v3/api-docs/**").permitAll()
                                                .requestMatchers("/swagger-resources/**").permitAll()
                                                .requestMatchers("/webjars/**").permitAll()
                                                .requestMatchers("/configuration/**").permitAll()
                                                // Actuator endpoints para Prometheus (no exponer en prod)
                                                .requestMatchers("/actuator/**").permitAll()

                                                // Endpoints de administración - Solo rol ADMIN
                                                .requestMatchers("/api/auth/admins/initialize").permitAll()
                                                .requestMatchers("/api/auth/admins/**").hasRole("ADMIN")

                                                // Endpoints de autenticación que requieren estar autenticado
                                                .requestMatchers("/api/auth/change-password").authenticated()
                                                .requestMatchers("/api/auth/logout").authenticated()
                                                .requestMatchers("/error").permitAll()
                                                // Todo lo demás requiere autenticación
                                                // Endpoints internos para comunicación entre microservicios
                                                .requestMatchers("/api/users/**").permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(e -> e
                                                .authenticationEntryPoint(authenticationEntryPoint))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}