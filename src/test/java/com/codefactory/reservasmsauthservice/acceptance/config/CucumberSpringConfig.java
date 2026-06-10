package com.codefactory.reservasmsauthservice.acceptance.config;

import com.codefactory.reservasmsauthservice.client.CatalogClient;
import com.codefactory.reservasmsauthservice.mapper.ProviderMapper;
import com.codefactory.reservasmsauthservice.repository.ClientRepository;
import com.codefactory.reservasmsauthservice.repository.ProviderRepository;
import com.codefactory.reservasmsauthservice.service.AuthService;
import com.codefactory.reservasmsauthservice.service.EmailService;
import com.codefactory.reservasmsauthservice.service.LoginService;
import com.codefactory.reservasmsauthservice.service.UserAuthService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfig {

    @MockBean
    public LoginService loginService;

    @MockBean
    public ClientRepository clientRepository;

    @MockBean
    public UserAuthService userAuthService;

    @MockBean
    public ProviderRepository providerRepository;

    @MockBean
    public CatalogClient catalogClient;

    @MockBean
    public EmailService emailService;

    @MockBean
    public ProviderMapper providerMapper;

    @MockBean
    public AuthService authService;
}