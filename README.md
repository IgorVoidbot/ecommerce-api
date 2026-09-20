# E-commerce API

API REST de e-commerce desenvolvida como projeto de portfólio com foco em vagas de
pessoa desenvolvedora Java/Spring júnior.

## Status

Fase 1 de 6 — Setup do projeto.

## Stack

- Java 21
- Spring Boot 3.4
- Spring Data JPA + Flyway
- PostgreSQL 16 (produção) / H2 (testes)
- Docker + Docker Compose
- Maven (wrapper incluído, não precisa instalar)

## Pré-requisitos

- JDK 21
- Docker

## Como rodar

1. Subir o banco:

   ```bash
   docker compose up -d
   ```

2. Rodar a aplicação:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Healthcheck: `http://localhost:8080/actuator/health`

## Como rodar os testes

```bash
./mvnw clean verify
```

## Documentação

O documento de requisitos completo está em [PRD.md](PRD.md).
