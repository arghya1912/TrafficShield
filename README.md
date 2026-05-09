# TrafficShield — an API Gateway for Microservices

TrafficShield is a backend/system-design project built with **Kotlin**, **Spring Boot**, and **Maven**. It demonstrates practical microservice reliability patterns such as **load balancing**, **token-bucket rate limiting**, **circuit breaker management**, **service health visibility**, request metrics, and AI-style traffic summaries.

## Live Demo

Swagger UI:  
https://trafficshield-q9dw.onrender.com/swagger-ui/index.html

> 1. For local setup, I have used **PostgreSQL + Redis**.  
> 2. For live Render deployment, I have used **H2 + in-memory rate limiter fallback** because it is hosted on a free environment.    
> 3. Since it is hosted in Render free tier, it may take some time to load.

## Features

- Configurable API Gateway for routing requests to backend service instances
- Round-robin and weighted round-robin load balancing
- Token-bucket rate limiting
- Custom circuit breaker with `CLOSED`, `OPEN`, and `HALF_OPEN` states
- Service health and circuit breaker inspection APIs
- Request metrics grouped by service and outcome type
- AI-style traffic summary generated from gateway metrics
- Swagger-based live API demo
- Docker Compose setup for local execution


## Architecture

```text
Client
  |
  v
TrafficShield Gateway
  |
  |-- Rate Limiter
  |-- Load Balancer
  |-- Circuit Breaker
  |-- Metrics Recorder
  |-- AI-style Summary Generator
  |
  v
Mock Backend Service Instances
```

## Main APIs

### Demo

```http
POST /demo/reset
POST /demo/run/basic
```

### Proxy

```http
GET /proxy/{serviceName}/**
```

Example:

```http
GET /proxy/payment-service/api/payments/501
Header: client-id: demo-user
```

### Admin / Observability

```http
GET /admin/metrics/summary
GET /admin/metrics/outcomes
GET /admin/services/status
GET /admin/circuit-breakers
GET /admin/rate-limits/{clientId}/{serviceName}
GET /admin/ai/traffic-summary
```

## Recommended Demo Flow

Open Swagger and run:

```text
1. POST /demo/reset
2. POST /demo/run/basic
3. GET /admin/metrics/summary
4. GET /admin/metrics/outcomes
5. GET /admin/services/status
6. GET /admin/circuit-breakers
7. GET /admin/ai/traffic-summary
```

## Local Setup

### Prerequisites

- Java 17
- Maven
- Docker
- Docker Compose

### How to run Locally

```bash
git clone https://github.com/arghya1912/TrafficShield.git
cd TrafficShield
docker compose up --build
```

Open Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

## Local vs Live Deployment

| Environment | Database | Rate Limiter |
|---|---|---|
| Local Docker | PostgreSQL | Redis |
| Render Live Demo | H2 | In-memory fallback |
