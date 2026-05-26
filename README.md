![Java](https://img.shields.io/badge/Java-21-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Kubernetes](https://img.shields.io/badge/Kubernetes-CKAD-blue)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-black)
![License](https://img.shields.io/badge/Status-Active-success)

# Atlas Commerce Platform

## Overview

Atlas Commerce Platform is a cloud-native microservices system built with Spring Boot, Kubernetes, Kafka, RabbitMQ, PostgreSQL, Redis, and modern DevOps tooling.

The project was designed to simulate a production-grade e-commerce backend focused on:

* Distributed systems
* Event-driven architecture
* Cloud-native deployment patterns
* Kubernetes operations
* Security
* Observability
* Async orchestration
* Saga-like workflows

The platform demonstrates how multiple independent services collaborate asynchronously through Kafka events while running inside Kubernetes.

---

# Architecture

## High-Level Architecture

```text
                        ┌─────────────────────┐
                        │     API Gateway     │
                        │ Spring Cloud Gateway│
                        └──────────┬──────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
        ▼                          ▼                          ▼
 ┌─────────────┐          ┌─────────────┐            ┌─────────────┐
 │ auth-service│          │catalog-serv.│            │order-service│
 └──────┬──────┘          └─────────────┘            └──────┬──────┘
        │                                                   │
        │                                                   ▼
        │                                            Kafka Events
        │                                                   │
        │                                                   ▼
        │        ┌──────────────┬──────────────┬──────────────┐
        │        ▼              ▼              ▼              ▼
        │  inventory-service payment-service shipping-service audit-service
        │        │              │              │
        │        ▼              ▼              ▼
        │  inventory-events payment-events shipping-events
        │
        ▼
 Redis / JWT blacklist
```

---

# Event-Driven Saga Flow

## Happy Path

```text
ORDER_CREATED
    ↓
INVENTORY_RESERVED
    ↓
PAYMENT_COMPLETED
    ↓
SHIPPING_CREATED
    ↓
NOTIFICATION_SENT
```

## Failure Path

```text
ORDER_CREATED
    ↓
INVENTORY_FAILED
    ↓
ORDER_FAILED
```

---

# Kafka Topics

| Topic            | Purpose                         |
| ---------------- | ------------------------------- |
| order-events     | Order creation events           |
| inventory-events | Inventory reservation / failure |
| payment-events   | Payment processing results      |
| shipping-events  | Shipment creation events        |
|                  |                                 |

---

# Services

## auth-service

Authentication and authorization service.

Features:

* JWT authentication
* Access + refresh tokens
* Token revocation
* BCrypt password hashing
* Login protection / rate limiting
* Redis blacklist integration

---

## order-service

Responsible for:

* Order creation
* Saga orchestration
* Kafka event publishing
* Order state transitions

Supported states:

```text
PENDING
RESERVED
PAID
SHIPPED
FAILED
```

The service consumes async events and updates the order lifecycle.

---

## inventory-service

Responsible for:

* Inventory validation
* Inventory reservation
* Inventory failure handling
* Publishing inventory events

Publishes:

```text
INVENTORY_RESERVED
INVENTORY_FAILED
```

---

## payment-service

Responsible for:

* Payment simulation
* Async payment processing
* Publishing payment events

Publishes:

```text
PAYMENT_COMPLETED
PAYMENT_FAILED
```

---

## shipping-service

Responsible for:

* Shipment creation
* Tracking number generation
* Carrier assignment

Publishes:

```text
SHIPPING_CREATED
```

---

## notification-service

Responsible for:

* Async notifications
* Shipment notifications
* Event-driven communication

---

## audit-service

Consumes Kafka events from multiple services and persists audit logs.

Audited flows include:

* Order creation
* Inventory reservation/failure
* Payments
* Shipments
* Security events

---

# Security

## JWT Authentication

Atlas uses stateless JWT authentication.

Features:

* Access tokens
* Refresh tokens
* Redis token blacklist
* Secure logout
* Role-based authorization
* Spring Security Resource Server

---

# Kubernetes Deployment

Each microservice includes:

* Deployment
* Service
* ConfigMap
* Secret
* Health probes
* Resource requests/limits
* Docker image

Infrastructure components:

* PostgreSQL StatefulSets
* Kafka
* RabbitMQ
* Redis
* NGINX Ingress

---

# Kubernetes Features

## Health Probes

```text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
```

## Resource Management

Configured:

* CPU requests/limits
* Memory requests/limits
* JVM container awareness

## Kustomize

Environment overlays are managed using Kustomize.

---

# Observability

## Current Stack

* Spring Boot Actuator
* Prometheus
* Grafana
* Kubernetes probes
* Structured logging

## Planned

* OpenTelemetry
* Tempo / Jaeger
* Loki
* Distributed tracing
* Centralized logging

---

# Messaging Architecture

## Kafka

Used for:

* Saga orchestration
* Async communication
* Event propagation
* Failure handling

## RabbitMQ

Used for:

* Traditional message broker experimentation
* Alternative async integration patterns

---

# CI/CD

## GitHub Actions

Current pipeline:

* Maven build
* Unit tests
* Docker build
* Docker Hub push

Planned:

* Kubernetes deployment
* ArgoCD
* EKS rollout
* Helm charts

---

# Local Development

## Docker Compose

```bash
docker compose up --build
```

---

# Kubernetes Deployment

## Apply manifests

```bash
kubectl apply -k platform/k8s/overlays/local
```

## Verify pods

```bash
kubectl get pods -n atlas
```

## Port-forward example

```bash
kubectl port-forward -n atlas svc/order-service 8083:80
```

---

# Example Event Payloads

## ORDER_CREATED

```json
{
  "orderId": 23,
  "userId": 1,
  "currency": "EUR",
  "items": [
    {
      "productId": 101,
      "quantity": 2,
      "unitPrice": 19.99
    }
  ]
}
```

---

## INVENTORY_FAILED

```json
{
  "orderId": 27,
  "status": "FAILED",
  "reason": "Inventory validation failed"
}
```

---

# Design Decisions

## Why Event-Driven?

The platform intentionally uses asynchronous orchestration to simulate real distributed systems.

Benefits:

* Loose coupling
* Scalability
* Fault isolation
* Async processing
* Independent service ownership

---

## Database Per Service

Each microservice owns its own database.

Benefits:

* Strong isolation
* Independent scaling
* Service autonomy
* Reduced coupling

---

# Current Technical Stack

| Technology      | Usage                     |
| --------------- | ------------------------- |
| Java 21         | Backend language          |
| Spring Boot     | Microservices             |
| Spring Security | Authentication            |
| PostgreSQL      | Persistence               |
| Redis           | Token blacklist / caching |
| Kafka           | Event streaming           |
| RabbitMQ        | Messaging                 |
| Docker          | Containers                |
| Kubernetes      | Orchestration             |
| Kustomize       | Manifest management       |
| Prometheus      | Metrics                   |
| Grafana         | Dashboards                |
| GitHub Actions  | CI/CD                     |

---

# Future Roadmap

## Infrastructure

* EKS deployment
* ArgoCD GitOps
* Helm charts
* HPA autoscaling
* Cluster autoscaler

## Observability

* OpenTelemetry
* Distributed tracing
* Loki logging
* Advanced dashboards

## Resilience

* DLQ topics
* Retry handling
* Resilience4j
* Circuit breakers
* Idempotency

## Security

* Keycloak / OpenID Connect
* Vault integration
* External Secrets
* NetworkPolicies

---

# Learning Outcomes

This project demonstrates:

* Distributed systems design
* Event-driven microservices
* Saga-like orchestration
* Kubernetes production patterns
* Cloud-native architecture
* Spring Security in distributed systems
* Async communication with Kafka
* DevOps and CI/CD practices
* Failure handling strategies

---

# Author

Built as a large-scale portfolio project focused on backend engineering, distributed systems, Kubernetes, and cloud-native architecture.
