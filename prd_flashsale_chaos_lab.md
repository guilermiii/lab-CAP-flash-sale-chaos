# Product Requirements Document (PRD): FlashSale Chaos Lab

* **Versão:** 1.0 (MVP de Portfólio)
* **Papel:** Especialista de Produto / Tech Product Manager
* **Público-Alvo do Produto:** Recrutadores Técnicos, Tech Leads e Engenheiros Avaliadores
* **Autor / Desenvolvedor:** Desenvolvedor Backend / Fullstack Júnior
* **Stack Principal:** Java 21/25, Spring Boot 3.4/4, PostgreSQL, Redis, Toxiproxy, Prometheus, k6, React, Tailwind CSS, Docker Compose

---

## 1. Visão do Produto e Proposta de Valor

### 1.1 O Problema
A maioria dos portfólios de desenvolvedores júnior apresenta apenas operações CRUD tradicionais em cenários ideais ("caminho feliz"), onde a rede nunca falha e apenas um usuário acessa o sistema por vez. Em entrevistas técnicas, candidatos frequentemente sabem recitar as definições teóricas de **ACID**, **BASE** e **Teorema CAP**, mas não conseguem demonstrar como esses conceitos impactam o código e a experiência do usuário em produção.

### 1.2 A Solução
O **FlashSale Chaos Lab** é um laboratório interativo de engenharia do caos e sistemas distribuídos que simula a venda de ingressos de um evento de altíssima demanda (*Flash Sale*). 

A aplicação coloca lado a lado, em tempo real, dois fluxos arquiteturais distintos operando sob carga pesada e falhas de rede injetadas intencionalmente:
1. **Fluxo Crítico de Compra (ACID / CP):** Garante consistência forte e zero duplicidade de vendas (*double booking*), preferindo recusar requisições com `503 Service Unavailable` caso o banco relacional sofra uma partição de rede.
2. **Fluxo de Vitrine e Consulta (BASE / AP):** Garante alta disponibilidade e baixa latência via cache distribuído, continuando no ar com status `200 OK` mesmo se a sincronização com o banco principal cair (exibindo dados temporariamente desatualizados até a rede se recuperar).

### 1.3 Pitch de Elevador (Para colocar no topo do seu GitHub/LinkedIn)
> *"Construí um simulador de venda de ingressos de alta concorrência em Java 25 e Spring Boot 4 com um painel de Engenharia do Caos em React. Com um clique, o usuário corta conexões de rede via Toxiproxy durante testes de carga do k6 para visualizar na prática a diferença entre consistência forte (ACID/CP) e disponibilidade eventual (BASE/AP)."*

---

## 2. Personas e Objetivos

| Persona | Perfil | O que busca ao avaliar o projeto |
| :--- | :--- | :--- |
| **Tech Lead / Entrevistador** | Avaliador técnico de vagas Júnior/Pleno | Quer verificar se você entende concorrência (*locks*), transações, resiliência, tratamento de erros HTTP semânticos e observabilidade básica. |
| **Recrutador Técnico (Tech Recruiter)** | Triagem inicial de currículo e GitHub | Busca projetos visualmente compreensíveis, com boa documentação (README), arquitetura moderna (Java 25 / Spring 4) e fáceis de entender sem precisar ler milhares de linhas de código. |
| **Desenvolvedor (Você)** | Construtor do projeto | Precisa de um escopo enxuto, sem *overengineering*, que possa ser executado localmente com um único comando Docker e gere evidências visuais fortes. |

---

## 3. Arquitetura e Topologia do Sistema

Toda a comunicação entre a aplicação Spring Boot e os bancos de dados passa obrigatoriamente pelo **Toxiproxy**, permitindo simular problemas reais de infraestrutura sem derrubar os containers.

```text
+-----------------------------------------------------------------------+
|                     FRONTEND (React + Tailwind CSS)                   |
|  [Botões de Caos]   [Comparador ACID vs BASE]   [Feed de Logs Live]   |
+-----------------------------------+-----------------------------------+
                                    | HTTP / REST + SSE
                                    v
+-----------------------+   +-------------------------------------------+
|   GERADOR DE CARGA    |   |        BACKEND (Java 25 + Spring 4)       |
|         (k6)          |-->|  - Virtual Threads habilitadas            |
|  500 VUs simultâneos  |   |  - /api/tickets/buy (ACID / CP)           |
+-----------------------+   |  - /api/tickets/catalog (BASE / AP)       |
                            |  - /api/chaos/* (Controle Toxiproxy)      |
                            +---------+-----------------------+---------+
                                      |                       |
                        Porta Proxy   |                       | Porta Proxy
                        (TCP 25432)   |                       | (TCP 26379)
                                      v                       v
                            +-------------------------------------------+
                            |          TOXIPROXY (Chaos Engine)         |
                            |   Injeta latência, perda de pacotes e     |
                            |   partições de rede (Network Partition)   |
                            +---------+-----------------------+---------+
                                      |                       |
                        Porta Real    |                       | Porta Real
                        (TCP 5432)    |                       | (TCP 6379)
                                      v                       v
                            +-------------------+   +-------------------+
                            |    PostgreSQL     |   |       Redis       |
                            |  (Fonte da Verdade|   | (Cache de Leitura |
                            |    ACID / CP)     |   |   & Eventos BASE) |
                            +-------------------+   +-------------------+
```

### Papel de cada tecnologia na arquitetura
* **Java 25 + Spring Boot 4:** Aproveita *Virtual Threads* para lidar com centenas de requisições bloqueadas durante simulações de latência sem esgotar o pool de threads do servidor web.
* **PostgreSQL:** Armazena o inventário oficial de ingressos e o registro de pedidos. Garante **ACID** usando transações explícitas e bloqueio pessimista a nível de linha (`SELECT ... FOR UPDATE`).
* **Redis:** Armazena a contagem rápida da vitrine e o feed dos últimos compradores. Atualizado de forma assíncrona após a confirmação no PostgreSQL, demonstrando **BASE** e **Consistência Eventual**.
* **Toxiproxy:** Proxy TCP que atua como "interruptor de rede" entre o Spring Boot e os bancos de dados.
* **k6:** Ferramenta de teste de carga em JavaScript que dispara centenas de compradores simultâneos disputando um estoque limitado.
* **React + Tailwind CSS:** Interface de controle visual que transforma conceitos abstratos de backend em botões interativos e gráficos fáceis de demonstrar.

---

## 4. Escopo Funcional (Épicos e User Stories)

### Épico 1: Núcleo Backend (ACID vs. BASE)

* **US01 - Compra de Ingresso com Consistência Forte (ACID / CP)**
  * **Como** comprador em uma *Flash Sale*, **quero** finalizar a compra de um ingresso de forma segura **para que** meu assento seja garantido sem risco de venda duplicada.
  * **Especificação Técnica:** Endpoint `POST /api/tickets/buy`. Executa dentro de uma transação `@Transactional` no PostgreSQL com `LockModeType.PESSIMISTIC_WRITE`.
  * **Critérios de Aceite:**
    * Se houver saldo $> 0$, decrementa 1 unidade no PostgreSQL, salva o pedido e retorna `201 Created`.
    * Se o saldo for $0$, retorna `409 Conflict` (Ingressos Esgotados).
    * Se o PostgreSQL estiver inacessível (partição de rede via Toxiproxy) ou exceder o timeout de conexão/transação, falha rapidamente e retorna `503 Service Unavailable` (comportamento **CP**).

* **US02 - Consulta de Vitrine de Alta Disponibilidade (BASE / AP)**
  * **Como** visitante da loja, **quero** visualizar quantos ingressos restam e os últimos compradores **mesmo** que o sistema de pagamentos esteja instável.
  * **Especificação Técnica:** Endpoint `GET /api/tickets/catalog`. Lê exclusivamente do **Redis**. A atualização do Redis ocorre de forma assíncrona (`@Async` ou via evento interno após o commit do PostgreSQL), com um atraso configurável (ex: $500\text{ms}$ a $2000\text{ms}$) para tornar o *replication lag* visível.
  * **Critérios de Aceite:**
    * Responde em menos de $15\text{ms}$ em condições normais.
    * Caso o PostgreSQL caia via Toxiproxy, este endpoint continua respondendo `200 OK` com os últimos dados disponíveis no Redis (comportamento **AP**).
    * Caso a conexão entre o Spring Boot e o Redis seja cortada, o sistema degrada graciosamente ou exibe o estado de desconexão no painel sem derrubar a aplicação.

* **US03 - Reset de Ambiente para Demonstrações**
  * **Como** apresentador do projeto, **quero** resetar o estoque para $50$ ingressos e limpar os logs com um clique **para** repetir testes durante uma entrevista.
  * **Especificação Técnica:** Endpoint `POST /api/admin/reset` que restaura o saldo no PostgreSQL, sincroniza o Redis e remove qualquer *toxic* ativo no Toxiproxy.

---

### Épico 2: Orquestrador de Engenharia do Caos (Integração Toxiproxy)

* **US04 - API de Controle de Topologia de Rede**
  * **Como** usuário do painel React, **quero** acionar cenários de falha de rede através de botões simples **sem** precisar digitar comandos `curl` no terminal.
  * **Especificação Técnica:** O backend Spring Boot expõe `POST /api/chaos/scenario/{tipo}` e `GET /api/chaos/status`, comunicando-se internamente com a API administrativa do Toxiproxy (`http://toxiproxy:8474`).
  * **Cenários Obrigatórios:**
    1. `HEALTHY`: Remove todas as falhas. Rede 100% operacional.
    2. `HIGH_LATENCY`: Adiciona um *toxic* de latência de $3000\text{ms}$ ($\pm 500\text{ms}$ de jitter) no proxy do PostgreSQL. Demonstra degradação de performance e retenção de conexões.
    3. `PARTITION_CP` (Corte no Banco Principal): Desabilita o proxy do PostgreSQL (`enabled: false`). Prova o lado **CP**: compras param (`503`), vitrine Redis continua no ar (`200`).
    4. `PARTITION_AP_LAG` (Corte na Sincronização/Cache): Desabilita o proxy do Redis (`enabled: false`). Compras continuam no PostgreSQL (saldo real diminui), mas a vitrine fica congelada no passado. Quando o botão `HEALTHY` é clicado novamente, o sistema reconcilia os dados (**Consistência Eventual**).

---

### Épico 3: Painel Interativo de Simulação (Frontend React + Tailwind CSS)

O frontend deve ser uma **Single Page Application (SPA)** limpa, responsiva e com estética de *Observability Dashboard* (modo escuro recomendado para destacar indicadores de status).

* **US05 - Painel de Botões de Trade-off (Chaos Controls)**
  * **Layout:** Barra superior ou card em destaque com 4 botões de estado mutuamente exclusivos (indicando visualmente qual cenário está ativo no momento) + botão de "Resetar Estoque (50 ingressos)" + botão "Comprar 1 Ingresso Manualmente".
  * **Feedback Visual:** Cada botão deve exibir um badge explicando o conceito teórico atrelado:
    * `[Verde] Rede Saudável` -> *ACID + BASE em harmonia*
    * `[Amarelo] Latência Extrema (+3s DB)` -> *Teste de Timeout & Virtual Threads*
    * `[Vermelho] Cortar PostgreSQL` -> *Simular CAP: Prioridade CP (Bloqueia Vendas)*
    * `[Laranja] Cortar Redis Sync` -> *Simular CAP: Prioridade AP (Dados Defasados)*

* **US06 - Comparador Visual Lado a Lado (Verdade Real vs. Visão Eventual)**
  * **Layout:** Duas colunas centrais de alto contraste:
    * **Coluna Esquerda (PostgreSQL - ACID / CP):** Exibe o estoque real confirmado no banco relacional, status da conexão (`ONLINE` / `PARTITIONED`) e contador de vendas bloqueadas por segurança (`503`).
    * **Coluna Direita (Redis - BASE / AP):** Exibe o estoque exibido na vitrine pública, status da conexão e um indicador calculado em tempo real de **Divergência (Drift)**: $\Delta = |\text{Estoque}_{\text{Postgres}} - \text{Estoque}_{\text{Redis}}|$.
  * **Objetivo de UX:** Quando $\Delta > 0$, um alerta amarelo pulsa na tela mostrando: *"Inconsistência Temporária Detectada (BASE): A vitrine está exibindo X ingressos a mais que o estoque real devido à partição de rede."* Ao restaurar a rede, o usuário vê $\Delta$ voltar a $0$.

* **US07 - Stream de Requisições e Telemetria ao Vivo**
  * **Layout:** Console/tabela na parte inferior da tela atualizada a cada $500\text{ms}$ (via polling leve ou Server-Sent Events - SSE) mostrando:
    * Taxa de requisições por segundo (RPS) vindo do k6 ou cliques manuais.
    * Distribuição de status HTTP (`201 Created`, `409 Conflict`, `503 Service Unavailable`).
    * Últimas 10 transações com timestamp, endpoint chamado, tempo de resposta em ms e resultado.

---

### Épico 4: Suite de Estresse e Concorrência (k6)

* **US08 - Script de Disputa Extrema (*Flash Sale Race Condition*)**
  * **Objetivo:** Provar que o modelo **ACID** impede *double booking*.
  * **Configuração do Script (`flash-sale-stress.js`):**
    * Dispara $300$ Virtual Users (VUs) simultâneos tentando comprar os $50$ ingressos disponíveis em uma janela de $15$ segundos, enquanto outros $100$ VUs consultam `GET /api/tickets/catalog` continuamente.
  * **Resultado Esperado:** Exatamente $50$ respostas `201 Created`, exatamente $250$ respostas `409 Conflict`, e saldo final no PostgreSQL igual a $0$ (nunca negativo).

* **US09 - Script de Caos Contínuo (*Chaos Endurance*)**
  * **Objetivo:** Gerar tráfego constante por $2$ minutos para que o usuário possa clicar nos botões do painel React durante o teste e observar os gráficos reagindo ao vivo.

---

## 5. Requisitos Não-Funcionais (Qualidade de Portfólio)

1. **Execução "Zero-Friction" (`docker-compose up`):** Todo o ecossistema (PostgreSQL, Redis, Toxiproxy, Backend Java 25, Frontend React) deve subir com um único comando `docker compose up --build`, sem exigir instalações locais complexas de bancos ou proxies na máquina do avaliador.
2. **Resiliência Configurada:** Pools de conexão (HikariCP) e timeouts do cliente Redis devem ser configurados para falhar rápido ($2\text{s}$ a $3\text{s}$) durante partições de rede, evitando que a aplicação trave indefinidamente.
3. **Documentação Orientada a Evidências (`README.md`):**
   * Diagrama da arquitetura.
   * Instruções de execução em 3 passos.
   * Seção **"Laboratório na Prática"** contendo capturas de tela do painel React em cada um dos 4 estados do Toxiproxy e o sumário de saída do terminal do k6.

---

## 6. Fora de Escopo (Anti-Scope do MVP)

Para manter o foco estrito nos conceitos de **CAP, ACID e BASE** e garantir a entrega rápida com alta qualidade, os seguintes itens estão **explicitamente fora de escopo**:
* Autenticação e Autorização (Login, JWT, OAuth2) — use IDs de compradores gerados aleatoriamente (ex: `user-8492`).
* Integração com gateways de pagamento reais ou fictícios externos.
* Apache Kafka ou RabbitMQ (o próprio Redis Streams/PubSub ou eventos assíncronos do Spring já cumprem o papel de propagação eventual sem pesar o Docker Compose).
* Orquestração com Kubernetes (Docker Compose é a ferramenta ideal para reprodutibilidade local).

---

## 7. Roadmap de Implementação Sugerido

| Fase | Foco Principal | Entregáveis Chave |
| :--- | :--- | :--- |
| **Fase 1: Infra & ACID** | Subir ambiente e garantir consistência forte | `docker-compose.yml` com Postgres, Redis e Toxiproxy. Projeto Java 25 + Spring 4 com `POST /api/tickets/buy` usando lock pessimista e testes unitários/integração. |
| **Fase 2: Camada BASE** | Adicionar leitura rápida e propagação assíncrona | Integração com Redis, endpoint `GET /api/tickets/catalog`, publicação assíncrona de eventos pós-compra e mecanismo de reconciliação. |
| **Fase 3: Motor de Caos** | Conectar Spring Boot ao Toxiproxy | Endpoints `/api/chaos/*` manipulando proxies TCP do Postgres e Redis. Teste manual via Postman/Insomnia validando erros `503` (CP) e `200` com cache antigo (AP). |
| **Fase 4: Frontend React** | Construir o painel visual em React + Tailwind | Tela única com os botões de cenário de caos, comparador lado a lado (Postgres vs. Redis) com indicador de *Drift* e feed de requisições ao vivo. |
| **Fase 5: k6 & README** | Validar sob estresse e empacotar para recrutadores | Scripts k6 (`stress` e `chaos`), coleta de prints/métricas reais e escrita do `README.md` explicando as decisões arquiteturais. |

---

## 8. Checklist de Pronto (Definition of Done para o Portfólio)

* [ ] O projeto sobe completamente com `docker compose up`.
* [ ] Teste do k6 com $300$ usuários simultâneos para $50$ ingressos resulta em exatamente $50$ vendas aprovadas (zero saldo negativo).
* [ ] Clicar em **"Cortar PostgreSQL"** no React faz o endpoint de compra retornar `503` imediatamente, enquanto o catálogo segue respondendo `200`.
* [ ] Clicar em **"Cortar Redis Sync"** permite novas compras no Postgres e exibe no painel React a diferença ($\Delta$) entre o estoque real e o estoque em cache.
* [ ] Restaurar a rede faz o estoque do Redis convergir automaticamente para o valor real do PostgreSQL.
* [ ] O `README.md` contém prints claros de todos os cenários acima.