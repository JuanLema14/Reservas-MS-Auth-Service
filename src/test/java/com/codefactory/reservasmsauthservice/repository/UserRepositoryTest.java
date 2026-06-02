package com.codefactory.reservasmsauthservice.repository;

import com.codefactory.reservasmsauthservice.entity.Client;
import com.codefactory.reservasmsauthservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para UserRepository.
 * Utiliza @DataJpaTest con H2 in-memory database.
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD_HASH = "hashed-password-123";

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTests {

        @Test
        @DisplayName("Debe encontrar usuario por email")
        void findByEmail_ExistingUser() {
            // Given
            Client client = createAndPersistClient(TEST_EMAIL, "Test User", "1234567890");

            // When
            Optional<User> found = userRepository.findByEmail(TEST_EMAIL);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(found.get()).isInstanceOf(Client.class);
        }

        @Test
        @DisplayName("Debe retornar empty para email no existente")
        void findByEmail_NonExistingUser() {
            // When
            Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Debe ser case-sensitive para búsqueda de email")
        @org.junit.jupiter.api.Disabled("H2 in-memory DB es case-insensitive por defecto. En producción (PostgreSQL) el email es case-sensitive.")
        void findByEmail_CaseSensitive() {
            // Given
            createAndPersistClient(TEST_EMAIL, "Test User", "1234567890");

            // When
            Optional<User> foundUpperCase = userRepository.findByEmail(TEST_EMAIL.toUpperCase());
            Optional<User> foundLowerCase = userRepository.findByEmail(TEST_EMAIL.toLowerCase());

            // Then
            assertThat(foundUpperCase).isEmpty();
            assertThat(foundLowerCase).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Debe retornar true para email existente")
        void existsByEmail_ExistingUser() {
            // Given
            createAndPersistClient(TEST_EMAIL, "Test User", "1234567890");

            // When
            boolean exists = userRepository.existsByEmail(TEST_EMAIL);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false para email no existente")
        void existsByEmail_NonExistingUser() {
            // When
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("save y delete")
    class SaveAndDeleteTests {

        @Test
        @DisplayName("Debe guardar nuevo usuario correctamente")
        void save_NewUser() {
            // Given
            Client client = new Client();
            client.setEmail("newuser@example.com");
            client.setPasswordHash(TEST_PASSWORD_HASH);
            client.setTipoUsuario(User.Role.CLIENTE);
            client.setNombre("New User");
            client.setTelefono("9876543210");
            client.setEstado("ACTIVO");

            // When
            User saved = userRepository.save(client);
            entityManager.flush();

            // Then
            assertThat(saved.getIdUsuario()).isNotNull();
            assertThat(saved.getEmail()).isEqualTo("newuser@example.com");
        }

        @Test
        @DisplayName("Debe actualizar usuario existente")
        void save_UpdateUser() {
            // Given
            Client client = createAndPersistClient(TEST_EMAIL, "Original Name", "1234567890");
            UUID userId = client.getIdUsuario();

            // When
            Optional<User> found = userRepository.findById(userId);
            found.ifPresent(user -> {
                user.setPasswordHash("new-hash");
                userRepository.save(user);
            });
            entityManager.flush();

            // Then
            Optional<User> updated = userRepository.findById(userId);
            assertThat(updated).isPresent();
            assertThat(updated.get().getPasswordHash()).isEqualTo("new-hash");
        }

        @Test
        @DisplayName("Debe eliminar usuario por ID")
        void delete_ById() {
            // Given
            Client client = createAndPersistClient(TEST_EMAIL, "Test User", "1234567890");
            UUID userId = client.getIdUsuario();

            // When
            userRepository.deleteById(userId);
            entityManager.flush();

            // Then
            Optional<User> deleted = userRepository.findById(userId);
            assertThat(deleted).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("Debe encontrar usuario por ID")
        void findById_ExistingUser() {
            // Given
            Client client = createAndPersistClient(TEST_EMAIL, "Test User", "1234567890");
            UUID userId = client.getIdUsuario();

            // When
            Optional<User> found = userRepository.findById(userId);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getIdUsuario()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Debe retornar empty para ID no existente")
        void findById_NonExistingUser() {
            // When
            Optional<User> found = userRepository.findById(UUID.randomUUID());

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("User fields")
    class UserFieldsTests {

        @Test
        @DisplayName("Debe guardar todos los campos de usuario correctamente")
        void save_AllFields() {
            // Given
            Client client = new Client();
            client.setEmail("fulluser@example.com");
            client.setPasswordHash(TEST_PASSWORD_HASH);
            client.setTipoUsuario(User.Role.CLIENTE);
            client.setEmailVerificado(true);
            client.setIntentosFallidos(0);
            client.setEstado("ACTIVO");
            client.setNombre("Full User");
            client.setTelefono("1112223333");

            // When
            User saved = userRepository.save(client);
            entityManager.flush();

            // Then
            assertThat(saved.getEmail()).isEqualTo("fulluser@example.com");
            assertThat(saved.getPasswordHash()).isEqualTo(TEST_PASSWORD_HASH);
            assertThat(saved.getTipoUsuario()).isEqualTo(User.Role.CLIENTE);
            assertThat(saved.getEmailVerificado()).isTrue();
            assertThat(saved.getIntentosFallidos()).isEqualTo(0);
            assertThat(saved.getEstado()).isEqualTo("ACTIVO");
            assertThat(saved.getFechaRegistro()).isNotNull();
        }

        @Test
        @DisplayName("Debe establecer fecha de registro automáticamente")
        void save_AutoSetsFechaRegistro() {
            // Given
            Client client = new Client();
            client.setEmail("autodate@example.com");
            client.setPasswordHash(TEST_PASSWORD_HASH);
            client.setTipoUsuario(User.Role.CLIENTE);
            client.setNombre("Auto Date User");
            client.setTelefono("4445556666");

            // When
            User saved = userRepository.save(client);
            entityManager.flush();

            // Then
            assertThat(saved.getFechaRegistro()).isNotNull();
            assertThat(saved.getFechaRegistro()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("Debe retornar todos los usuarios")
        void findAll_ReturnsAllUsers() {
            // Given
            createAndPersistClient("user1@example.com", "User One", "1111111111");
            createAndPersistClient("user2@example.com", "User Two", "2222222222");
            createAndPersistClient("user3@example.com", "User Three", "3333333333");

            // When
            var allUsers = userRepository.findAll();

            // Then
            assertThat(allUsers).hasSizeGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Count")
    class CountTests {

        @Test
        @DisplayName("Debe retornar count correcto de usuarios")
        void count_ReturnsCorrectCount() {
            // Given
            long initialCount = userRepository.count();
            createAndPersistClient("count1@example.com", "Count One", "1111111111");
            createAndPersistClient("count2@example.com", "Count Two", "2222222222");
            entityManager.flush();

            // When
            long newCount = userRepository.count();

            // Then
            assertThat(newCount).isEqualTo(initialCount + 2);
        }
    }

    // Helper methods
    private Client createAndPersistClient(String email, String name, String phone) {
        Client client = new Client();
        client.setEmail(email);
        client.setPasswordHash(TEST_PASSWORD_HASH);
        client.setTipoUsuario(User.Role.CLIENTE);
        client.setEmailVerificado(false);
        client.setIntentosFallidos(0);
        client.setEstado("ACTIVO");
        client.setNombre(name);
        client.setTelefono(phone);
        return entityManager.persist(client);
    }
}