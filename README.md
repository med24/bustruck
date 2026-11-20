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
