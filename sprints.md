# Sprints — E-commerce API

Plano de desenvolvimento dividido em sprints. Cada sprint corresponde a uma fase do
[PRD.md](PRD.md). Marque com `X` os itens conforme forem concluídos.

---

## Sprint 1 — Setup do Projeto

**Branch:** `feature/fase-1-setup` | **Status:** ✅ Concluída | **Agente responsável:** `devops-engineer`

**Objetivo:** projeto base rodando com banco, migração e healthcheck.

- [x] Projeto Spring Boot 3.4 + Java 21 com dependências base
- [x] Maven Wrapper incluído no repositório
- [x] `docker-compose.yml` com PostgreSQL 16
- [x] Flyway configurado
- [x] `V1__create_users_and_categories.sql` criada e aplicada
- [x] `application.yml` (Postgres) e `application-test.yml` (H2) configurados
- [x] `GET /actuator/health` respondendo 200
- [x] README inicial criado
- [x] `mvn clean verify` passando
- [x] Merge da branch em `develop`

---

## Sprint 2 — Domínio Categoria e Produto

**Branch:** `feature/fase-2-catalog` | **Status:** ✅ Concluída | **Agente responsável:** `backend-dev`

**Objetivo:** primeiro CRUD completo, paginação e tratamento de erros.

- [x] Entidades `Category` e `Product` + repositories
- [x] DTOs de request/response + mappers
- [x] Services (interface + implementação) de categoria e produto
- [x] Controllers REST de categoria e produto
- [x] Paginação e filtros (nome, categoria) em produtos
- [x] `@RestControllerAdvice` com padrão de erro JSON
- [x] Seed de admin e categorias via migration (`V2`)
- [x] Validações Bean Validation nos requests
- [x] Testes unitários das services de categoria e produto
- [x] Testes de integração dos endpoints (200/201/400/404/409)
- [x] `mvn clean verify` passando
- [x] Merge da branch em `develop`

---

## Sprint 3 — Autenticação e Autorização JWT

**Branch:** `feature/fase-3-auth` | **Status:** ✅ Concluída | **Agente responsável:** `security-engineer`

**Objetivo:** segurança completa com JWT e roles.

- [x] Entidade `User` + repository
- [x] `POST /auth/register` com senha BCrypt
- [x] `POST /auth/login` retornando JWT (expiração 2h)
- [x] `JwtService` (geração/validação de token)
- [x] `JwtAuthFilter` + `UserDetailsServiceImpl`
- [x] `SecurityConfig` com regras por rota e role
- [x] Endpoints de escrita protegidos como `ADMIN`
- [x] E-mail duplicado retornando 409
- [x] Testes unitários de auth service e jwt
- [x] Testes de integração (401 sem token / 403 sem role)
- [x] `mvn clean verify` passando
- [x] Merge da branch em `develop`

---

## Sprint 4 — Carrinho de Compras

**Branch:** `feature/fase-4-cart` | **Status:** ✅ Concluída | **Agente responsável:** `backend-dev`

**Objetivo:** carrinho por usuário com regras de estoque.

- [x] Entidades `Cart` e `CartItem` + migration (`V4`)
- [x] Criação automática do carrinho no registro do usuário
- [x] `GET /cart` com itens e total calculado
- [x] `POST /cart/items` adicionando produto
- [x] `PUT /cart/items/{itemId}` alterando quantidade
- [x] `DELETE /cart/items/{itemId}` removendo item
- [x] `DELETE /cart` limpando carrinho
- [x] Produto repetido soma quantidade (sem duplicidade)
- [x] Validação de estoque na adição/alteração (400 se exceder)
- [x] Isolamento: usuário vê apenas o próprio carrinho
- [x] Testes unitários da service de carrinho
- [x] `mvn clean verify` passando
- [x] Merge da branch em `develop`

---

## Sprint 5 — Pedidos e Estoque

**Branch:** `feature/fase-5-orders` | **Status:** ✅ Concluída | **Agente responsável:** `backend-dev`

**Objetivo:** fluxo de compra completo com transações e baixa de estoque.

- [x] Entidades `Order` e `OrderItem` (snapshot de preço/nome) + migration (`V5`)
- [x] `POST /orders` checkout transacional (baixa estoque + limpa carrinho)
- [x] Checkout com carrinho vazio retornando 400
- [x] Estoque insuficiente com rollback total
- [x] `GET /orders` listando pedidos do usuário (paginado)
- [x] `GET /orders/{id}` detalhe (dono ou admin)
- [x] `GET /admin/orders` listando todos (admin)
- [x] `PATCH /admin/orders/{id}/status` atualizando status
- [x] Cancelamento devolvendo itens ao estoque
- [x] Usuário não acessa pedido alheio (403)
- [x] Testes de integração do checkout (sucesso, falha, rollback)
- [x] `mvn clean verify` passando
- [x] Merge da branch em `develop`

---

## Sprint 6 — Qualidade Final e Entrega

**Branch:** `feature/fase-6-polish` | **Status:** ✅ Concluída | **Agente responsável:** `tech-lead`

**Objetivo:** projeto pronto para apresentar a recrutadores.

- [x] OpenAPI/Swagger configurado (`/swagger-ui.html`)
- [x] Cobertura de testes >= 70% nas services
- [x] `Dockerfile` da aplicação + compose final (api + postgres)
- [x] GitHub Actions com build e testes
- [x] README completo (stack, como rodar, endpoints, prints)
- [x] Revisão geral do código e commit history
- [x] `mvn clean verify` passando
- [x] Merge em `develop` e release `v1.0.0` em `main`

---

## Regras Gerais

- Cada sprint começa em `develop` e volta para `develop` via merge `--no-ff`.
- Commits seguem o padrão Conventional Commits definido no PRD.
- Um sprint só é considerado concluído quando todos os itens estiverem marcados com `X`.
