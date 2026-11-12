# ⚡ FlashSale Chaos Lab

> **Simulador Interativo de Alta Concorrência & Engenharia do Caos**  
> Demonstrando na prática as diferenças arquiteturais entre **Consistência Forte (ACID / CP)** e **Disponibilidade Eventual (BASE / AP)** com **Java 21/25, Spring Boot 3.4, PostgreSQL, Redis, Toxiproxy, React, Tailwind CSS e Prometheus**.

---

## 🎯 Proposta de Valor e Conceito Teórico

A maioria das aplicações CRUD em portfólios opera apenas em cenários ideais ("caminho feliz"), onde redes nunca falham e transações são triviais. 

O **FlashSale Chaos Lab** simula a venda de ingressos de um evento de altíssima demanda (*Flash Sale*) submetido a falhas intencionais de infraestrutura (latência e partições de rede) injetadas via **Toxiproxy**:

1. **Fluxo Crítico de Compra (ACID / CP):**
   - Garante **zero venda duplicada (*double booking*)** utilizando transações explícitas com bloqueio pessimista a nível de linha no PostgreSQL (`LockModeType.PESSIMISTIC_WRITE` -> `SELECT ... FOR UPDATE`).
   - Sob partição de rede no banco relacional, falha rapidamente com **`503 Service Unavailable`** (RFC 7807 `ProblemDetail`), preferindo consistência à disponibilidade (**Garantia CP do Teorema CAP**).
2. **Fluxo de Vitrine e Catálogo (BASE / AP):**
   - Garante **baixa latência (&lt; 15ms)** e alta disponibilidade lendo exclusivamente do cache distribuído no **Redis**.
   - Atualizado assincronamente pós-commit (`@Async`), simulando *replication lag*.
   - Se o banco principal cair, a vitrine continua respondendo **`200 OK`** com os últimos dados disponíveis (**Garantia AP do Teorema CAP**).
3. **Detecção Visual de Drift ($\Delta$):**
   - O painel calcula em tempo real a divergência entre a verdade relacional e a visão em cache: $\Delta = |\text{Estoque}_{\text{Postgres}} - \text{Estoque}_{\text{Redis}}|$.
   - Quando $\Delta > 0$, um alerta pulsante sinaliza a inconsistência temporária decorrente de partição ou lag.

---

## 🏗️ Topologia da Arquitetura

```text
+-----------------------------------------------------------------------------------------+
|                               FRONTEND (React + Tailwind CSS)                           |
|      [Botões de Caos]         [Comparador ACID vs BASE]         [Feed de Telemetria]    |
|   (http://localhost:3005)     (Postgres Real vs Redis Vitrine)  (RPS, Status 201/409/503)
+--------------------------------------------+--------------------------------------------+
                                             | HTTP / REST
                                             v
+-----------------------+        +--------------------------------------------------------+
|       PROMETHEUS      |        |              BACKEND (Java 21/25 + Spring Boot)        |
|  (http://localhost:   |<-------|  - Virtual Threads habilitadas                         |
|         9095)         | Scrape |  - /api/tickets/buy (ACID / CP - Lock Pessimista)      |
+-----------------------+  5s    |  - /api/tickets/catalog (BASE / AP - Cache Redis)      |
                                 |  - /api/chaos/* (Orquestrador Toxiproxy)               |
                                 |  - /actuator/health/{liveness,readiness} (K8s Probes)  |
                                 |  - /actuator/prometheus (Micrometer Metrics)           |
                                 +------------+-----------------------------+-------------+
                                              |                             |
                                  Porta Proxy |                 Porta Proxy |
                                  (TCP 25432) |                 (TCP 26379) |
                                              v                             v
                                 +--------------------------------------------------------+
                                 |               TOXIPROXY (Chaos Engine)                 |
                                 |        (API REST: http://localhost:8474)               |
                                 |  Injeta latência, perda de pacotes e corte de links    |
                                 +------------+-----------------------------+-------------+
                                              |                             |
                                   Porta Real |                  Porta Real |
                                   (TCP 5432) |                  (TCP 6379) |
                                              v                             v
                                 +-------------------------+   +--------------------------+
                                 |       PostgreSQL        |   |          Redis           |
                                 |  (Fonte da Verdade CP)  |   |    (Vitrine Cache AP)    |
                                 +-------------------------+   +--------------------------+
```

---

## 🌐 Mapeamento de Serviços e Portas (Zero Conflitos)

Todos os serviços operam em portas dedicadas na rede isolada `flashsale-network`, sem colidir com outros containers locais:

| Serviço | Porta Host | Porta Container | Descrição |
| :--- | :--- | :--- | :--- |
| **Frontend Dashboard** | [`3005`](http://localhost:3005) | `80` | Interface visual SPA React + Tailwind CSS |
| **Backend Spring Boot** | [`8088`](http://localhost:8088) | `8080` | API REST, Actuator, Probes e Telemetria |
| **Prometheus UI** | [`9095`](http://localhost:9095) | `9090` | Servidor de monitoramento e métricas em tempo real |
| **Toxiproxy Admin** | [`8474`](http://localhost:8474) | `8474` | API de controle dos proxies TCP e injeção de toxics |
| **Proxy PostgreSQL** | `25432` | `25432` | Canal TCP com injeção de falhas para o PostgreSQL |
| **Proxy Redis** | `26379` | `26379` | Canal TCP com injeção de falhas para o Redis |
| **PostgreSQL Real** | *Rede interna* | `5432` | Banco relacional isolado (evita colisão com a 5432 do host) |
| **Redis Real** | *Rede interna* | `6379` | Cache de leitura isolado |

---

## 🚀 Como Executar em 1 Comando

### Pré-requisitos
- Docker e Docker Compose instalados.

### Inicialização Rápida
```bash
# Clone o repositório
git clone https://github.com/guilermiii/flash-sale-chaos-lab.git
cd flash-sale-chaos-lab

# Suba todo o ecossistema (Postgres, Redis, Toxiproxy, Backend, Frontend e Prometheus)
docker compose up -d --build
```

Acesse o painel web em: **[http://localhost:3005](http://localhost:3005)**

---

## ☸️ Endpoints do Kubernetes (Liveness & Readiness Probes)

A aplicação utiliza a integração oficial do Spring Boot Actuator com o ecossistema Kubernetes:

| Probe | Endpoint HTTP | Papel no Kubernetes |
| :--- | :--- | :--- |
| **Liveness Probe** | `GET http://localhost:8088/actuator/health/liveness` | Verifica se a aplicação está viva. Se falhar, o kubelet reinicia o container. |
| **Readiness Probe** | `GET http://localhost:8088/actuator/health/readiness` | Verifica se a aplicação está pronta para receber tráfego de usuários. |
| **Health Detalhado** | `GET http://localhost:8088/actuator/health` | Status de todos os componentes (Postgres, Redis, espaço em disco, etc.). |

### Exemplo de Configuração de Pod Kubernetes:
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: flashsale-backend
spec:
  containers:
    - name: backend
      image: flashsale-backend:latest
      ports:
        - containerPort: 8080
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 15
        periodSeconds: 10
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        initialDelaySeconds: 10
        periodSeconds: 5
```

---

## 📊 Coleta de Métricas com Prometheus

A aplicação exporta métricas no padrão OpenMetrics/Prometheus através da biblioteca oficial **Micrometer Prometheus Registry**:

- **Endpoint de Métricas:** `GET http://localhost:8088/actuator/prometheus`
- **Interface Web do Prometheus:** [http://localhost:9095](http://localhost:9095)
- **Status dos Alvos:** [http://localhost:9095/targets](http://localhost:9095/targets) (Target `flashsale-backend` em status `UP`)

### Principais Métricas Coletadas:
- `http_server_requests_seconds_count{uri="/api/tickets/buy", status="201|409|503"}`: Taxa e distribuição de status das vendas.
- `http_server_requests_seconds_max`: Latência máxima observada (útil no teste de `HIGH_LATENCY`).
- `jvm_memory_used_bytes`: Uso de memória Heap e Non-Heap da JVM.
- `jvm_threads_live_threads`: Contagem de threads ativas (validando Virtual Threads).

---

## 🧪 Laboratório na Prática: Cenários de Caos

Você pode alternar os cenários com um clique no painel ou via `curl`:

### 1. Cenário `HEALTHY` (Rede 100% Operacional)
```bash
curl -X POST http://localhost:8088/api/chaos/scenario/HEALTHY
```
- Compras no PostgreSQL e consultas no Redis operam normalmente. $\Delta = 0$.

### 2. Cenário `HIGH_LATENCY` (Latência de +3s no Banco Relacional)
```bash
curl -X POST http://localhost:8088/api/chaos/scenario/HIGH_LATENCY
```
- Injeta toxic de latência de $3000\text{ms}$ ($\pm 500\text{ms}$ jitter) no proxy do PostgreSQL.
- O endpoint de compra demora mais de 3s, testando os timeouts de conexão e a capacidade das *Virtual Threads*.

### 3. Cenário `PARTITION_CP` (Corte no Banco Principal)
```bash
curl -X POST http://localhost:8088/api/chaos/scenario/PARTITION_CP
```
- Desabilita o proxy do PostgreSQL (`enabled: false`).
- **Comportamento CP:** `POST /api/tickets/buy` retorna imediatamente **`503 Service Unavailable`** para impedir inconsistência.
- **Comportamento AP:** `GET /api/tickets/catalog` segue respondendo **`200 OK`** diretamente do Redis com `dataSource: REDIS_FALLBACK_AP`.

### 4. Cenário `PARTITION_AP_LAG` (Corte na Sincronização de Cache)
```bash
curl -X POST http://localhost:8088/api/chaos/scenario/PARTITION_AP_LAG
```
- Desabilita o proxy do Redis (`enabled: false`).
- Compras continuam sendo aprovadas no PostgreSQL (saldo real diminui), mas a vitrine pública não recebe as atualizações.
- O dashboard exibe em tempo real o **Drift ($\Delta > 0$)** com banner pulsante de alerta.
- Ao clicar em `HEALTHY`, a consistência eventual é atingida e $\Delta$ retorna a 0.

### 5. Reset do Ambiente
```bash
curl -X POST http://localhost:8088/api/admin/reset
```
- Restaura o estoque para 50 ingressos, apaga o histórico de ordens, limpa o Redis e redefine o Toxiproxy para `HEALTHY`.

---

## 🛠️ Stack Tecnológica

- **Backend:** Java 21/25, Spring Boot 3.4.2 (Web, Data JPA, Data Redis, Validation, Actuator)
- **Métricas & Probes:** Micrometer Prometheus, Spring Actuator Kubernetes Probes
- **Persistência & Cache:** PostgreSQL 17 (LockModeType.PESSIMISTIC_WRITE), Redis 7
- **Motor de Caos:** Toxiproxy 2.11 (Shopify)
- **Frontend:** React 18, Tailwind CSS, Lucide React, Nginx
- **Monitoramento:** Prometheus v3.1.0
- **Testes Automatizados:** JUnit 5, Mockito, AssertJ, MockMvc Standalone (29 testes unitários)

---

## 📜 Licença

MIT License. Desenvolvido para fins educacionais e demonstração prática de engenharia de software distribuída.
