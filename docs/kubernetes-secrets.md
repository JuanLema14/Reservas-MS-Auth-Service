# Secrets para Kubernetes - MS-Auth-Service

Este documento describe las variables necesarias para el archivo `secrets.yaml` requerido para el despliegue en Kubernetes.

## Ubicación del Archivo

El archivo `secrets.yaml` debe estar en la carpeta `kubernetes/` del proyecto:

```
Reservas-MS-Auth-Service/
└── kubernetes/
    ├── k8s.yaml        # ConfigMap + Deployment + Service
    ├── secrets.yaml    # Secrets (NO subir a Git)
    └── .gitignore      # Ignora secrets.yaml
```

## Variables Requeridas

### 1. Base de Datos (Supabase)

| Variable | Descripción |
|----------|-------------|
| `DB_URL` | URL JDBC de PostgreSQL (Transaction Pooler) |
| `DB_USER` | Usuario de Supabase (`postgres.[PROJECT-REF]`) |
| `DB_PASSWORD` | Contraseña de la base de datos |

### 2. JWT

| Variable | Descripción |
|----------|-------------|
| `JWT_SECRET` | Secreto para firmar tokens JWT |
| `JWT_EXPIRATION` | Tiempo de expiración del token (milisegundos) |

> **IMPORTANTE:** El `JWT_SECRET` debe ser **el mismo valor** en todos los microservicios.

### 3. Email (Gmail SMTP)

| Variable | Descripción |
|----------|-------------|
| `EMAIL_USERNAME` | Dirección de correo Gmail |
| `EMAIL_PASSWORD` | App Password de Google |

### 4. URLs de Servicios

| Variable | Descripción |
|----------|-------------|
| `FRONTEND_URL` | URL base del frontend |
| `SERVICES_CATALOG_URL` | URL del Catalog Service (`http://catalog-service:8082`) |

## Ejemplo de secrets.yaml

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-service-secrets
  namespace: default
type: Opaque
stringData:
  DB_URL: "jdbc:postgresql://..."
  DB_USER: "postgres.[PROJECT-REF]"
  DB_PASSWORD: "[TU-CONTRASEÑA]"
  JWT_SECRET: "[TU-JWT-SECRET]"
  JWT_EXPIRATION: "86400000"
  EMAIL_USERNAME: "tu@email.com"
  EMAIL_PASSWORD: "[APP-PASSWORD]"
  FRONTEND_URL: "https://tu-frontend.vercel.app"
  SERVICES_CATALOG_URL: "http://catalog-service:8082"
```

## Aplicar en Kubernetes

```bash
kubectl apply -f kubernetes/secrets.yaml
```

## Seguridad

⚠️ **NUNCA** subas el archivo `secrets.yaml` a Git. Ya está incluido en `.gitignore`.
