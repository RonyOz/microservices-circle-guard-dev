# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Context

This is the **application code repo** for CircleGuard (Proyecto Final IngeSoft V).

Full workspace context: `/home/ronyoz/dev/cg/CLAUDE.md`
Project plan + completeness tracker: `/home/ronyoz/dev/cg/PROJECT-FINAL-PLAN.md`

Read both files before starting any task.

## This repo's role

Spring Boot 3.2.4 / Java 21 / Gradle multi-project. Contains all 8 microservices under `services/circleguard-<name>-service/`. The companion ops repo (`microservices-circle-guard-ops`) holds CI/CD pipelines, Helm charts, and Terraform — do not add those here.

## Build & test

```bash
# Start middleware
docker-compose -f docker-compose.dev.yml up -d

# Build all (skip tests)
./gradlew build -x test --parallel

# Test single service
./gradlew :services:circleguard-auth-service:test

# Skip promotion-service tests (Testcontainers requires Docker socket)
./gradlew build -x :services:circleguard-promotion-service:test --parallel
```

## Key architectural invariants

- Identity graph (Neo4j) never stores real names — only salted-hash tokens
- Health status promotions flow via Kafka events from promotion-service → notification-service
- Auth uses dual-chain: LDAP (university users) + Local (guests)
- K-anonymity filter (k=5) must be applied before any aggregate health stats leave dashboard-service
- Testcontainers integration tests in promotion-service spin up real Neo4j + PostgreSQL — these fail in Docker-in-Docker CI (skip with `-x :services:circleguard-promotion-service:test`)
