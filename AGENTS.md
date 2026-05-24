# AGENTS.md — microservices-circle-guard-dev

Para opencode. Ver contexto completo del workspace en `/home/ronyoz/dev/cg/AGENTS.md` y `CLAUDE.md`.

## Rol de este repo

Spring Boot 3.2.4 / Java 21 / Gradle multi-project. Contiene los 8 microservicios en `services/circleguard-<name>-service/`. El ops repo (`microservices-circle-guard-ops`) tiene CI/CD, Helm charts y Terraform.

## Build & test

```bash
# Start middleware (PG, Neo4j, Kafka, Redis, OpenLDAP)
docker-compose -f docker-compose.dev.yml up -d

# Build all (skip tests)
./gradlew build -x test --parallel

# Test single service
./gradlew :services:circleguard-auth-service:test

# Skip promotion-service tests (Testcontainers needs Docker socket)
./gradlew build -x :services:circleguard-promotion-service:test --parallel
```

## Invariantes arquitectónicos clave

- Grafo de identidad (Neo4j) nunca guarda nombres reales — solo salted-hash tokens
- Promociones de status fluyen via Kafka events: promotion-service → notification-service
- Auth usa dual-chain: LDAP (universitarios) + Local (invitados)
- K-anonymity filter (k=5) en dashboard-service antes de agregados
- OpenAPI docs: `http://localhost:<port>/swagger-ui/index.html`

## Plan activo

`/home/ronyoz/dev/cg/PROJECT-FINAL-PLAN.md` — leer antes de cualquier tarea.
