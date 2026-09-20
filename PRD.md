# PRD — E-commerce API

Documento de Requisitos do Produto (PRD) do projeto de portfólio **E-commerce API**.
Projeto iniciante em Java/Spring Boot com foco em conseguir vaga de pessoa desenvolvedora júnior.

| Item | Descrição |
|---|---|
| Nome do projeto | ecommerce-api |
| Tipo | API REST (backend) |
| Autor | Igor |
| Data | 20/09/2026 |
| Status | Aprovado |

---

## 1. Visão Geral

### 1.1 Objetivo

Desenvolver uma API REST de e-commerce funcional de ponta a ponta, que sirva como
projeto de portfólio demonstrando domínio de: Spring Boot, Spring Data JPA,
Spring Security com JWT, validações, tratamento de erros, testes automatizados,
Docker e documentação de API.

### 1.2 Problema que resolve

Demonstrar capacidade de construir um sistema real com autenticação, autorização,
gestão de estoque e fluxo de compra — cobrindo o stack mais exigido em vagas Java júnior.

### 1.3 Público-alvo do portfólio

- Empresas que contratam pessoas desenvolvedoras Java/Spring júnior.
- Recrutadores técnicos que avaliam: organização do repositório, commits, branches,
  testes, documentação e boas práticas.

### 1.4 Escopo

**Dentro do escopo:**

- Cadastro e autenticação de usuários (JWT).
- CRUD de categorias e produtos.
- Carrinho de compras por usuário.
- Criação de pedidos (checkout) com baixa de estoque.
- Consulta de pedidos e atualização de status (admin).
- Paginação e filtros em listagens.
- Documentação OpenAPI (Swagger).
- Testes unitários e de integração.
- Docker (aplicação + banco de dados).
- README completo do projeto.

**Fora do escopo:**

- Pagamentos reais (gateway).
- Frete e cálculo de envio.
- Frontend.
- Microservices, mensageria (Kafka/RabbitMQ).
- Auditoria avançada e multi-tenancy.

---

## 2. Stack Tecnológica

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.x |
| Persistência | Spring Data JPA + Hibernate | - |
| Segurança | Spring Security + JWT (jjwt) | 0.12.x |
| Banco (produção) | PostgreSQL | 16 |
| Banco (testes) | H2 | - |
| Migrations | Flyway | 10.x |
| Documentação | springdoc-openapi (Swagger) | 2.x |
| Build | Maven | 3.9+ |
| Testes | JUnit 5, Mockito, Testcontainers | - |
| Validação | Bean Validation (jakarta.validation) | - |
| Container | Docker + Docker Compose | - |
| Qualidade | Checkstyle / Spotless (opcional) | - |

---

## 3. Requisitos Funcionais

### Autenticação

| ID | Requisito |
|---|---|
| RF-01 | Usuário pode se registrar com nome, e-mail e senha (senha mínima de 8 caracteres). |
| RF-02 | Usuário pode se autenticar com e-mail e senha, recebendo um token JWT de acesso. |
| RF-03 | Token JWT deve expirar em 2 horas. |
| RF-04 | E-mail deve ser único no sistema (retornar 409 em duplicidade). |

### Categorias

| ID | Requisito |
|---|---|
| RF-05 | Admin pode criar categoria (nome único, obrigatório). |
| RF-06 | Qualquer usuário autenticado pode listar categorias. |
| RF-07 | Admin pode atualizar e excluir categoria. |
| RF-08 | Não é permitido excluir categoria com produtos vinculados (retornar 409). |

### Produtos

| ID | Requisito |
|---|---|
| RF-09 | Admin pode criar produto (nome, descrição, preço, estoque, categoria). |
| RF-10 | Produto deve ter preço > 0 e estoque >= 0. |
| RF-11 | Qualquer usuário autenticado pode listar produtos com paginação (page/size/sort). |
| RF-12 | Listagem deve aceitar filtro por nome (LIKE) e por categoria. |
| RF-13 | Admin pode atualizar e excluir produto. |
| RF-14 | Admin pode ajustar quantidade de estoque (entrada/saída). |

### Carrinho

| ID | Requisito |
|---|---|
| RF-15 | Cada usuário possui um único carrinho, criado automaticamente no primeiro uso. |
| RF-16 | Usuário pode adicionar produto ao carrinho com quantidade. |
| RF-17 | Quantidade no carrinho não pode exceder o estoque disponível. |
| RF-18 | Usuário pode alterar quantidade e remover itens do carrinho. |
| RF-19 | Usuário pode consultar o carrinho com total calculado. |
| RF-20 | Carrinho deve impedir duplicidade: mesmo produto soma quantidade. |

### Pedidos

| ID | Requisito |
|---|---|
| RF-21 | Usuário pode criar pedido (checkout) a partir do carrinho. |
| RF-22 | Checkout baixa o estoque de cada item e limpa o carrinho. |
| RF-23 | Checkout falha (400) se carrinho vazio ou estoque insuficiente. |
| RF-24 | Pedido registra snapshot de preço de cada item no momento da compra. |
| RF-25 | Usuário lista seus pedidos com paginação. |
| RF-26 | Usuário consulta detalhe de um pedido próprio. |
| RF-27 | Admin lista todos os pedidos e atualiza status (PENDING → PAID → SHIPPED → DELIVERED / CANCELLED). |
| RF-28 | Cancelamento de pedido devolve os itens ao estoque. |

### Tratamento de erros (global)

| ID | Requisito |
|---|---|
| RF-29 | Toda resposta de erro segue o padrão JSON: `timestamp`, `status`, `error`, `message`, `path`. |
| RF-30 | Erros de validação de campos devem listar campo + mensagem (400). |
| RF-31 | Recursos não encontrados retornam 404; conflitos 409; acesso negado 403; não autenticado 401. |

---

## 4. Requisitos Não Funcionais

| ID | Requisito |
|---|---|
| RNF-01 | Arquitetura em camadas: Controller → Service → Repository (DTOs entre camadas, sem expor entidades). |
| RNF-02 | Senhas armazenadas com BCrypt (nunca em texto puro). |
| RNF-03 | Endpoints administrativos protegidos por role `ADMIN`. |
| RNF-04 | Cobertura de testes >= 70% na camada de serviço. |
| RNF-05 | API documentada com OpenAPI (Swagger UI em `/swagger-ui.html`). |
| RNF-06 | Aplicação containerizada com Docker Compose (api + postgres). |
| RNF-07 | Migrations versionadas com Flyway (sem `ddl-auto: create` em produção). |
| RNF-08 | Código em inglês (classes, métodos), documentação e commits em português ou inglês (padronizado). |
| RNF-09 | Respostas de listagem paginadas (page, size, totalElements, totalPages). |
| RNF-10 | Operações de checkout transacionais (rollback em caso de falha). |

---

## 5. Modelo de Dados

### 5.1 Entidades

| Entidade | Campos | Relacionamentos |
|---|---|---|
| `User` | id, name, email (unique), password, role (USER/ADMIN), createdAt | 1 → 1 Cart; 1 → N Order |
| `Category` | id, name (unique) | 1 → N Product |
| `Product` | id, name, description, price, stockQuantity, createdAt, updatedAt | N → 1 Category; 1 → N CartItem; 1 → N OrderItem |
| `Cart` | id, createdAt, updatedAt | 1 → 1 User; 1 → N CartItem |
| `CartItem` | id, quantity | N → 1 Cart; N → 1 Product |
| `Order` | id, status (enum), totalAmount, createdAt | N → 1 User; 1 → N OrderItem |
| `OrderItem` | id, productName (snapshot), unitPrice (snapshot), quantity, subtotal | N → 1 Order; N → 1 Product (referência) |

### 5.2 Regras de integridade

- `user.email` e `category.name` únicos (constraint + validação de negócio).
- `OrderItem` guarda nome e preço congelados no momento da compra (histórico imutável).
- Exclusão de produto com pedidos vinculados deve ser desencorajada (soft-delete opcional na Fase 5).

---

## 6. Endpoints REST

Base URL: `/api/v1`

### Autenticação

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| POST | `/auth/register` | Pública | Cadastrar usuário (retorna 201) |
| POST | `/auth/login` | Pública | Autenticar e obter JWT |

### Categorias

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| GET | `/categories` | Autenticado | Listar categorias |
| GET | `/categories/{id}` | Autenticado | Buscar categoria |
| POST | `/categories` | ADMIN | Criar categoria |
| PUT | `/categories/{id}` | ADMIN | Atualizar categoria |
| DELETE | `/categories/{id}` | ADMIN | Excluir categoria |

### Produtos

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| GET | `/products?page=&size=&sort=&name=&categoryId=` | Autenticado | Listar com paginação e filtros |
| GET | `/products/{id}` | Autenticado | Buscar produto |
| POST | `/products` | ADMIN | Criar produto |
| PUT | `/products/{id}` | ADMIN | Atualizar produto |
| DELETE | `/products/{id}` | ADMIN | Excluir produto |
| PATCH | `/products/{id}/stock` | ADMIN | Ajustar estoque (`{ "quantity": 10 }` entrada/saída) |

### Carrinho

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| GET | `/cart` | Autenticado | Consultar carrinho com itens e total |
| POST | `/cart/items` | Autenticado | Adicionar produto (`productId`, `quantity`) |
| PUT | `/cart/items/{itemId}` | Autenticado | Alterar quantidade |
| DELETE | `/cart/items/{itemId}` | Autenticado | Remover item |
| DELETE | `/cart` | Autenticado | Limpar carrinho |

### Pedidos

| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| POST | `/orders` | Autenticado | Checkout do carrinho |
| GET | `/orders?page=&size=` | Autenticado | Listar pedidos do usuário |
| GET | `/orders/{id}` | Autenticado (dono ou admin) | Detalhe do pedido |
| GET | `/admin/orders?page=&size=` | ADMIN | Listar todos os pedidos |
| PATCH | `/admin/orders/{id}/status` | ADMIN | Atualizar status (`PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED`) |

### Status de pedido (enum)

```
PENDING → PAID → SHIPPED → DELIVERED
PENDING/PAID → CANCELLED (devolve estoque)
```

---

## 7. Arquitetura

### 7.1 Estrutura de pacotes

```
com.github.igor.ecommerce
├── EcommerceApiApplication.java
├── config/          # SecurityConfig, OpenApiConfig, JwtConfig
├── controller/      # Endpoints REST (thin controllers)
├── service/         # Regras de negócio
│   └── impl/        # Implementações
├── repository/      # Spring Data JPA
├── entity/          # Entidades JPA
├── dto/             # Records/classes de entrada e saída
│   ├── request/
│   └── response/
├── mapper/          # Conversão entity ↔ dto
├── exception/       # Exceções customizadas + @RestControllerAdvice
├── security/        # JwtService, JwtAuthFilter, UserDetailsServiceImpl
└── enums/           # OrderStatus, Role
```

### 7.2 Regras de arquitetura

- Controllers não contêm regra de negócio; apenas delegam a Services.
- Entidades JPA nunca são expostas nas respostas (usar DTOs).
- Services usam interfaces + implementações (facilita testes com Mockito).
- Transações com `@Transactional` nas operações de escrita multi-tabela (checkout).
- Exceções de negócio customizadas: `NotFoundException` (404), `ConflictException` (409),
  `BusinessException` (400), `UnauthorizedException` (401), `ForbiddenException` (403).
- `@RestControllerAdvice` centraliza o tratamento de erros.
- Validações com Bean Validation (`@NotBlank`, `@Email`, `@Positive`, etc.).

---

## 8. Fases de Desenvolvimento

> Ordem recomendada: cada fase entrega um PR (Pull Request) mesclado em `develop`.

### Fase 1 — Setup do Projeto

**Objetivo:** projeto base rodando com banco, migração e healthcheck.

**Entregáveis:**

- Projeto Spring Boot gerado (Spring Initializr) com dependências base.
- `docker-compose.yml` com PostgreSQL 16.
- Flyway configurado + `V1__create_users_and_categories.sql`.
- `application.yml` e `application-test.yml` (H2).
- `GET /health` (Actuator) respondendo 200.

**Critérios de aceite:**

- `docker compose up -d` sobe o Postgres.
- Aplicação inicia e conecta no banco.
- `GET /health` retorna 200.
- README inicial criado.

**Branch:** `feature/fase-1-setup`

**Commits esperados:**

```
chore: initial spring boot project setup
chore: add docker-compose with postgresql
feat: configure flyway with initial migration
feat: add actuator health endpoint
docs: add initial readme
```

### Fase 2 — Domínio Categoria e Produto

**Objetivo:** primeiro CRUD completo, paginação e tratamento de erros.

**Entregáveis:**

- Entidades `Category` e `Product` + repositories.
- DTOs de request/response + mappers.
- Services e Controllers de categoria e produto.
- Paginação + filtros (nome, categoria) em produtos.
- `@RestControllerAdvice` com padrão de erro.
- Seed de admin e categorias via migration.

**Critérios de aceite:**

- CRUD completo via Swagger/Postman com validações.
- Filtros e paginação funcionando.
- Erros 404/400/409 no padrão JSON definido.

**Branch:** `feature/fase-2-catalog`

**Commits esperados:**

```
feat: add category entity and migration
feat: add category crud with validation
feat: add product entity and migration
feat: add product crud with pagination and filters
feat: add global exception handler with error pattern
test: add unit tests for category and product services
```

### Fase 3 — Autenticação e Autorização JWT

**Objetivo:** segurança completa com JWT e roles.

**Entregáveis:**

- Entidade `User` com BCrypt.
- `POST /auth/register` e `POST /auth/login`.
- `JwtService` (geração/validação), `JwtAuthFilter`.
- `SecurityConfig` com regras por rota e role.
- Proteção de endpoints de escrita como ADMIN.

**Critérios de aceite:**

- Login retorna JWT válido; token expira em 2h.
- Rotas protegidas rejeitam sem token (401) e sem role (403).
- E-mail duplicado retorna 409.
- Testes de unidade para o JwtService e filtro.

**Branch:** `feature/fase-3-auth`

**Commits esperados:**

```
feat: add user entity and repository
feat: add register and login endpoints
feat: add jwt service and authentication filter
feat: add security config with role-based rules
test: add tests for auth service and jwt
```

### Fase 4 — Carrinho de Compras

**Objetivo:** carrinho por usuário com regras de estoque.

**Entregáveis:**

- Entidades `Cart` e `CartItem`.
- Criação automática do carrinho no registro do usuário.
- Endpoints de carrinho (adicionar, alterar, remover, consultar).
- Validação de estoque na adição/alteração de quantidade.
- Total calculado do carrinho.

**Critérios de aceite:**

- Usuário vê apenas o próprio carrinho (isolamento por usuário).
- Produto repetido soma quantidade.
- Exceder estoque retorna 400.

**Branch:** `feature/fase-4-cart`

**Commits esperados:**

```
feat: add cart and cart item entities with migration
feat: create cart automatically on user registration
feat: add cart endpoints with stock validation
feat: prevent duplicate items by merging quantity
test: add unit tests for cart service
```

### Fase 5 — Pedidos e Estoque

**Objetivo:** fluxo de compra completo com transações e baixa de estoque.

**Entregáveis:**

- Entidades `Order` e `OrderItem` (snapshot de preço/nome).
- Checkout transacional (baixa estoque + limpa carrinho).
- Endpoints de pedido do usuário e do admin.
- Máquina de estados de status com cancelamento devolvendo estoque.
- Paginação na listagem de pedidos.

**Critérios de aceite:**

- Checkout com carrinho vazio retorna 400.
- Estoque insuficiente falha sem efeitos parciais (rollback).
- Cancelamento devolve estoque.
- Usuário não acessa pedido de outro usuário (403).

**Branch:** `feature/fase-5-orders`

**Commits esperados:**

```
feat: add order and order item entities with migration
feat: add transactional checkout with stock decrease
feat: add order endpoints for users and admin
feat: add order status workflow with stock return on cancel
test: add integration tests for checkout
```

### Fase 6 — Qualidade Final e Entrega

**Objetivo:** projeto pronto para apresentar a recrutadores.

**Entregáveis:**

- Swagger/OpenAPI configurado e revisado.
- Cobertura de testes >= 70% (unitários + integração com H2/Testcontainers).
- Dockerfile + compose final (api + postgres).
- GitHub Actions com build e testes.
- README completo: descrição, stack, como rodar, endpoints, prints.
- Revisão de commit history e merge em `main`.

**Critérios de aceite:**

- `mvn clean verify` passa com sucesso.
- Subir tudo apenas com `docker compose up`.
- README permite reproduzir o projeto do zero.
- Histórico de commits limpo e semanticamente organizado.

**Branch:** `feature/fase-6-polish`

**Commits esperados:**

```
feat: add openapi documentation
test: increase test coverage for services
feat: add dockerfile and compose for the api
ci: add github actions workflow
docs: complete readme with setup instructions
refactor: code review cleanup
```

---

## 9. Estratégia de Branches

Modelo Gitflow simplificado:

```
main       ← apenas releases estáveis (merge de develop)
develop    ← integração das fases
feature/*  ← uma branch por fase
```

### Regras

- `main` nunca recebe commit direto.
- Cada fase começa em `develop` e volta para `develop` via Pull Request.
- Commits atômicos: uma mudança lógica por commit.
- Nunca commitar secrets (`application.yml` de produção, chaves JWT).

### Comandos por fase

```bash
git checkout develop
git pull origin develop
git checkout -b feature/fase-N-nome

# ... commits da fase ...

git checkout develop
git pull origin develop
git merge --no-ff feature/fase-N-nome
git push origin develop
```

Ao final de todas as fases:

```bash
git checkout main
git merge --no-ff develop
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin main --tags
```

---

## 10. Convenção de Commits

Padrão **Conventional Commits** (em inglês, coerente com o código):

| Tipo | Uso |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `docs` | Documentação |
| `test` | Testes |
| `refactor` | Refatoração sem mudança de comportamento |
| `chore` | Tarefas de build/config |
| `ci` | Integração contínua |
| `style` | Formatação |

Formato:

```
tipo(escopo): descrição curta em inglês, imperativo, max 50 chars

Corpo opcional explicando o porquê, max 72 chars por linha
```

Exemplos:

```
feat(auth): add jwt token generation
fix(cart): prevent duplicate items on add
test(order): cover checkout rollback scenario
refactor(product): extract stock logic to service
```

---

## 11. Definition of Done (por funcionalidade)

Uma funcionalidade só é considerada concluída quando:

- [ ] Implementada seguindo a arquitetura em camadas.
- [ ] Validações de entrada e de negócio aplicadas.
- [ ] Erros tratados pelo handler global no padrão JSON.
- [ ] Testes unitários (e de integração quando couber) escritos e passando.
- [ ] `mvn clean verify` executa sem falhas.
- [ ] Endpoint testado manualmente (Postman/Swagger).
- [ ] Commits seguem a convenção e o PR foi revisado antes do merge.

---

## 12. Roadmap Resumo

| Fase | Conteúdo | Branch | Situação |
|---|---|---|---|
| 1 | Setup + Docker + Flyway + Health | `feature/fase-1-setup` | Não iniciada |
| 2 | CRUD Categoria/Produto | `feature/fase-2-catalog` | Não iniciada |
| 3 | Auth JWT + Roles | `feature/fase-3-auth` | Não iniciada |
| 4 | Carrinho | `feature/fase-4-cart` | Não iniciada |
| 5 | Pedidos + Estoque | `feature/fase-5-orders` | Não iniciada |
| 6 | Qualidade + Entrega | `feature/fase-6-polish` | Não iniciada |
