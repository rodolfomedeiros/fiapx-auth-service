# fiapx-auth-service

Autenticação do **FIAP X**: cadastro de usuários, login com emissão de JWT e introspecção
de token para os demais microsserviços.

**Java 25 (LTS)** · Spring Boot 4 · Spring Security · Spring Data JPA · Flyway

## Repositórios do projeto

| Repositório | Linguagem | Papel |
| :--- | :--- | :--- |
| [fiapx-platform](https://github.com/rodolfomedeiros/fiapx-platform) | — | Compose, Kubernetes, contratos, topologia do broker |
| **fiapx-auth-service** *(você está aqui)* | Java 25 · Spring Boot 4 | Cadastro, login, emissão e introspecção de JWT |
| [fiapx-video-management-service](https://github.com/rodolfomedeiros/fiapx-video-management-service) | Python 3.13 · FastAPI | Upload, listagem, download e WebSocket de tempo real |
| [fiapx-video-processor-worker](https://github.com/rodolfomedeiros/fiapx-video-processor-worker) | Rust 1.94 · Tokio | Extração de quadros com FFmpeg e compactação em `.zip` |
| [fiapx-notification-service](https://github.com/rodolfomedeiros/fiapx-notification-service) | Go 1.25 | Consumo da DLQ e envio de e-mail de falha |
| [fiapx-web](https://github.com/rodolfomedeiros/fiapx-web) | React 19 · TypeScript 6 | Interface de upload, acompanhamento e download |

> Para subir o sistema inteiro, use o **fiapx-platform**. Este repositório sozinho precisa
> de um PostgreSQL acessível.

## Onde este serviço entra

```mermaid
flowchart TB
    Client(["Cliente"])
    GW["API Gateway<br/>Nginx · Ingress"]
    Auth["auth-service<br/>Java 25 · Spring Boot 4"]
    VMS["video-management-service<br/>Python 3.13"]
    Worker["video-processor-worker<br/>Rust 1.94"]
    Notif["notification-service<br/>Go 1.25"]
    MQ{{"RabbitMQ<br/>video.events"}}
    PG[("PostgreSQL 17")]
    S3[("MinIO · S3")]

    Client -->|login| GW
    GW --> Auth
    GW --> VMS
    VMS -.->|"POST /introspect<br/>a cada requisição"| Auth
    Auth ---|"tabela users"| PG
    VMS --- PG
    VMS --> MQ
    MQ --> Worker
    MQ --> Notif
    Worker --- S3

    classDef atual stroke:#e8590c,stroke-width:4px
    class Auth atual
```

O serviço de vídeos **não** valida o JWT por conta própria: ele chama
`POST /api/v1/auth/introspect`. Assim a chave de assinatura nunca sai deste serviço, e
revogar ou trocar o segredo não exige redeploy dos demais. O custo dessa chamada extra por
requisição é amortizado por um cache no Redis, do lado do serviço de vídeos.

## API

Caminhos internos são `/api/v1/auth/*`; pelo gateway do Compose, `/auth/*`.

### `POST /api/v1/auth/register`

```json
{ "name": "Ana", "email": "ana@example.com", "password": "senha-bem-longa" }
```

`201` devolve `{ "id", "name", "email" }`. O e-mail é normalizado para minúsculas e a senha
guardada com BCrypt — nunca em claro, e nunca devolvida.

| Regra | Resposta |
| :--- | :--- |
| `name` até 100 caracteres, `email` válido, `password` de 8 a 128 | `400` se violada |
| E-mail já cadastrado | `409` |

### `POST /api/v1/auth/login`

```json
{ "email": "ana@example.com", "password": "senha-bem-longa" }
```

`200` devolve `{ "access_token", "token_type": "Bearer", "expires_in": 3600 }`.
Credenciais inválidas devolvem `401`.

O login compara um hash **mesmo quando o e-mail não existe**, para que o tempo de resposta
não revele quais contas estão cadastradas.

### `POST /api/v1/auth/introspect`

Recebe `Authorization: Bearer <token>` e sempre responde `200`:

```json
{ "active": true, "sub": "<uuid>", "email": "ana@example.com", "name": "Ana" }
```

Token ausente, malformado, expirado ou assinado com outra chave devolve
`{ "active": false }`, com os demais campos omitidos.

### Operacionais

| Endpoint | Uso |
| :--- | :--- |
| `GET /actuator/health/readiness` | Probe de readiness |
| `GET /actuator/health/liveness` | Probe de liveness |
| `GET /actuator/prometheus` | Métricas |

## Configuração

| Variável | Padrão | Descrição |
| :--- | :--- | :--- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/fiapx` | JDBC do PostgreSQL |
| `DATABASE_USERNAME` | `fiapx` | Usuário do banco |
| `DATABASE_PASSWORD` | `fiapx` | Senha do banco |
| `JWT_SECRET` | valor de desenvolvimento | Segredo HMAC — **mínimo 32 bytes** |
| `JWT_EXPIRATION_MINUTES` | `60` | Validade do token emitido |
| `PORT` | `8081` | Porta HTTP |

## Organização

Camadas no molde dos serviços do `gear-up`: as dependências apontam para dentro, e os casos
de uso não conhecem Spring, JPA nem HTTP.

```
com.fiapx.auth/
├── entities/       # User, TokenClaims e as exceções de domínio
├── usecases/       # RegisterUser, AuthenticateUser, IntrospectToken + ports/
├── controllers/    # AuthController: só traduz HTTP, com os DTOs de request e response
├── gateways/       # Implementações das ports: JPA, BCrypt e JWT
├── drivers/        # Repositório do Spring Data, SecurityConfig e a fiação dos casos de uso
└── shared/         # GlobalExceptionHandler
```

| Ponto | Por quê |
| :--- | :--- |
| Handlers sem `@Service` | São Java puro, montados em `AuthUseCaseConfig`, e instanciáveis em teste sem subir contexto |
| `UserRepository` é uma interface do domínio | O Spring Data mora no driver; `JpaUserRepository` faz a ponte |
| Exceções de domínio, não `ResponseStatusException` | O `GlobalExceptionHandler` traduz para 409 e 401, e o domínio não precisa conhecer HTTP |
| `User` é entidade e mapeamento ao mesmo tempo | Separá-los pediria um mapper para quatro campos que nunca divergem do schema |
| Sem camada de presenters | Os DTOs de resposta são quatro records no controller; uma camada só para eles seria cerimônia |

`spring.jpa.hibernate.ddl-auto` está em `validate`: a aplicação recusa subir se o schema
encontrado não corresponder ao mapeamento, em vez de alterar o banco silenciosamente.

## Executar

```sh
mvn spring-boot:run
```

Com o PostgreSQL do Compose já de pé, os padrões das variáveis funcionam sem configuração
adicional.

## Testes

```sh
mvn verify
```

35 testes cobrindo emissão e verificação do token (expiração, assinatura adulterada, payload
adulterado, token malformado), cadastro com e-mail duplicado, normalização do e-mail,
validação de payload e todas as respostas de introspecção. Não é necessário banco: a porta de
repositório é substituída por um mock e as rotas rodam em `MockMvc` standalone.

A separação em camadas rendeu testes mais diretos: as regras são exercitadas nos handlers,
em JUnit puro, e o `MockMvc` ficou só com o que é de fato HTTP — status, formato da resposta
e validação de entrada.

`mvn verify` trava o build abaixo de **80% de cobertura de linhas**, via JaCoCo, como nos
serviços do `gear-up`. Hoje o conjunto avaliado está em 91%. Ficam de fora `drivers/`, a
classe de boot e as `@Configuration`: são fiação, e cobri-las exercitaria o Spring, não as
regras.

```sh
open target/site/jacoco/index.html   # relatório detalhado
```
