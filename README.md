![Java](https://img.shields.io/badge/Java-21-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Kubernetes](https://img.shields.io/badge/Kubernetes-CKAD-blue)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-black)
![License](https://img.shields.io/badge/Status-Active-success)

# Atlas Commerce Platform

Cloud-native microservices platform built with **Spring Boot** and **Kubernetes**, designed to demonstrate production-grade backend, security, and DevOps practices.

---

## 🚀 Overview

Atlas Commerce is a distributed system composed of independent microservices that handle authentication, product catalog, and order processing.

It showcases:

* Stateless authentication with JWT
* Refresh token flow with persistence
* Immediate logout using token blacklist
* Inter-service communication with RabbitMQ
* Kubernetes-native deployment
* Secure configuration with Secrets and ConfigMaps

---

## 🏗️ Architecture

```text
Client
  │
  ├── Auth Service (JWT + Refresh Tokens)
  │        │
  │        └── PostgreSQL (users, refresh tokens)
  │
  ├── Catalog Service
  │        │
  │        └── PostgreSQL (products)
  │
  ├── Order Service
  │        │
  │        ├── PostgreSQL (orders)
  │        └── RabbitMQ (event-driven communication)
  │
  └── Redis (token blacklist / caching)
```

---

## 🧩 Services

### 🔐 auth-service

* User registration and login
* JWT (access + refresh tokens)
* Token refresh endpoint
* Logout with token revocation (blacklist)
* Rate limiting / brute-force protection

### 📦 catalog-service

* Product management (CRUD)
* JWT validation (resource server)
* Independent from auth-service

### 🧾 order-service

* Order creation
* Event publishing to RabbitMQ
* JWT validation
* Designed for async processing

---

## 🔒 Security

* Stateless authentication using JWT
* Access tokens with short expiration
* Refresh tokens stored in database
* Token blacklist for immediate logout
* Password hashing with BCrypt
* Secrets managed via Kubernetes Secrets
* Rate limiting on login endpoint

---

## ☁️ Kubernetes Deployment

Each service is deployed with:

* **Deployment / StatefulSet**
* **Service (ClusterIP)**
* **ConfigMap (non-sensitive config)**
* **Secret (credentials, JWT)**
* **Probes (startup, liveness, readiness)**
* **Resource limits**
* **NGINX Ingress**


### Infrastructure components:

* PostgreSQL (StatefulSet)
* Redis (blacklist / caching)
* RabbitMQ (event broker)
* NGINX Ingress Controller

---

## 📊 Observability

Atlas Commerce includes production-style observability tooling:

* Prometheus metrics scraping
* Spring Boot Actuator integration
* Grafana dashboards
* Kubernetes health probes
* Service-level monitoring

Future additions:

* OpenTelemetry
* Distributed tracing
* Loki log aggregation

---

## 🔄 CI/CD

GitHub Actions pipelines validate:

* Maven build
* Unit tests
* Docker image builds

Future pipeline stages:

* Docker Hub publishing
* Kubernetes deployment
* EKS rollout

---

## 🐳 Run Locally (Docker Compose)

```bash
docker-compose up --build
```

---

## ☸️ Deploy to Kubernetes

```bash
kubectl apply -f platform/k8s/
kubectl get pods -n atlas
```

Port-forward example:

```bash
kubectl port-forward -n atlas svc/auth-service 8081:8081
```

---

## 🧪 API Examples

### Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@atlas.com","password":"123456"}'
```

---

### Access protected endpoint

```bash
curl http://localhost:8082/api/v1/products \
  -H "Authorization: Bearer ACCESS_TOKEN"
```

---

### Refresh token

```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"REFRESH_TOKEN"}'
```

---

## ⚡ Design Decisions

* **Stateless authentication** for scalability
* **Blacklist with Redis** to support immediate logout
* **Microservice isolation** (each service owns its data)
* **Event-driven architecture** for order processing
* **Monorepo** for simplified development and visibility

---

## 📈 Future Improvements

* API Gateway (NGINX / Spring Cloud Gateway)
* Distributed tracing (OpenTelemetry)
* Centralized logging (ELK / Loki)
* External Secrets (AWS Secrets Manager / Vault)
* Horizontal Pod Autoscaling (HPA)
* CI/CD pipelines (GitHub Actions)

---

## 🧠 Key Learning Outcomes

This project demonstrates:

* Real-world Spring Security implementation
* Secure JWT handling in distributed systems
* Kubernetes production patterns
* Microservices architecture design
* DevOps mindset and tooling

---

## 📬 Author

Built as a portfolio project to demonstrate backend and cloud-native expertise.

---

## ⭐ Final Note

This is not a toy project.
It is designed to reflect **real production challenges and solutions**.

---
