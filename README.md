# Plataforma de Reservas de Servicios - MS-Auth-Service

[![CI/CD Pipeline](https://github.com/Isa-Bedoya-UdeA/Reservas-MS-Auth-Service/actions/workflows/build.yml/badge.svg)](https://github.com/Isa-Bedoya-UdeA/Reservas-MS-Auth-Service/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=JuanLema14_Reservas-MS-Auth-Service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=JuanLema14_Reservas-MS-Auth-Service)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=JuanLema14_Reservas-MS-Auth-Service&metric=bugs)](https://sonarcloud.io/summary/new_code?id=JuanLema14_Reservas-MS-Auth-Service)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=JuanLema14_Reservas-MS-Auth-Service&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=JuanLema14_Reservas-MS-Auth-Service)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=JuanLema14_Reservas-MS-Auth-Service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=JuanLema14_Reservas-MS-Auth-Service)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=JuanLema14_Reservas-MS-Auth-Service&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=JuanLema14_Reservas-MS-Auth-Service)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=JuanLema14_Reservas-MS-Auth-Service&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=JuanLema14_Reservas-MS-Auth-Service)

## Descripción

CodeF@ctory - Caso 15 - Plataforma de Reservas de Servicios - Microservicio de Autenticación y Usuarios.

## Responsabilidad

* Gestión de identidad
* Autenticación
* Autorización

## Tecnologías

### Backend

* **Java 17**
* **Spring Boot 3.5.13**
* **Spring Security** (Autenticación y autorización)
* **Spring Data JPA** (Persistencia)
* **JWT** (JSON Web Tokens para autenticación)
* **MapStruct** (Mapeo entre entidades y DTOs)
* **Lombok** (Reducción de código boilerplate)
* **Maven** (Gestión de dependencias)

### Herramientas de Desarrollo

* **Git** (Control de versiones)
* **GitHub** (Repositorio remoto)
* **Postman** (Pruebas de APIs)
* **SonarCloud** (Análisis de calidad de código)

## Requisitos Previos

Antes de ejecutar el proyecto, asegúrate de tener instalado:

* **JDK 17** o superior
* **Maven 3.8+**
* **Oracle Database** o **PostgreSQL**
* **Git**

## Instalación

### 1. Clonar el Repositorio

```bash
git clone https://github.com/Isa-Bedoya-UdeA/Reservas-MS-Auth-Service
cd Reservas-MS-Auth-Service
```

### 2. Configurar la Base de Datos y Propiedades

Copia el archivo `.env.example` a `.env`:

```bash
cp .env.example .env
```

Edita el archivo `.env` con tus credenciales de Supabase:

```bash
# SPRING PROFILE
SPRING_PROFILE=dev

# DATABASE CONFIG - SUPABASE (Transaction Pooler - IPv4 compatible)
DB_URL=jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
DB_USER=postgres.[TU-PROJECT-REF]
DB_PASSWORD=[TU-CONTRASEÑA-DE-SUPABASE]

# EXTERNAL SERVICES URLs
SERVICES_CATALOG_URL=http://localhost:8082
```

### 3. Configurar JWT

Genera un JWT_SECRET seguro:

```bash
openssl rand -base64 64
```

Agrega el JWT_SECRET generado a tu archivo `.env`:

```bash
JWT_SECRET=[TU-JWT-SECRET-SEGURA]
JWT_EXPIRATION=86400000
```

> **IMPORTANTE:** El JWT_SECRET debe ser el mismo en todos los microservicios.

### 4. Compilar el Proyecto

```bash
# Limpia el target y compila
Remove-Item -Recurse -Force target -ErrorAction SilentlyContinue
mvn clean compile
```

### 5. Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

> **IMPORTANTE:** Para el correcto funcionamiento, debes tener corriendo ambos microservicios:
> - Auth Service (puerto 8081)
> - Catalog Service (puerto 8082)

La aplicación estará disponible en: `http://localhost:8081`

## Estructura del Proyecto

```
Reservas-MS-Auth-Service/
├── src/
│   ├── main/
│   │   ├── java/com/codefactory/reservasmsauthservice/
│   │   │   ├── client/              # Feign Clients para comunicación entre microservicios
│   │   │   ├── config/              # Configuración de Spring (Security, JWT, etc.)
│   │   │   ├── controller/          # Controladores REST (Auth, Verification, Health)
│   │   │   ├── dto/                 # Data Transfer Objects (Request y Response)
│   │   │   ├── entity/              # Entidades JPA (User, Client, Provider)
│   │   │   ├── exception/           # Excepciones personalizadas y manejo global
│   │   │   ├── mapper/              # Mapeadores (MapStruct) entre entidades y DTOs
│   │   │   ├── repository/          # Repositorios Spring Data JPA
│   │   │   ├── security/            # Seguridad (JWT filter, user details)
│   │   │   ├── service/             # Interfaces de servicios
│   │   │   └── service/impl/        # Implementaciones de servicios
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── email.properties      # Configuración de email
│   │       └── templates/           # Plantillas Thymeleaf para emails
│   │           └── email-verification.html
│   └── test/
│       ├── java/                    # Tests unitarios y de integración
│       └── resources/
│           └── application-test.properties
├── docs/                            # Diagramas y documentación arquitectónica
├── .env.example                     # Plantilla de variables de entorno
├── .env                             # Variables de entorno (no versionado)
├── Dockerfile                       # Configuración de Docker
├── pom.xml                          # Configuración de Maven
└── README.md
```

## Endpoints Principales

### Health Check
- `GET /api/`: Health Check - Retorna estado del servicio
- `GET /api/version`: Version Check - Retorna versión del servicio

### Registro de Usuarios
- `POST /api/auth/register/client`: Registrar un nuevo cliente
- `POST /api/auth/register/provider`: Registrar un nuevo proveedor

### Verificación de Email
- `POST /api/auth/verify-email`: Verificar email del usuario usando token
- `POST /api/auth/resend-verification-email`: Reenviar token de verificación

### Autenticación (Login)
- `POST /api/auth/login`: Iniciar sesión con email y contraseña
- `POST /api/auth/refresh`: Renovar access token usando refresh token
- `POST /api/auth/logout`: Cerrar sesión revocando refresh token

### Gestión de Contraseñas
- `POST /api/auth/password-reset/request`: Solicitar restablecimiento de contraseña (olvidé mi contraseña)
- `POST /api/auth/password-reset/confirm`: Confirmar restablecimiento de contraseña con token
- `POST /api/auth/change-password`: Cambiar contraseña (requiere autenticación)

### Gestión de Administradores (Solo rol ADMIN)
- `POST /api/auth/admins/initialize`: Inicializar primer administrador (público, solo funciona cuando no existen admins)
- `POST /api/auth/admins?creadoPor={UUID}`: Crear nuevo administrador
- `GET /api/auth/admins`: Obtener todos los administradores
- `GET /api/auth/admins/{id}`: Obtener administrador por ID
- `PUT /api/auth/admins/{id}`: Actualizar administrador
- `DELETE /api/auth/admins/{id}`: Desactivar administrador (soft delete)
- `PATCH /api/auth/admins/{id}/activate`: Reactivar administrador

## Autenticación y Gestión de Sesiones

El sistema implementa autenticación basada en JWT (JSON Web Tokens) con un mecanismo de refresh tokens para mantener sesiones activas de forma segura. La autenticación incluye medidas de seguridad como límite de intentos fallidos, bloqueo temporal de cuentas y auditoría de intentos de login.

### Características de Seguridad

- **Email verificado obligatorio:** No se puede iniciar sesión si el email no ha sido verificado.
- **Límite de intentos fallidos:** Después de 5 intentos fallidos en 1 hora, la cuenta se bloquea temporalmente.
- **Bloqueo temporal:** Las cuentas bloqueadas permanecen bloqueadas por 24 horas.
- **Reset automático:** Los intentos fallidos se resetean cuando el login es exitoso o cuando el bloqueo expira.
- **Auditoría:** Todos los intentos de login (exitosos y fallidos) se registran con IP y User-Agent.
- **Refresh tokens:** Tokens de larga duración (7 días) para renovar access tokens sin re-authenticar.
- **Rotación de tokens:** Los refresh tokens se rotan en cada renovación para mayor seguridad.

### Tiempos de Expiración

- **Access Token:** 15 minutos (900,000 ms) - Token de corto plazo para acceso a recursos protegidos.
- **Refresh Token:** 7 días (604,800,000 ms) - Token de largo plazo para renovar access tokens.
- **Bloqueo de cuenta:** 24 horas después de 5 intentos fallidos.

### Flujo de Login

1. El usuario envía email y contraseña al endpoint `/api/auth/login`.
2. El sistema valida que el email esté verificado.
3. El sistema verifica si la cuenta está bloqueada y si el bloqueo aún es válido.
4. Si el bloqueo expiró, se resetean los intentos fallidos.
5. Se valida la contraseña usando BCrypt.
6. Si la contraseña es incorrecta:
   - Se incrementa el contador de intentos fallidos.
   - Se registra el intento fallido en la tabla `intento_login`.
   - Si se alcanza el límite (5 intentos), la cuenta se bloquea por 24 horas.
7. Si la contraseña es correcta:
   - Se resetean los intentos fallidos a 0.
   - Se registra el intento exitoso.
   - Se genera un access token (15 min) con claims: email, role, userId.
   - Se genera un refresh token (7 días) y se guarda en la tabla `token_refresh`.
   - Se retorna el access token, refresh token y datos del usuario.

### Flujo de Refresh Token

1. El usuario envía el refresh token al endpoint `/api/auth/refresh`.
2. El sistema busca el token en la base de datos.
3. Se valida que el token no esté revocado ni expirado.
4. Se valida el token con JWT.
5. Se genera un nuevo access token (15 min).
6. Se genera un nuevo refresh token (rotación) y se revoca el anterior.
7. Se retorna el nuevo access token y refresh token.

### Flujo de Logout

1. El usuario envía el refresh token al endpoint `/api/auth/logout`.
2. El sistema busca el token en la base de datos.
3. Se marca el token como revocado con la fecha de revocación.
4. El token ya no puede usarse para renovar access tokens.

### Códigos de Error HTTP

| Código | Error | Descripción |
|--------|-------|-------------|
| 400 | Bad Request | Email o password vacíos/nulos |
| 401 | Unauthorized | Password incorrecto o token inválido |
| 403 | Forbidden | Email no verificado |
| 404 | Not Found | Usuario no existe |
| 423 | Locked | Cuenta bloqueada por intentos fallidos |

### Configuración

Las siguientes propiedades se configuran en `application.properties`:

```properties
# JWT Configuration
jwt.secret=${JWT_SECRET:default-secret-key-change-in-prod}
jwt.access-expiration=${JWT_ACCESS_EXPIRATION:900000}
jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION:604800000}

# Login Security Configuration
login.max-attempts=${LOGIN_MAX_ATTEMPTS:5}
login.lockout-duration-hours=${LOGIN_LOCKOUT_DURATION_HOURS:24}
```

### Variables de Entorno

```bash
# JWT Secret (debe ser el mismo en todos los microservicios)
JWT_SECRET=tu-jwt-secret-segura

# Tiempos de expiración (opcional, usa defaults si no se especifica)
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Configuración de seguridad de login (opcional)
LOGIN_MAX_ATTEMPTS=5
LOGIN_LOCKOUT_DURATION_HOURS=24
```

## Gestión de Contraseñas

### Descripción

El sistema proporciona dos funcionalidades para la gestión de contraseñas:

1. **Olvidé mi contraseña (Password Reset)** - Permite a los usuarios recuperar su cuenta cuando olvidan su contraseña, sin necesidad de autenticación.
2. **Cambiar contraseña (Change Password)** - Permite a los usuarios autenticados cambiar su contraseña actual por una nueva.

Ambas funcionalidades incluyen validaciones de formato de contraseña, envío de correos electrónicos de confirmación y revocación de sesiones activas para mayor seguridad.

### Flujo de "Olvidé mi Contraseña" (Password Reset)

Este flujo permite a los usuarios recuperar su cuenta sin necesidad de autenticación.

1. El usuario solicita el restablecimiento enviando su email al endpoint `/api/auth/password-reset/request`.
2. El sistema valida que el email existe en la base de datos.
3. Se invalidan cualquier token de reset existente para ese usuario.
4. Se genera un nuevo token único (UUID) con expiración de 24 horas.
5. Se envía un email al usuario con un enlace para restablecer la contraseña: `{frontendUrl}/reset-password?token={token}`.
6. El usuario hace clic en el enlace y es redirigido al frontend.
7. El usuario ingresa su nueva contraseña en el formulario.
8. El usuario envía la nueva contraseña al endpoint `/api/auth/password-reset/confirm` con el token.
9. El sistema valida que el token sea válido (existe, no usado, no expirado).
10. El sistema valida que la nueva contraseña cumpla con los requisitos de formato.
11. Se actualiza la contraseña del usuario con hash BCrypt.
12. Se marca el token como usado.
13. Se revocan todos los refresh tokens del usuario (todas las sesiones activas se cierran).
14. Se envía un email de confirmación al usuario con aviso de seguridad.
15. El usuario debe iniciar sesión nuevamente con su nueva contraseña.

#### Endpoint: Solicitar Reset

**Request:**
```bash
POST /api/auth/password-reset/request
Content-Type: application/json

{
  "email": "usuario@ejemplo.com"
}
```

**Response:**
```json
{
  "message": "Si el email existe en nuestro sistema, recibirás un enlace para restablecer tu contraseña",
  "success": true,
  "timestamp": "2026-04-16T16:30:00Z"
}
```

#### Endpoint: Confirmar Reset

**Request:**
```bash
POST /api/auth/password-reset/confirm
Content-Type: application/json

{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "NuevaContraseña123!"
}
```

**Response:**
```json
{
  "message": "Contraseña restablecida exitosamente",
  "success": true,
  "timestamp": "2026-04-16T16:35:00Z"
}
```

### Flujo de "Cambiar Contraseña" (Change Password)

Este flujo permite a los usuarios autenticados cambiar su contraseña actual por una nueva.

1. El usuario autenticado va a la configuración de su cuenta.
2. Ingresa su contraseña actual y la nueva contraseña.
3. Envía la solicitud al endpoint `/api/auth/change-password` con el JWT válido en el header `Authorization`.
4. El sistema extrae el `userId` del JWT.
5. El sistema valida que la contraseña actual sea correcta (comparando con el hash almacenado).
6. El sistema valida que la nueva contraseña cumpla con los requisitos de formato.
7. El sistema valida que la nueva contraseña sea diferente a la actual.
8. Se actualiza la contraseña del usuario con hash BCrypt.
9. Se revocan todos los refresh tokens del usuario (todas las sesiones activas se cierran).
10. Se envía un email de confirmación al usuario con aviso de seguridad.
11. El usuario debe iniciar sesión nuevamente con su nueva contraseña.

#### Endpoint: Cambiar Contraseña

**Request:**
```bash
POST /api/auth/change-password
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "currentPassword": "ContraseñaActual123!",
  "newPassword": "NuevaContraseña456!"
}
```

**Response:**
```json
{
  "message": "Contraseña cambiada exitosamente",
  "success": true,
  "timestamp": "2026-04-16T16:40:00Z"
}
```

### Códigos de Error HTTP

| Código | Error | Descripción |
|--------|-------|-------------|
| 400 | Bad Request | Email vacío, contraseña no cumple formato, misma contraseña |
| 401 | Unauthorized | Contraseña actual incorrecta |
| 404 | Not Found | Email no existe, token no existe |
| 410 | Gone | Token expirado o ya usado |

### Configuración

Las siguientes propiedades se configuran en `application.properties`:

```properties
# Password Reset Configuration
password.reset.token.expiration.hours=${PASSWORD_RESET_TOKEN_EXPIRATION_HOURS:24}
```

### Variables de Entorno

```bash
# Configuración de reset de contraseña (opcional)
PASSWORD_RESET_TOKEN_EXPIRATION_HOURS=24
```

### Seguridad Adicional

- **Revocación de sesiones:** Al cambiar la contraseña (ambos flujos), se revocan todos los refresh tokens del usuario, cerrando todas las sesiones activas.
- **Email de confirmación:** Se envía un email de confirmación con aviso de seguridad para detectar cambios no autorizados.
- **Validación de token:** Los tokens de reset son de un solo uso y expiran después de 24 horas.
- **Invalidación de tokens antiguos:** Al solicitar un nuevo reset, se invalidan los tokens anteriores del usuario.

## Relaciones entre Entidades

- **User**: Entidad base con email, password y rol
- **Client**: Extiende de User, representa a un cliente que reserva servicios
- **Provider**: Extiende de User, representa a un proveedor que ofrece servicios
- **EmailVerificationToken**: Token para verificación de email, asociado a un User

## Diagramas

### Diagrama del Modelo de Dominio
[docs/domain-model.png]
(Pendiente)

### Diagrama de Arquitectura C4
[docs/architecture-c4.png]
(Pendiente)

### Diagrama de Componentes
[docs/components.png]
(Pendiente)

### Diagrama de Secuencia
[docs/sequence.png]
(Pendiente)

### Diagrama MER Lógico
![MER Lógico](docs/mer-diagram.png)

### ADRs (Architecture Decision Records)
[docs/adrs/]
(Pendiente)

### Documentación de API (Swagger/OpenAPI)
![Swagger](docs/swagger.png)

**Ruta de acceso:** http://localhost:8081/swagger-ui/index.html#/

### Variables de Entorno para Despliegue
[docs/environment-variables.md]
(Pendiente)

## Pruebas en Postman

Para ver todos los ejemplos de pruebas en Postman, incluyendo registro de usuarios y verificación de email, consulta el archivo: **[docs/PruebasPostman.md](docs/PruebasPostman.md)**
