# Equipe de Agentes — ecommerce-api

Este projeto é desenvolvido por uma equipe de agentes especialistas (definidos
em `.opencode/agent/`). Cada sprint do [sprints.md](sprints.md) tem um agente
responsável que conduz a execução.

## Regra geral

> Sempre que for executar uma sprint, deixe o agente especialista responsável
> por ela conduzir o trabalho (despachado via Task tool). O `tech-lead`
> orquestra, revisa e faz o merge.

## Time

| Agente | Especialidade | Quando usar |
|---|---|---|
| `tech-lead` | Orquestração, revisão, merges, releases | Em todas as sprints; lidera a sprint 6 |
| `backend-dev` | Java 21, Spring Boot, JPA, DTOs, regras de negócio | CRUDs, entidades, services, controllers |
| `security-engineer` | Spring Security, JWT, BCrypt, roles | Autenticação e autorização |
| `qa-engineer` | JUnit, Mockito, MockMvc, cobertura | Testes de toda sprint; lidera testes na sprint 6 |
| `devops-engineer` | Docker, Compose, GitHub Actions, CI | Infra e entrega; lidera Docker/CI na sprint 6 |

## Mapeamento Sprint → Agente responsável

| Sprint | Conteúdo | Agente responsável | Apoio |
|---|---|---|---|
| 1 | Setup do projeto | `devops-engineer` (concluída) | `backend-dev` |
| 2 | CRUD Categoria e Produto | `backend-dev` | `qa-engineer` |
| 3 | Autenticação e Autorização JWT | `security-engineer` | `qa-engineer` |
| 4 | Carrinho de Compras | `backend-dev` | `qa-engineer` |
| 5 | Pedidos e Estoque | `backend-dev` | `qa-engineer` |
| 6 | Qualidade Final e Entrega | `tech-lead` | `qa-engineer`, `devops-engineer` |

## Fluxo de trabalho por sprint

1. `tech-lead` cria a branch `feature/fase-N-nome` a partir de `develop`.
2. O agente responsável implementa seguindo o PRD e o sprints.md.
3. `qa-engineer` escreve/revisa os testes e roda o build:
   `JAVA_HOME=/home/igor/.jdks/ms-21.0.12.1 ./mvnw clean verify`
4. `tech-lead` revisa (Definition of Done), faz o merge `--no-ff` em `develop`
   e marca os itens concluídos com `X` no sprints.md.

> Nota: para carregar novos agentes é necessário reiniciar o opencode.
