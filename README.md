# DevolucionesApp

Plataforma MVP para gestionar solicitudes de devolución de pagos duplicados o en exceso.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.0, Spring Security, JPA/Hibernate |
| Frontend | Angular 17, Tailwind CSS |
| Base de datos | PostgreSQL 16 |
| Infra | Docker, Docker Compose |

## Levantar

```bash
# Copiar variables de entorno
cp devoluciones-app-back/.env.example .env

# Levantar todo (DB + backend + frontend)
docker compose up -d

# Verificar
curl http://localhost:8080/api/v1/auth/login   # backend
# Frontend: http://localhost:4200
```

## Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| `analista1` | `123456` | ANALISTA |
| `supervisor1` | `123456` | SUPERVISOR |

## Estructura del repo

```
├── docker-compose.yml          # Levanta db + backend + frontend
├── devoluciones-app-back/      # API REST (Spring Boot)
│   ├── src/
│   │   ├── main/java/          # Controllers, Services, Models
│   │   ├── main/resources/     # application.yaml, data.sql
│   │   └── test/               # Tests unitarios
│   ├── Dockerfile
│   └── pom.xml
└── devoluciones-app/           # Frontend (Angular 17)
    ├── src/
    ├── Dockerfile
    └── nginx.conf
```

## API principal

```
POST   /api/v1/auth/login                    # Login (JWT)
POST   /api/v1/solicitudes                   # Crear solicitud
GET    /api/v1/solicitudes                   # Listar (paginado, filtros)
POST   /api/v1/solicitudes/{id}/enviar       # BORRADOR → EN_REVISION
POST   /api/v1/solicitudes/{id}/aprobar      # EN_REVISION → APROBADA (SUPERVISOR)
POST   /api/v1/solicitudes/{id}/rechazar     # EN_REVISION → RECHAZADA (SUPERVISOR)
POST   /api/v1/solicitudes/{id}/pagar        # APROBADA → PAGADA (SUPERVISOR)
POST   /api/v1/solicitudes/{id}/reabrir      # RECHAZADA → BORRADOR (máx. 1 vez)
POST   /api/v1/cargas                        # Carga masiva CSV
```

## Tests

```bash
cd devoluciones-app-back
./mvnw test
```

## Decisiones de diseño

- **Máquina de estados**: R1-R7 implementadas en `SolicitudService` con transacciones atómicas (evento + cambio de estado en la misma transacción).
- **Idempotencia CSV**: `referencia_banco` tiene constraint UNIQUE en DB; re-subir el mismo archivo retorna 409 en filas duplicadas.
- **Validación RUT**: Validador custom Bean Validation con módulo 11, soporta DV `K`.
- **JWT**: Cookie-based con refresh token; stateless en Spring Security.
