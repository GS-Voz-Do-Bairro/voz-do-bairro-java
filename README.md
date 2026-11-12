# Voz do Bairro — API Java (Spring Boot)

Global Solution FIAP 2026/2 — tema **CitizenTech** — disciplina **Java Advanced**.

## Integrantes

| Nome | RM | Turma |
|------|----|-------|
| Andrei de Paiva Gibbini | 563061 | 2TDSPF |
| Arthur Câmara | 562310 | 2TDSPG |
| Diogo Cunha | 563654 | 2TDSPF |
| Pedro Sakai Silva Zambaca | 565956 | 2TDSPF |
| Pedro Santos Pequini | 561842 | 2TDSPF |

Responsável por esta disciplina: **Arthur Câmara (RM 562310)**.

---

## 1. Problema e solução

Moradores de São Paulo convivem com problemas de zeladoria, mobilidade, iluminação e segurança. As reclamações se perdem em canais dispersos e não há transparência sobre **se** e **quando** a prefeitura responde.

O **Voz do Bairro** organiza essa participação:

1. O morador **propõe** uma melhoria para o seu bairro.
2. A comunidade **apoia** (API .NET). Ao atingir a meta de apoios, a proposta vira **DEMANDA** e a prefeitura passa a ter **15 dias** para responder.
3. O **gestor público** responde (aceita com prazo de execução ou recusa com justificativa) e depois marca como concluída.
4. Se o prazo vence sem resposta, a demanda fica **ATRASADA** automaticamente.
5. Tudo é **público** num placar por bairro (contagem por status e % de demandas respondidas no prazo).
6. **IA generativa (Spring AI)** gera um resumo neutro de cada proposta e da discussão e ajuda o gestor com um rascunho de parecer.

ODS relacionados: 11 (cidades sustentáveis), 10 (redução das desigualdades) e 16 (instituições eficazes e transparentes).

Esta API Java é dona de: autenticação/cadastro, usuários, propostas, respostas, notificações, placar, Spring AI e RabbitMQ. A API .NET é dona de bairros (CRUD), comentários e apoios. As duas usam o **mesmo banco Oracle** e o **mesmo JWT** (contrato em `CONTRATO.md` do grupo). Esta é a API implantada na Azure (disciplina de DevOps).

## 2. Arquitetura

```mermaid
flowchart LR
    APP["App Mobile<br/>(React Native)"]

    subgraph JAVA["API Java - Spring Boot :8080"]
        JC["Controllers REST<br/>+ Spring Security JWT"]
        JS["Services<br/>(regras de status)"]
        JAI["Spring AI<br/>ChatClient"]
        JPUB["Publisher<br/>(após commit)"]
        JCON["Consumidores<br/>@RabbitListener"]
        JAG["Agendador<br/>@Scheduled"]
    end

    subgraph NET["API .NET - Minimal API :5080"]
        NC["Bairros, comentários,<br/>apoios, login"]
    end

    DB[("Oracle 19c<br/>VB_*")]
    MQ{{"RabbitMQ<br/>exchange topic<br/>vozdobairro.events"}}
    LLM["LLM compatível<br/>com OpenAI<br/>(OpenAI / Groq)"]

    APP -->|HTTPS + Bearer JWT| JC
    APP -->|HTTPS + Bearer JWT| NC
    JC --> JS --> DB
    NC --> DB
    JS -.->|proposta.criada| JPUB
    JAG -.->|proposta.prazo-vencido| JPUB
    JPUB --> MQ
    NC -->|proposta.meta-atingida| MQ
    MQ --> JCON
    JCON --> JS
    JCON --> JAI
    JC --> JAI
    JAI -->|HTTP| LLM
```

Camadas do código (`br.com.fiap.vozdobairro`):

| Pacote | Responsabilidade |
|---|---|
| `controller` | Endpoints REST, validação de entrada (`@Valid`), `@PreAuthorize` por papel |
| `service` | Regras de negócio e transações (transições de status, permissões de autor) |
| `repository` | Spring Data JPA (paginação, consultas JPQL, updates atômicos) |
| `domain` | Entidades JPA mapeando as tabelas `VB_*` e enums |
| `dto` / `mapper` | Records de entrada/saída e conversão entidade → DTO |
| `security` | JWT HS256 (Nimbus), conversor de papéis, 401/403 em Problem Details |
| `messaging` | Publicação e consumo de eventos RabbitMQ, agendador de prazos |
| `ai` | Integração com LLM via Spring AI (com fallback) |
| `exception` | `@RestControllerAdvice` com respostas Problem Details (RFC 7807) |
| `config` | RabbitMQ, OpenAPI, Clock/Async/Scheduling |

### Entidades (tabelas do contrato)

`Usuario` (VB_USUARIO), `Proposta` (VB_PROPOSTA), `Resposta` (VB_RESPOSTA), `Notificacao` (VB_NOTIFICACAO) com escrita; `Bairro` (VB_BAIRRO), `Comentario` (VB_COMENTARIO) e `Apoio` (VB_APOIO) somente leitura (`@Immutable`, repositórios sem métodos de escrita).
Relacionamentos: Proposta → Usuario (autor) e → Bairro; Resposta → Proposta e → Usuario (gestor); Notificacao → Usuario e → Proposta; Comentario/Apoio → Proposta.

## 3. Requisito 5 — Arquitetura distribuída: opção **A (RabbitMQ)**

Exchange `vozdobairro.events` (topic, durável). Mensagens JSON UTF-8 puras (`content_type=application/json`, persistentes, sem cabeçalhos de tipo do Spring), o que permite interoperar com o .NET.

| Evento (routing key) | Quem publica | Fila consumida pelo Java | O que acontece |
|---|---|---|---|
| `proposta.criada` | Java, **após o commit** da criação (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`) | `vb.proposta.criada.resumo-ia` | O consumidor chama o LLM (Spring AI) e grava `RESUMO_IA`. A criação da proposta responde imediatamente, sem esperar a IA. |
| `proposta.meta-atingida` | .NET (quando o apoio faz `TOTAL_APOIOS >= META_APOIOS`) | `vb.proposta.meta-atingida` | Notifica autor, apoiadores e todos os gestores. |
| `proposta.prazo-vencido` | Java (agendador `@Scheduled`, padrão a cada 10 min) | `vb.proposta.prazo-vencido` | `DEMANDA → ATRASADA` e notifica autor e gestores. |

Garantias:
- **Idempotência**: `proposta.criada` não refaz resumo já existente; `meta-atingida` não notifica de novo se já existe notificação desse tipo para a proposta; `prazo-vencido` usa um `UPDATE ... WHERE STATUS='DEMANDA' AND PRAZO_RESPOSTA < agora AND sem resposta` (reprocessar não muda nada).
- **Resiliência**: se o RabbitMQ estiver fora do ar, a publicação só registra erro em log; a requisição HTTP não falha. Mensagens com JSON inválido são descartadas com log; falhas de infraestrutura têm 3 tentativas.

## 4. Spring AI (requisito 4)

A IA está **integrada ao fluxo**, não é um chat solto:

1. **Resumo neutro automático** — ao criar uma proposta, o consumidor de `proposta.criada` envia título, descrição, categoria, bairro, apoios e os comentários existentes ao LLM, pedindo um resumo **neutro**, em PT-BR, de 2–3 frases, sem opinião. O texto (máx. 1000 caracteres) vai para `RESUMO_IA` e aparece no detalhe da proposta. O gestor pode regenerar com `POST /api/propostas/{id}/resumo-ia` (útil depois que a discussão cresce).
2. **Rascunho de parecer para o gestor** — `POST /api/propostas/{id}/resposta/rascunho-ia` devolve uma saída **estruturada** obtida com `ChatClient...call().entity(ParecerIa.class)`: o Spring AI gera o JSON Schema do record `ParecerIa(sugestaoDecisao, justificativa, pontosDeAtencao)`, anexa ao prompt e converte a resposta para o record. Nada é salvo: o gestor revisa e envia pelo `POST /resposta`.
3. **Fallback** — sem chave configurada, com a IA desligada ou se a chamada falhar, a API não quebra: gera um resumo simples (trecho da descrição) e um rascunho-modelo, com `geradoPorIa=false`, e registra log. **A proposta nunca falha por causa da IA.**

Proteções: o conteúdo dos usuários vai delimitado e o prompt de sistema manda ignorar instruções dentro dele (mitigação de prompt injection); nome/e-mail do autor **não** são enviados ao LLM.

**Funciona com qualquer API compatível com OpenAI.** Exemplos:

| Provedor | `OPENAI_BASE_URL` | `OPENAI_MODEL` |
|---|---|---|
| OpenAI | `https://api.openai.com` (padrão) | `gpt-4o-mini` (padrão) |
| Groq (tem plano gratuito) | `https://api.groq.com/openai` | `llama-3.3-70b-versatile` |

## 5. Como rodar

### Pré-requisitos

- **JDK 21** (`java -version`). Não precisa instalar o Maven: use o wrapper `mvnw`.
- **Docker** (para o RabbitMQ local).
- Acesso ao Oracle da FIAP (o schema vem do script `banco/script-bd-base.sql`).

### 5.1 Banco Oracle

Rode uma única vez o script `banco/script-bd-base.sql` no seu schema Oracle (SQL Developer → abrir o arquivo → *Executar Script* / F5). Ele cria as tabelas `VB_*` e os usuários de teste. A API **não** cria tabelas (`ddl-auto=none`).

### 5.2 RabbitMQ local

```bash
docker compose up -d
```
Painel em http://localhost:15672 (guest/guest). A exchange e as 3 filas são criadas automaticamente pela API na primeira conexão.

### 5.3 Variáveis de ambiente e execução

| Variável | Obrigatória | Exemplo / padrão |
|---|---|---|
| `DB_URL` | sim | `jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL` |
| `DB_USER` | sim | `rm000000` |
| `DB_PASSWORD` | sim | sua senha |
| `JWT_SECRET` | sim | mínimo **32 caracteres**; **o mesmo valor da API .NET** |
| `RABBITMQ_URL` | não | `amqp://guest:guest@localhost:5672` (CloudAMQP: `amqps://user:pass@host/vhost`) |
| `OPENAI_API_KEY` | não | sem ela, a IA usa o fallback |
| `OPENAI_BASE_URL` | não | `https://api.openai.com` |
| `OPENAI_MODEL` | não | `gpt-4o-mini` |
| `PRAZO_CRON` | não | `0 */10 * * * *` (agendador de prazos) |
| `CORS_ALLOWED_ORIGINS` | não | `*` |
| `PORT` | não | `8080` |

Linux / macOS / Git Bash:
```bash
export DB_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL"
export DB_USER="rm000000"
export DB_PASSWORD="sua-senha"
export JWT_SECRET="um-segredo-compartilhado-com-pelo-menos-32-chars"
export OPENAI_API_KEY="sk-..."      # opcional
./mvnw spring-boot:run
```

Windows (PowerShell):
```powershell
$env:DB_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL"
$env:DB_USER="rm000000"
$env:DB_PASSWORD="sua-senha"
$env:JWT_SECRET="um-segredo-compartilhado-com-pelo-menos-32-chars"
$env:OPENAI_API_KEY="sk-..."        # opcional
.\mvnw.cmd spring-boot:run
```

> Se `./mvnw` der "permission denied": `chmod +x mvnw`.
> Não defina `OPENAI_API_KEY` vazia por engano; se não tiver chave, simplesmente não defina a variável (mesmo vazia a API sobe, mas fica sem IA).

API em http://localhost:8080 — Swagger em **http://localhost:8080/swagger-ui.html** — health em http://localhost:8080/actuator/health.

### 5.4 Testes

```bash
./mvnw test
```
Os testes usam o perfil `test` (H2 em modo Oracle, schema criado pelo Hibernate, RabbitTemplate mockado, IA desligada). Não precisam de Oracle, RabbitMQ nem chave de LLM.

### 5.5 Docker

```bash
docker build -f dockerfiles/Dockerfile.java -t voz-do-bairro-java .
docker run --rm -p 8080:8080 \
  -e DB_URL=... -e DB_USER=... -e DB_PASSWORD=... -e JWT_SECRET=... \
  -e RABBITMQ_URL=amqp://guest:guest@host.docker.internal:5672 \
  voz-do-bairro-java
```

## 6. Usuários de teste

Senha de todos: **`Senha@123`**

| E-mail | Papel | Id |
|---|---|---|
| `cidadao@vozdobairro.com` | CIDADAO | 1 |
| `vizinho@vozdobairro.com` | CIDADAO | 2 |
| `gestor@vozdobairro.com` | GESTOR | 3 |

O cadastro público (`/api/auth/register`) sempre cria CIDADAO; gestores vêm do banco.

## 7. Endpoints principais

Todos sob `/api`. "Autenticado" = qualquer papel com token válido.

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| POST | `/auth/register` | público | Cadastra cidadão e devolve token |
| POST | `/auth/login` | público | Login, devolve token (2h) |
| GET | `/bairros` | público | Lista bairros |
| GET | `/bairros/{id}/placar` | público | Propostas por status e % respondidas no prazo |
| GET | `/usuarios/me` | autenticado | Meus dados |
| PUT | `/usuarios/me` | autenticado | Atualiza nome/bairro |
| GET | `/usuarios` | GESTOR | Lista usuários (paginado) |
| GET | `/propostas?bairroId=&status=&categoria=&page=&size=&sort=` | autenticado | Lista paginada com filtros |
| GET | `/propostas/minhas` | autenticado | Minhas propostas (paginado) |
| GET | `/propostas/{id}` | autenticado | Detalhe com resumo IA, resposta e total de comentários |
| POST | `/propostas` | CIDADAO | Cria proposta (ABERTA, meta do bairro) |
| PUT | `/propostas/{id}` | autor | Edita (só ABERTA) |
| DELETE | `/propostas/{id}` | autor / GESTOR | Autor: só ABERTA sem apoios. Gestor: qualquer |
| PATCH | `/propostas/{id}/concluir` | GESTOR | EM_EXECUCAO → CONCLUIDA |
| POST | `/propostas/{id}/resumo-ia` | GESTOR | Regenera resumo com IA (síncrono) |
| GET | `/propostas/{id}/resposta` | autenticado | Resposta do gestor |
| POST | `/propostas/{id}/resposta` | GESTOR | Responde DEMANDA/ATRASADA |
| PUT | `/propostas/{id}/resposta` | GESTOR | Edita justificativa/prazo |
| POST | `/propostas/{id}/resposta/rascunho-ia` | GESTOR | Rascunho de parecer com IA (não salva) |
| GET | `/notificacoes` | autenticado | Minhas notificações (paginado) |
| PATCH | `/notificacoes/{id}/lida` | dono | Marca como lida |

Paginação: `page` (começa em 0), `size` (máx. 100) e `sort` (ex.: `sort=dtCriacao,desc`). Resposta paginada:
```json
{ "conteudo": [ ... ], "pagina": 0, "tamanho": 10, "totalElementos": 42, "totalPaginas": 5, "primeira": true, "ultima": false }
```

## 8. Exemplos de JSON

**Login** — `POST /api/auth/login`
```json
{ "email": "cidadao@vozdobairro.com", "senha": "Senha@123" }
```
Resposta `200`:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "expiraEm": "2026-11-20T15:00:00Z",
  "usuario": { "id": 1, "nome": "Maria Cidadã", "email": "cidadao@vozdobairro.com", "papel": "CIDADAO", "bairroId": 2 } }
```

**Criar proposta** — `POST /api/propostas` (Bearer de CIDADAO)
```json
{ "titulo": "Iluminação na Rua Agnaldo Macedo",
  "descricao": "A rua fica totalmente escura à noite entre os números 100 e 400.",
  "categoria": "SEGURANCA", "bairroId": 2 }
```
Resposta `201` (o `resumoIa` é preenchido segundos depois, de forma assíncrona):
```json
{ "id": 10, "titulo": "Iluminação na Rua Agnaldo Macedo", "descricao": "...", "categoria": "SEGURANCA",
  "status": "ABERTA", "resumoIa": null, "metaApoios": 30, "totalApoios": 0, "autorId": 1, "autorNome": "Maria Cidadã",
  "bairroId": 2, "bairroNome": "Capão Redondo", "dtCriacao": "2026-11-12T13:00:00Z", "dtVirouDemanda": null, "prazoResposta": null }
```

**Responder demanda** — `POST /api/propostas/10/resposta` (Bearer de GESTOR)
```json
{ "decisao": "ACEITA", "justificativa": "A troca das lâmpadas foi incluída no cronograma da subprefeitura.", "prazoExecucao": "2026-12-20" }
```

**Rascunho IA** — `POST /api/propostas/10/resposta/rascunho-ia`
```json
{ "propostaId": 10, "sugestaoDecisao": "ACEITA",
  "justificativa": "A demanda dos moradores do Capão Redondo sobre a iluminação...",
  "pontosDeAtencao": ["Confirmar com a Ilume a viabilidade técnica", "Verificar orçamento do trimestre"],
  "geradoPorIa": true, "aviso": "Rascunho gerado por IA. Revise antes de usar: a decisão é do gestor." }
```

**Erro de validação** — `400`, `application/problem+json`
```json
{ "type": "about:blank", "title": "Requisição inválida", "status": 400,
  "detail": "Um ou mais campos são inválidos.", "instance": "/api/propostas",
  "errors": { "titulo": ["não pode ser vazio"], "categoria": ["é obrigatório"] } }
```
Outros códigos: `401` sem token/token inválido, `403` papel ou dono incorreto, `404` não encontrado, `409` conflito (e-mail já cadastrado, proposta já respondida), `422` regra de negócio (ex.: editar proposta que já virou DEMANDA).

## 9. Decisões técnicas

- **JWT sem jjwt**: emissão com `NimbusJwtEncoder` + `ImmutableSecret` e validação com `NimbusJwtDecoder.withSecretKey` (HS256), via `spring-boot-starter-oauth2-resource-server`. Claim `role` vira `ROLE_CIDADAO`/`ROLE_GESTOR` (`JwtGrantedAuthoritiesConverter`). Valida `iss=voz-do-bairro` e expiração. Esta é a única API que emite tokens; a API .NET só os valida (mesmo segredo e claims).
- **Senhas** com BCrypt `$2a$` custo 10.
- **Escrita concorrente com o .NET**: `Proposta` usa `@DynamicUpdate` e as colunas que o .NET mantém (`TOTAL_APOIOS`, `DT_VIROU_DEMANDA`, `PRAZO_RESPOSTA`) são somente leitura no Java; `RESUMO_IA` e `ATRASADA` são gravados por `UPDATE` pontual. Assim o Java nunca sobrescreve um apoio registrado pelo .NET.
- **Datas** gravadas em UTC; a API devolve ISO-8601 com `Z`.
- **`LIDA NUMBER(1)`** mapeado com `AttributeConverter<Boolean, Integer>` (funciona igual no Oracle e no H2).
- **Tipo da notificação** codificado como prefixo da mensagem (ex.: `[Meta atingida] ...`), já que `VB_NOTIFICACAO` não tem coluna de tipo; isso viabiliza a idempotência sem alterar o schema.
- **Paginação** devolvida num envelope próprio (`PaginaResponse`) estável para o app Mobile.
- **CORS** liberado (sem cookies; autenticação por header).
- **Health** não depende do RabbitMQ (`management.health.rabbit.enabled=false`), para que a indisponibilidade do broker não derrube a aplicação no deploy.

## 10. Declaração de uso de IA generativa

Ferramentas de IA generativa foram usadas como apoio no desenvolvimento deste projeto. Todo o código foi lido, revisado, compilado e testado pelo responsável, que responde pelo conteúdo entregue.

**Ferramentas utilizadas:** Claude (Anthropic), pelo chat e pelo Claude Code.

**Partes em que foram usadas:**

- Discussão da ideia da solução e da divisão entre as disciplinas
- Geração inicial da estrutura do projeto e de trechos de código
- Revisão, correção de erros de compilação e criação de testes
- Escrita e revisão desta documentação
