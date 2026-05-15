# QuickBite Backend

QuickBite Backend is a Spring Boot microservices platform for food delivery. It includes authentication, restaurant discovery, cart and order flow, payment handling, delivery agent management, reviews, notifications, service discovery, and an API gateway.

## Architecture Overview

The repository is organized as separate Spring Boot services:

| Module | Port | Responsibility |
|---|---:|---|
| `api-gateway` | `8080` | Single entry point, routing, filters, circuit breakers |
| `eureka-server` | `8761` | Service registry and discovery |
| `auth-service` | `8081` | Register, login, JWT, profile, admin user management |
| `restaurant-service` | `8082` | Restaurants, menu, categories, items, approvals, ratings |
| `order-service` | `8085` | Cart and order management, analytics, order lifecycle |
| `payment-service` | `8084` | Payments, wallet, Razorpay flow, notifications |
| `delivery-service` | `8087` | Delivery agent registration, tracking, assignment, earnings |

## Logical Services Inside Modules

Some logical services are implemented inside existing modules instead of separate Maven projects:

- `cart` is handled inside `order-service`
- `review-service` is handled inside `restaurant-service`
- `notification-service` is handled inside `payment-service`
- `menu` APIs are part of `restaurant-service`

## Service Flow

1. `auth-service` issues and validates JWTs.
2. `restaurant-service` manages restaurant onboarding, menu data, and ratings.
3. `order-service` manages cart and order lifecycle.
4. `payment-service` processes payments, wallet top-up, and sends notifications.
5. `delivery-service` assigns and tracks delivery agents.
6. `api-gateway` exposes all services through one base URL and forwards requests through Eureka.

## Gateway Routes

Public access is normally done through the gateway:

| Gateway Path | Forwards To |
|---|---|
| `/api/v1/auth/**` | `auth-service` |
| `/api/v1/restaurants/**` | `restaurant-service` |
| `/api/v1/menu/**` | `restaurant-service` |
| `/api/v1/cart/**` | `order-service` |
| `/api/v1/orders/**` | `order-service` |
| `/api/v1/payments/**` | `payment-service` |
| `/api/v1/wallet/**` | `payment-service` |
| `/api/v1/agents/**` | `delivery-service` |
| `/api/v1/reviews/**` | `restaurant-service` |
| `/api/v1/notifications/**` | `payment-service` |

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- Redis 6379 for `restaurant-service`
- RabbitMQ 5672 for order/payment/delivery flows
- Eureka server running on `8761`

## Local Setup

### 1. Start Eureka Server

```bash
cd eureka-server
mvn spring-boot:run
```

### 2. Start Core Services

Run each service in its own terminal:

```bash
cd auth-service
mvn spring-boot:run
```

```bash
cd restaurant-service
mvn spring-boot:run
```

```bash
cd order-service
mvn spring-boot:run
```

```bash
cd payment-service
mvn spring-boot:run
```

```bash
cd delivery-service
mvn spring-boot:run
```

### 3. Start API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

## Suggested Startup Order

1. `eureka-server`
2. `auth-service`
3. `restaurant-service`
4. `order-service`
5. `payment-service`
6. `delivery-service`
7. `api-gateway`

## Database Names

The services use these MySQL databases by default:

- `quickbite_auth`
- `quickbite_restaurant`
- `quickbite_orders`
- `quickbite_payment`
- `quickbite_delivery`

## Important Base URLs

After all services are up, the important local URLs are:

- Gateway: `http://localhost:8080`
- Eureka Dashboard: `http://localhost:8761`
- Auth Swagger: `http://localhost:8081/swagger-ui.html`
- Restaurant Swagger: `http://localhost:8082/swagger-ui.html`
- Order Swagger: `http://localhost:8085/swagger-ui.html`
- Payment Swagger: `http://localhost:8084/swagger-ui.html`
- Delivery Swagger: `http://localhost:8087/swagger-ui.html`

## Key API Groups

### Auth Service

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/validate-token`
- `GET /api/v1/auth/profile`
- `PUT /api/v1/auth/profile`
- `PUT /api/v1/auth/password`
- `GET /api/v1/auth/admin/users`

### Restaurant Service

- `GET /api/v1/restaurants`
- `GET /api/v1/restaurants/{id}`
- `GET /api/v1/restaurants/search`
- `GET /api/v1/restaurants/nearby`
- `POST /api/v1/restaurants`
- `PUT /api/v1/restaurants/{id}`
- `PUT /api/v1/restaurants/{id}/approve`
- `PUT /api/v1/restaurants/{id}/rating`
- `GET /api/v1/restaurants/{id}/menu`
- `GET /api/v1/restaurants/items/{itemId}`

### Order Service

- `GET /orders/{orderId}`
- `POST /orders`
- `PUT /orders/{orderId}/status`
- `PUT /orders/{orderId}/accept`
- `PUT /orders/{orderId}/cancel`
- `POST /orders/{orderId}/reorder`
- `GET /orders/restaurant/{restaurantId}/analytics`
- Cart APIs under `/cart/**`

### Payment Service

- `POST /api/v1/payments/process`
- `GET /api/v1/payments/{id}`
- `POST /api/v1/payments/{id}/refund`
- `GET /api/v1/wallet/{customerId}`
- `POST /api/v1/wallet/topup`
- `POST /api/v1/payments/razorpay/create-order`
- `POST /api/v1/payments/razorpay/verify`
- Notification APIs under `/api/v1/notifications/**`

### Delivery Service

- `POST /api/v1/agents/register`
- `GET /api/v1/agents/my`
- `PUT /api/v1/agents/{agentId}/location`
- `PUT /api/v1/agents/{agentId}/availability`
- `PUT /api/v1/agents/{agentId}/verify`
- `POST /api/v1/agents/{agentId}/assign-order`
- `POST /api/v1/agents/{agentId}/complete/{orderId}`
- `GET /api/v1/agents/{agentId}/earnings`

### Review Service

- `POST /api/v1/reviews`
- `GET /api/v1/reviews/restaurant/{restaurantId}`
- `GET /api/v1/reviews/customer/{customerId}`
- `GET /api/v1/reviews/agent/{agentId}`
- `PUT /api/v1/reviews/{reviewId}`
- `DELETE /api/v1/reviews/{reviewId}`
- `GET /api/v1/reviews/restaurant/{restaurantId}/average`

## Environment Variables

Each service reads from its own `.env` or `.env.properties` file if present. Common variables include:

- `QUICKBITE_EUREKA_URL`
- `QUICKBITE_AUTH_DB_URL`
- `QUICKBITE_RESTAURANT_DB_URL`
- `QUICKBITE_ORDER_DB_URL`
- `QUICKBITE_PAYMENT_DB_URL`
- `QUICKBITE_DELIVERY_DB_URL`
- `QUICKBITE_RABBITMQ_HOST`
- `QUICKBITE_REDIS_HOST`
- `QUICKBITE_AUTH_JWT_SECRET`
- `QUICKBITE_RESTAURANT_JWT_SECRET`
- `QUICKBITE_ORDER_JWT_SECRET`
- `QUICKBITE_PAYMENT_JWT_SECRET`
- `QUICKBITE_DELIVERY_JWT_SECRET`
- `QUICKBITE_GATEWAY_JWT_SECRET`

## Build All Services

If you want to compile everything before pushing:

```bash
cd auth-service && mvn clean test
cd ../restaurant-service && mvn clean test
cd ../order-service && mvn clean test
cd ../payment-service && mvn clean test
cd ../delivery-service && mvn clean test
cd ../api-gateway && mvn clean test
cd ../eureka-server && mvn clean test
```

## Render Deployment

Use this flow to deploy the backend on Render as separate services.

1. Push the backend repo to GitHub.
2. Deploy `eureka-server` first.
3. In Render, create a new `Web Service` from the repo and point it to `QuickBite-Backend/eureka-server`.
4. Choose `Docker` as the runtime. Render will use the `Dockerfile` in that folder.
5. Use the repository root as the build context so the Dockerfile can access `eureka-server/src`.
5. Set these environment variables for Eureka:
   - `PORT` is managed by Render automatically.
   - `EUREKA_CLIENT_REGISTER_WITH_EUREKA=false`
   - `EUREKA_CLIENT_FETCH_REGISTRY=false`
6. Deploy and copy the public Render URL, for example `https://quickbite-eureka-server.onrender.com`.
7. Deploy the remaining services one by one using the same repo and their own folders:
   - `api-gateway`
   - `auth-service`
   - `restaurant-service`
   - `order-service`
   - `payment-service`
   - `delivery-service`
8. For every service except Eureka, set `QUICKBITE_EUREKA_URL` to the Eureka public URL plus `/eureka/`.
9. Add the required database, RabbitMQ, Redis, JWT, and Razorpay secrets in each service.
10. After deploy, point the frontend to the Render API Gateway URL.

### Suggested Render Service Order

1. `eureka-server`
2. `auth-service`
3. `restaurant-service`
4. `order-service`
5. `payment-service`
6. `delivery-service`
7. `api-gateway`

### Minimum Env Vars By Service

`eureka-server`

- `EUREKA_CLIENT_REGISTER_WITH_EUREKA=false`
- `EUREKA_CLIENT_FETCH_REGISTRY=false`

`auth-service`

- `QUICKBITE_EUREKA_URL`
- `QUICKBITE_AUTH_DB_URL`
- `QUICKBITE_AUTH_DB_USERNAME`
- `QUICKBITE_AUTH_DB_PASSWORD`
- `QUICKBITE_AUTH_JWT_SECRET`
- `QUICKBITE_FRONTEND_URL`
- `QUICKBITE_GOOGLE_CLIENT_ID`
- `QUICKBITE_GOOGLE_CLIENT_SECRET`

`restaurant-service`

- `QUICKBITE_EUREKA_URL`
- `QUICKBITE_RESTAURANT_DB_URL`
- `QUICKBITE_RESTAURANT_DB_USERNAME`
- `QUICKBITE_RESTAURANT_DB_PASSWORD`
- `QUICKBITE_RESTAURANT_JWT_SECRET`
- `QUICKBITE_RABBITMQ_HOST`
- `QUICKBITE_RABBITMQ_USERNAME`
- `QUICKBITE_RABBITMQ_PASSWORD`
- `QUICKBITE_REDIS_HOST`
- `QUICKBITE_REDIS_PORT`

`order-service`

- `QUICKBITE_EUREKA_URL`
- `QUICKBITE_ORDER_DB_URL`
- `QUICKBITE_ORDER_DB_USERNAME`
- `QUICKBITE_ORDER_DB_PASSWORD`
- `QUICKBITE_ORDER_JWT_SECRET`
- `QUICKBITE_RABBITMQ_HOST`
- `QUICKBITE_RABBITMQ_USERNAME`
- `QUICKBITE_RABBITMQ_PASSWORD`

`payment-service`

- `QUICKBITE_EUREKA_URL`
- `QUICKBITE_PAYMENT_DB_URL`
- `QUICKBITE_PAYMENT_DB_USERNAME`
- `QUICKBITE_PAYMENT_DB_PASSWORD`
- `QUICKBITE_PAYMENT_JWT_SECRET`
- `QUICKBITE_RABBITMQ_HOST`
- `QUICKBITE_RABBITMQ_USERNAME`
- `QUICKBITE_RABBITMQ_PASSWORD`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `RAZORPAY_WEBHOOK_SECRET`
- `QUICKBITE_MAIL_USERNAME`
- `QUICKBITE_MAIL_PASSWORD`

`delivery-service`

- `QUICKBITE_EUREKA_URL`
- `QUICKBITE_DELIVERY_DB_URL`
- `QUICKBITE_DELIVERY_DB_USERNAME`
- `QUICKBITE_DELIVERY_DB_PASSWORD`
- `QUICKBITE_DELIVERY_JWT_SECRET`
- `QUICKBITE_RABBITMQ_HOST`
- `QUICKBITE_RABBITMQ_USERNAME`
- `QUICKBITE_RABBITMQ_PASSWORD`

`api-gateway`

- `QUICKBITE_EUREKA_URL`
- `QUICKBITE_GATEWAY_JWT_SECRET`

## Notes

- All services register with Eureka for discovery.
- The gateway uses route forwarding and circuit breakers.
- MySQL schemas are created automatically with `ddl-auto=update` in local development.
- Swagger is available on each service that exposes OpenAPI configuration.

## Repository Status

This README is designed for the current multi-service backend layout and is ready to be pushed to `main`.

# Merged with main


