# Sprints — E-commerce API

Plano de desenvolvimento dividido em sprints. Cada sprint corresponde a uma fase do
[PRD.md](PRD.md). Marque com `X` os itens conforme forem concluídos.

---

## Sprint 1 — Setup do Projeto

**Branch:** `feature/fase-1-setup` | **Status:** ✅ Concluída

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

**Branch:** `feature/fase-2-catalog` | **Status:** ⬜ Não iniciada

**Objetivo:** primeiro CRUD completo, paginação e tratamento de erros.

- [ ] Entidades `Category` e `Product` + repositories
- [ ] DTOs de request/response + mappers
- [ ] Services (interface + implementação) de categoria e produto
- [ ] Controllers REST de categoria e produto
- [ ] Paginação e filtros (nome, categoria) em produtos
- [ ] `@RestControllerAdvice` com padrão de erro JSON
- [ ] Seed de admin e categorias via migration (`V2`)
- [ ] Validações Bean Validation nos requests
- [ ] Testes unitários das services de categoria e produto
- [ ] Testes de integração dos endpoints (200/201/400/404/409)
- [ ] `mvn clean verify` passando
- [ ] Merge da branch em `develop`

---

## Sprint 3 — Autenticação e Autorização JWT

**Branch:** `feature/fase-3-auth` | **Status:** ⬜ Não iniciada

**Objetivo:** segurança completa com JWT e roles.

- [ ] Entidade `User` + repository
- [ ] `POST /auth/register` com senha BCrypt
- [ ] `POST /auth/login` retornando JWT (expiração 2h)
- [ ] `JwtService` (geração/validação de token)
- [ ] `JwtAuthFilter` + `UserDetailsServiceImpl`
- [ ] `SecurityConfig` com regras por rota e role
- [ ] Endpoints de escrita protegidos como `ADMIN`
- [ ] E-mail duplicado retornando 409
- [ ] Testes unitários de auth service e jwt
- [ ] Testes de integração (401 sem token / 403 sem role)
- [ ] `mvn clean verify` passando
- [ ] Merge da branch em `develop`

---

## Sprint 4 — Carrinho de Compras

**Branch:** `feature/fase-4-cart` | **Status:** ⬜ Não iniciada

**Objetivo:** carrinho por usuário com regras de estoque.

- [ ] Entidades `Cart` e `CartItem` + migration (`V4`)
- [ ] Criação automática do carrinho no registro do usuário
- [ ] `GET /cart` com itens e total calculado
- [ ] `POST /cart/items` adicionando produto
- [ ] `PUT /cart/items/{itemId}` alterando quantidade
- [ ] `DELETE /cart/items/{itemId}` removendo item
- [ ] `DELETE /cart` limpando carrinho
- [ ] Produto repetido soma quantidade (sem duplicidade)
- [ ] Validação de estoque na adição/alteração (400 se exceder)
- [ ] Isolamento: usuário vê apenas o próprio carrinho
- [ ] Testes unitários da service de carrinho
- [ ] `mvn clean verify` passando
- [ ] Merge da branch em `develop`

---

## Sprint 5 — Pedidos e Estoque

**Branch:** `feature/fase-5-orders` | **Status:** ⬜ Não iniciada

**Objetivo:** fluxo de compra completo com transações e baixa de estoque.

- [ ] Entidades `Order` e `OrderItem` (snapshot de preço/nome) + migration (`V5`)
- [ ] `POST /orders` checkout transacional (baixa estoque + limpa carrinho)
- [ ] Checkout com carrinho vazio retornando 400
- [ ] Estoque insuficiente com rollback total
- [ ] `GET /orders` listando pedidos do usuário (paginado)
- [ ] `GET /orders/{id}` detalhe (dono ou admin)
- [ ] `GET /admin/orders` listando todos (admin)
- [ ] `PATCH /admin/orders/{id}/status` atualizando status
- [ ] Cancelamento devolvendo itens ao estoque
- [ ] Usuário não acessa pedido alheio (403)
- [ ] Testes de integração do checkout (sucesso, falha, rollback)
- [ ] `mvn clean verify` passando
- [ ] Merge da branch em `develop`

---

## Sprint 6 — Qualidade Final e Entrega

**Branch:** `feature/fase-6-polish` | **Status:** ⬜ Não iniciada

**Objetivo:** projeto pronto para apresentar a recrutadores.

- [ ] OpenAPI/Swagger configurado (`/swagger-ui.html`)
- [ ] Cobertura de testes >= 70% nas services
- [ ] `Dockerfile` da aplicação + compose final (api + postgres)
- [ ] GitHub Actions com build e testes
- [ ] README completo (stack, como rodar, endpoints, prints)
- [ ] Revisão geral do código e commit history
- [ ] `mvn clean verify` passando
- [ ] Merge em `develop` e release `v1.0.0` em `main`

---

## Regras Gerais

- Cada sprint começa em `develop` e volta para `develop` via merge `--no-ff`.
- Commits seguem o padrão Conventional Commits definido no PRD.
- Um sprint só é considerado concluído quando todos os itens estiverem marcados com `X`.
