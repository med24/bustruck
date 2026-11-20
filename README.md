# bustruck
## Microservice Scaffolding (`generation.ps1`)

The `generation.ps1` PowerShell script automates bootstrapping Spring Boot microservices for the **BusTrack** architecture via the [Spring Initializr](https://spring.io) API. Each generated service is configured with Java 17, Maven, and the `com.bustrack.<servicename>` base package.

### Preconfigured Services

| Option | Service Name | Included Dependencies |
| :--- | :--- | :--- |
| **`1`** | `payment-service` | `web-services`, `data-jpa`, `postgresql`, `actuator` |
| **`2`** | `tracking-service` | `data-jpa`, `postgresql`, `actuator` |
| **`3`** | `ticket-service` | `web`, `validation`, `data-jpa`, `postgresql`, `actuator`, `web-services` |
| **`4`** | `api-gateway` | `cloud-gateway`, `actuator` |
| **`A`** | **All Services** | Generates all four core services above in batch |
| **`C`** | **Custom Service** | Prompts for a custom artifact name and comma-separated Spring dependencies |

### Usage

1. Open a PowerShell terminal in the `bustrack/` root directory.
2. *(First-time Windows setup only)* Allow local script execution for your user account:
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```
3. Run the generator script:
   ```powershell
   .\generation.ps1
   ```
4. Follow the interactive prompt to select one or more services:
   * **Single service:** Enter `1`
   * **Multiple services:** Enter `1,3`
   * **All services:** Enter `A`
   * **New custom service:** Enter `C`

### Post-Generation Notes
* **Protocol Contracts:** After generating the base Spring Boot folders, place your gRPC (`tracking.proto`) and SOAP (`payment.xsd`) contract files into their respective `src/main/` paths or sync them from the `contracts/` directory.
* **Containerization:** Add the service-specific `Dockerfile` to each generated root directory before running `docker-compose up`.


### Contracts (**FlowDiagram**)
> bus not included in this diagram 


```mermaid
flowchart TB
    Client(["External Client<br/>(Mobile App / Web / Postman)"])

    subgraph GatewayLayer["Entry Point"]
        GW["api-gateway<br/>(Port 8080)"]
    end

    subgraph CoreServices["BusTrack Microservices"]
        Ticket["ticket-service<br/>(REST Server + SOAP/gRPC Client)<br/>Port 8081"]
        Tracking["tracking-service<br/>(gRPC Server)<br/>Port 9090 (gRPC) / 8082 (Actuator)"]
        Payment["payment-service<br/>(SOAP Server)<br/>Port 8083"]
    end

    subgraph Contracts["Published Contracts (/contracts)"]
        OpenAPI["📄 OpenAPI 3.0<br/>contracts/openapi/ticket-service.yaml<br/>──────────────────────────<br/>• Defines REST endpoints (/api/v1/lines, /tickets)<br/>• Validates JSON request/response DTOs<br/>• Generates Swagger UI documentation"]
        Proto["⚡ Protocol Buffers (gRPC)<br/>contracts/proto/tracking.proto<br/>──────────────────────────<br/>• Binary low-latency GPS telemetry<br/>• Unary: GetLastPosition<br/>• Server Stream: StreamLinePositions<br/>• Client Stream: ReportPositions"]
        SOAP["🧾 XSD / WSDL (SOAP)<br/>contracts/soap/payment.xsd & .wsdl<br/>──────────────────────────<br/>• Strict XML schema for financial transactions<br/>• Operations: Authorize, Refund, GetStatus<br/>• Typed SOAP Faults for declined cards"]
    end

    Client -->|"HTTP / JSON"| GW
    GW -->|"Routes /api/v1/**"| Ticket

    Ticket -.->|"Implements & Serves"| OpenAPI
    Ticket -->|"gRPC (HTTP/2 + Protobuf)<br/>With Deadline & Fallback"| Tracking
    Ticket -->|"SOAP (HTTP + XML)<br/>With Retry & Circuit Breaker"| Payment

    Tracking -.->|"Source of Truth"| Proto
    Ticket -.->|"Client Stub Copy"| Proto
    Payment -.->|"Contract-First WSDL Generation"| SOAP
```
### Sequence Diagrams for more details
#### ticketing
```mermaid
sequenceDiagram
    actor User as External Client
    participant GW as api-gateway (:8080)
    participant Ticket as ticket-service (:8081)<br/>[SOAP Client]
    participant Pay as payment-service (:8083)<br/>[SOAP Server]

    User->>GW: POST /api/v1/tickets (JSON)
    GW->>Ticket: Forward POST /api/v1/tickets
    Note over Ticket: 1. Save Ticket as PENDING
    Note over Ticket: 2. Marshal Java DTO -> SOAP XML
    Ticket->>Pay: POST http://payment-service:8083/ws (XML)
    alt Payment Approved
        Pay-->>Ticket: SOAP Response (APPROVED + paymentId)
        Note over Ticket: 3. Update Ticket to PAID
        Ticket-->>GW: 201 Created (JSON)
        GW-->>User: 201 Created (JSON)
    else Card Refused / Limit Exceeded
        Pay-->>Ticket: SOAP Fault (Error Code + Message)
        Note over Ticket: 3. Update Ticket to CANCELLED
        Ticket-->>GW: 422 Unprocessable Entity (JSON)
        GW-->>User: 422 Unprocessable Entity (JSON)
    end


```



#### Browse Lines & Check Next Bus (REST → gRPC)
```mermaid
sequenceDiagram
    actor User as External Client
    participant GW as api-gateway (:8080)
    participant Ticket as ticket-service (:8081)
    participant TDB as ticket-db (:5432)
    participant Track as tracking-service (:9090)

    User->>GW: GET /api/v1/lines
    GW->>Ticket: Route /api/v1/lines (with X-Correlation-Id)
    Ticket->>TDB: SELECT * FROM lines
    TDB-->>Ticket: List<Line>
    Ticket-->>GW: 200 OK (JSON Lines)
    GW-->>User: 200 OK (JSON Lines)

    User->>GW: GET /api/v1/lines/{id}/next-bus
    GW->>Ticket: Route GET /api/v1/lines/{id}/next-bus
    Note over Ticket: Attach gRPC deadline (2s)
    Ticket->>Track: gRPC GetLastPosition / StreamLinePositions
    Track-->>Ticket: Position (busId, lat, lon, speed)
    Note over Ticket: Compute ETA
    Ticket-->>GW: 200 OK (JSON NextBusDTO)
    GW-->>User: 200 OK (JSON NextBusDTO)

```




#### Successfu Ticket Purchase (Rest --> SOAP)

```mermaid
sequenceDiagram
    actor User as External Client
    participant GW as api-gateway (:8080)
    participant Ticket as ticket-service (:8081)
    participant TDB as ticket-db (:5432)
    participant Pay as payment-service (:8083)
    participant PDB as payment-db (:5432)

    User->>GW: POST /api/v1/tickets (lineId, passenger, cardNumber)
    GW->>Ticket: Forward POST /api/v1/tickets
    Ticket->>TDB: INSERT Ticket (status = PENDING)
    TDB-->>Ticket: ticketId
    Note over Ticket: Marshal Java DTO to SOAP XML
    Ticket->>Pay: SOAP AuthorizePayment (amount, cardNumber, ticketId)
    Pay->>PDB: INSERT Payment (status = APPROVED)
    PDB-->>Pay: paymentId
    Pay-->>Ticket: SOAP AuthorizePaymentResponse (APPROVED, paymentId)
    Ticket->>TDB: UPDATE Ticket SET status = PAID, paymentId = ...
    TDB-->>Ticket: Updated Ticket
    Ticket-->>GW: 201 Created + Location: /api/v1/tickets/{id}
    GW-->>User: 201 Created (JSON TicketResponse)

```




#### Buisness Rejection -SOAP Fault


```mermaid
sequenceDiagram
    actor User as External Client
    participant GW as api-gateway (:8080)
    participant Ticket as ticket-service (:8081)
    participant TDB as ticket-db (:5432)
    participant Pay as payment-service (:8083)

    User->>GW: POST /api/v1/tickets (refused test card)
    GW->>Ticket: Forward POST /api/v1/tickets
    Ticket->>TDB: INSERT Ticket (status = PENDING)
    Ticket->>Pay: SOAP AuthorizePayment (refused card)
    Note over Pay: Bank validation fails
    Pay-->>Ticket: SOAP Fault (Code: CARD_DECLINED, Msg: Insufficient funds)
    Note over Ticket: Catch SoapFaultClientException
    Ticket->>TDB: UPDATE Ticket SET status = CANCELLED
    Ticket-->>GW: 422 Unprocessable Entity (JSON ErrorDTO)
    GW-->>User: 422 Unprocessable Entity (Clear error message)

```




#### Payment Timeout, Retry & Circuit Breaker Fallback


```mermaid
sequenceDiagram
    actor User as External Client
    participant GW as api-gateway (:8080)
    participant Ticket as ticket-service (:8081)<br/>[Resilience4j]
    participant TDB as ticket-db (:5432)
    participant Pay as payment-service (:8083)

    User->>GW: POST /api/v1/tickets
    GW->>Ticket: Forward POST /api/v1/tickets
    Ticket->>TDB: INSERT Ticket (status = PENDING)

    loop Retry with Exponential Backoff (e.g., 3 attempts)
        Ticket->>Pay: SOAP AuthorizePayment
        Pay--xTicket: SocketTimeoutException / ConnectionRefused
    end

    Note over Ticket: Failure threshold reached -> Circuit Breaker OPENS
    Note over Ticket: Invoke fallbackAuthorizePayment()
    Ticket-->>GW: 503 Service Unavailable (Ticket kept PENDING, retry later)
    GW-->>User: 503 Service Unavailable (JSON Fallback Response)

```



####  Tracking Service Down — Graceful Degradation

```mermaid 
sequenceDiagram
    actor User as External Client
    participant GW as api-gateway (:8080)
    participant Ticket as ticket-service (:8081)<br/>[Resilience4j]
    participant Track as tracking-service (:9090)
    participant Pay as payment-service (:8083)

    User->>GW: GET /api/v1/lines/{id}/next-bus
    GW->>Ticket: Forward request
    Ticket->>Track: gRPC GetLastPosition (with deadline)
    Track--xTicket: StatusRuntimeException: UNAVAILABLE
    Note over Ticket: Invoke fallbackNextBus()
    Ticket-->>GW: 200 OK / 503 (Degraded: "Real-time position unavailable")
    GW-->>User: Degraded JSON response

    Note over User,Pay: Meanwhile, ticket purchases remain 100% operational
    User->>GW: POST /api/v1/tickets
    GW->>Ticket: Forward purchase request
    Ticket->>Pay: SOAP AuthorizePayment
    Pay-->>Ticket: SOAP Response (APPROVED)
    Ticket-->>GW: 201 Created (PAID)
    GW-->>User: 201 Created (PAID)

```


#### Bus GPS Telemetry & Live Streaming


```mermaid
sequenceDiagram
    participant Sim as Position Simulator<br/>(or grpcurl Client)
    participant Track as tracking-service (:9090)<br/>[@GrpcService]
    participant TrDB as tracking-db (:5432)

    rect rgb(235, 245, 255)
        Note over Sim,TrDB: Client-Streaming / Simulator Ingestion
        Sim->>Track: stream Position (busId, lineCode, lat, lon, timestamp)
        Track->>TrDB: UPSERT Bus lastPosition & INSERT history
        Track-->>Sim: Ack (receivedCount)
    end

    rect rgb(240, 255, 240)
        Note over Sim,TrDB: Server-Streaming Subscription
        Sim->>Track: StreamLinePositions(LineRequest: lineCode="L10")
        loop Every N seconds while stream active
            Track->>TrDB: SELECT latest positions for line L10
            TrDB-->>Track: List<Position>
            Track-->>Sim: stream Position update
        end
    end

```