# Variables de Entorno - Reservas-MS-Auth-Service

Este documento describe todas las variables de entorno necesarias para el despliegue del microservicio de Autenticación.

## Archivo de Configuración

Copia el archivo `.env.example` a `.env` y configura los valores:

```bash
cp .env.example .env
```

## Variables Requeridas

### 1. Perfil de Spring

| Variable | Descripción | Valor por Defecto | Opciones |
|----------|-------------|-------------------|----------|
| `SPRING_PROFILE` | Perfil activo de Spring Boot | `dev` | `dev`, `test`, `prod` |

### 2. Configuración de Base de Datos (Supabase)

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `DB_URL` | URL JDBC de PostgreSQL (Transaction Pooler IPv4) | `jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0` |
| `DB_USER` | Usuario de la base de datos | `postgres.[PROJECT-REF]` |
| `DB_PASSWORD` | Contraseña de Supabase | `[TU-CONTRASEÑA]` |

**Nota:** Se recomienda usar el Transaction Pooler de Supabase (puerto 6543) para compatibilidad con IPv4.

### 3. Configuración JWT

| Variable | Descripción | Recomendación |
|----------|-------------|---------------|
| `JWT_SECRET` | Secreto para firmar tokens JWT | Generar con: `openssl rand -base64 64` |
| `JWT_EXPIRATION` | Tiempo de expiración del token en milisegundos | `86400000` (24 horas) |

⚠️ **IMPORTANTE:** El `JWT_SECRET` debe ser el **MISMO VALOR** en todos los microservicios para que la validación de tokens funcione correctamente.

### 4. Configuración de Email (Gmail SMTP)

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `EMAIL_USERNAME` | Dirección de correo Gmail | `tucorreo@gmail.com` |
| `EMAIL_PASSWORD` | App Password de Google | `xxxx xxxx xxxx xxxx` |

**Para obtener el App Password:**
1. Ve a: https://myaccount.google.com/apppasswords
2. Genera una contraseña de aplicación para "Correo"
3. Usa esa contraseña en `EMAIL_PASSWORD`

### 5. URL del Frontend

| Variable | Descripción | Valor Local | Valor Producción |
|----------|-------------|-------------|------------------|
| `FRONTEND_URL` | URL base para enlaces de verificación | `http://localhost:3000` | `https://tu-dominio.com` |

### 6. URLs de Servicios Externos

| Variable | Descripción | Puerto Local |
|----------|-------------|--------------|
| `SERVICES_AUTH_URL` | URL del Auth Service (este mismo) | `http://localhost:8081` |
| `SERVICES_CATALOG_URL` | URL del Catalog Service | `http://localhost:8082` |
| `SERVICES_RESOURCE_URL` | URL del Resource/Schedule Service | `http://localhost:8083` |
| `SERVICES_RESERVATION_URL` | URL del Reservation Service | `http://localhost:8084` |

## Ejemplo Completo (.env)

```bash
# ======================
# SPRING PROFILE
# ======================
SPRING_PROFILE=dev

# ======================
# DATABASE CONFIG
# ======================
DB_URL=jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
DB_USER=postgres.[TU-PROJECT-REF]
DB_PASSWORD=[TU-CONTRASEÑA-DE-SUPABASE]

# ======================
# JWT CONFIG (MISMO EN TODOS LOS MS)
# ======================
JWT_SECRET=[TU-JWT-SECRET-SEGURA]
JWT_EXPIRATION=86400000

# ======================
# EMAIL CONFIG
# ======================
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

# ======================
# FRONTEND URL
# ======================
FRONTEND_URL=http://localhost:3000

# ======================
# EXTERNAL SERVICES URLs
# ======================
SERVICES_AUTH_URL=http://localhost:8081
SERVICES_CATALOG_URL=http://localhost:8082
SERVICES_RESOURCE_URL=http://localhost:8083
SERVICES_RESERVATION_URL=http://localhost:8084
```

## Despliegue en Producción (Render)

Cuando despliegues en Render, configura estas variables en el dashboard:

1. Ve a tu servicio en Render Dashboard
2. Sección "Environment"
3. Agrega cada variable con su valor correspondiente
4. Para producción, actualiza las URLs de servicios:
   - `SERVICES_AUTH_URL=https://ms-auth-service.onrender.com`
   - `SERVICES_CATALOG_URL=https://ms-catalog-service.onrender.com`
   - etc.

## Verificación

Para verificar que todas las variables están configuradas correctamente:

```bash
# Ver el perfil activo
cat .env | grep SPRING_PROFILE

# Iniciar el servicio y verificar logs
mvn spring-boot:run
```

En los logs de inicio aparecerá la validación de configuración mostrando qué variables están configuradas.
