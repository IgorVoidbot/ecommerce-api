# E-commerce API

API REST de e-commerce desenvolvida como projeto de portfólio com foco em vagas de
pessoa desenvolvedora Java/Spring júnior.

Funcionalidades: cadastro e autenticação JWT, CRUD de categorias e produtos com
paginação e filtros, carrinho de compras por usuário com validação de estoque e
fluxo de pedidos (checkout, baixa de estoque, cancelamento com devolução).

## Status

Todas as 6 fases do [PRD](PRD.md) concluídas — release `v1.0.0`.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (LTS) |
| Framework | Spring Boot 3.4 |
| Persistência | Spring Data JPA + Hibernate + Flyway |
| Segurança | Spring Security + JWT (jjwt 0.12) |
| Banco de dados | PostgreSQL 16 (produção) / H2 (testes) |
| Documentação | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5 + Mockito + MockMvc + JaCoCo |
| Build | Maven (wrapper incluído, não precisa instalar) |
| Container | Docker + Docker Compose |
| CI | GitHub Actions |

## Como rodar (Docker — recomendado)

```bash
docker compose up --build
```

- API: `http://localhost:8080`
- Healthcheck: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

O Postgres sobe com a API (volume persistente). O banco é migrado automaticamente
pelo Flyway e já vem com um usuário admin e categorias iniciais.

## Como rodar (local)

Pré-requisitos: JDK 21 e Docker (apenas para o banco).

```bash
# 1. subir o banco
docker compose up -d postgres

# 2. rodar a aplicação
./mvnw spring-boot:run
```

## Credenciais iniciais

| Campo | Valor |
|---|---|
| E-mail | `admin@ecommerce.com` |
| Senha | `admin123` |
| Role | `ADMIN` |

## Autenticação

1. Registrar usuário (público):

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Igor", "email": "igor@example.com", "password": "senha1234"}'
```

2. Login (público) — retorna `token`, `expiresIn` (segundos, 2h), `type` e `user`:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@ecommerce.com", "password": "admin123"}'
```

3. Usar o token nas próximas requisições:

```bash
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <TOKEN>"
```

## Endpoints

Base URL: `/api/v1`

### Autenticação

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| POST | `/auth/register` | Pública | Cadastrar usuário (201) |
| POST | `/auth/login` | Pública | Autenticar e obter JWT |

### Categorias

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| GET | `/categories` | Autenticado | Listar categorias |
| GET | `/categories/{id}` | Autenticado | Buscar categoria |
| POST | `/categories` | ADMIN | Criar categoria (201) |
| PUT | `/categories/{id}` | ADMIN | Atualizar categoria |
| DELETE | `/categories/{id}` | ADMIN | Excluir categoria (204; 409 se houver produtos) |

### Produtos

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| GET | `/products?page=&size=&sort=&name=&categoryId=` | Autenticado | Listar com paginação e filtros |
| GET | `/products/{id}` | Autenticado | Buscar produto |
| POST | `/products` | ADMIN | Criar produto (201) |
| PUT | `/products/{id}` | ADMIN | Atualizar produto (aceita campos parciais) |
| DELETE | `/products/{id}` | ADMIN | Excluir produto (204) |
| PATCH | `/products/{id}/stock` | ADMIN | Ajustar estoque: `{"quantity": 10}` (positivo = entrada, negativo = saída) |

### Carrinho

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| GET | `/cart` | Autenticado | Consultar carrinho com itens e total |
| POST | `/cart/items` | Autenticado | Adicionar `{"productId": 1, "quantity": 2}` (201; produto repetido soma quantidade) |
| PUT | `/cart/items/{itemId}` | Autenticado | Alterar quantidade `{"quantity": 3}` |
| DELETE | `/cart/items/{itemId}` | Autenticado | Remover item (204) |
| DELETE | `/cart` | Autenticado | Limpar carrinho (204) |

### Pedidos

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| POST | `/orders` | Autenticado | Checkout do carrinho (201; baixa estoque e limpa carrinho) |
| GET | `/orders?page=&size=` | Autenticado | Listar pedidos do usuário |
| GET | `/orders/{id}` | Autenticado (dono ou admin) | Detalhe do pedido |
| GET | `/admin/orders?page=&size=` | ADMIN | Listar todos os pedidos |
| PATCH | `/admin/orders/{id}/status` | ADMIN | Atualizar status `{"status": "PAID"}` |

Fluxo de status: `PENDING → PAID → SHIPPED → DELIVERED`; `PENDING`/`PAID` podem
ser `CANCELLED` (devolve os itens ao estoque).

## Tratamento de erros

Toda resposta de erro segue o padrão JSON:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product not found with id: 999",
  "instance": "/api/v1/products/999",
  "timestamp": "2026-09-20T20:15:30.123456-03:00",
  "error": "Not Found",
  "message": "Product not found with id: 999",
  "path": "/api/v1/products/999"
}
```

Erros de validação (400) incluem o mapa `errors` com campo → mensagem.

## Como rodar os testes

```bash
./mvnw clean verify
```

O build executa os testes (H2 + Flyway), o `jacoco:check` (cobertura mínima de 70%
nas services) e gera o relatório em `target/site/jacoco/index.html`.

## CI

O workflow `.github/workflows/ci.yml` roda `./mvnw clean verify` em todo push/PR
nas branches `main` e `develop` (JDK 21).

## Arquitetura

Camadas: Controller → Service (interface + impl) → Repository, com DTOs entre as
camadas (entidades nunca são expostas nas respostas), exceções de negócio tratadas
por `@RestControllerAdvice` e operações multi-tabela transacionais (checkout).

```
com.github.igor.ecommerce
├── config/          # SecurityConfig, OpenApiConfig
├── controller/      # Endpoints REST
├── service/         # Regras de negócio (+ impl/)
├── repository/      # Spring Data JPA
├── entity/          # Entidades JPA
├── dto/             # request/response
├── mapper/          # entity ↔ dto
├── exception/       # Exceções customizadas + handler global
├── security/        # JwtService, JwtAuthFilter, UserDetailsService
└── enums/           # Role, OrderStatus
```

## Documentação

- Requisitos completos: [PRD.md](PRD.md)
- Planejamento das sprints: [sprints.md](sprints.md)
- API interativa: `http://localhost:8080/swagger-ui.html` (Bearer JWT configurado)
