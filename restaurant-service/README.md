# QuickBite — restaurant-service

**Port:** `8082` | **DB:** `quickbite_restaurant` (MySQL) | **Cache:** Redis

---

## Project Structure

```
restaurant-service/
├── src/main/java/com/quickbite/restaurant/
│   ├── RestaurantServiceApplication.java      ← Main class (@EnableFeignClients)
│   ├── constants/
│   │   └── AppConstants.java                  ← All string constants, roles, messages
│   ├── entity/
│   │   ├── Restaurant.java                    ← Core domain entity
│   │   ├── MenuCategory.java                  ← Menu sections (Starters, Mains...)
│   │   └── MenuItem.java                      ← Individual dishes
│   ├── repository/
│   │   ├── RestaurantRepository.java          ← Haversine geo-query, approval, search
│   │   ├── MenuCategoryRepository.java
│   │   └── MenuItemRepository.java
│   ├── service/
│   │   ├── RestaurantService.java             ← Interface (contract)
│   │   └── impl/RestaurantServiceImpl.java    ← Full business logic + Redis cache
│   ├── controller/
│   │   └── RestaurantController.java          ← All REST endpoints
│   ├── dto/
│   │   ├── request/                           ← Input validation DTOs
│   │   └── response/                          ← Output DTOs (ApiResponse, PagedResponse)
│   ├── config/
│   │   ├── SecurityConfig.java                ← JWT + role-based access rules
│   │   ├── JwtAuthFilter.java                 ← Token parsing filter
│   │   ├── JwtUtils.java                      ← Token validation (same secret as auth-service)
│   │   ├── AppConfig.java                     ← ModelMapper, Redis, RestTemplate beans
│   │   └── SwaggerConfig.java                 ← OpenAPI 3.0 docs
│   ├── feign/
│   │   ├── NotificationClient.java            ← Feign to notification-service
│   │   └── NotificationClientFallback.java    ← Graceful degradation fallback
│   └── exception/
│       ├── GlobalExceptionHandler.java         ← Unified error responses
│       ├── ResourceNotFoundException.java
│       ├── UnauthorizedException.java
│       └── DuplicateResourceException.java
├── src/main/resources/
│   ├── application.yml                        ← All configs (DB, Redis, Eureka, JWT)
│   └── schema.sql                             ← Reference SQL + sample data
└── pom.xml                                    ← All dependencies
```

---

## API Endpoints

### Public (No JWT needed — Guests + Customers)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/restaurants` | All approved restaurants (paginated) |
| GET | `/api/v1/restaurants/{id}` | Restaurant detail + full menu |
| GET | `/api/v1/restaurants/search?keyword=pizza` | Global search |
| GET | `/api/v1/restaurants/nearby?lat=28.6&lng=77.2&radius=5` | Geo-proximity |
| GET | `/api/v1/restaurants/cuisine/{cuisine}` | Filter by cuisine |
| GET | `/api/v1/restaurants/city/{city}` | Filter by city |
| GET | `/api/v1/restaurants/{id}/menu` | Full menu items |
| GET | `/api/v1/restaurants/{id}/categories` | Menu categories |
| GET | `/api/v1/restaurants/{id}/items/veg` | Veg only items |
| GET | `/api/v1/restaurants/{id}/items/search?keyword=paneer` | Search menu |
| GET | `/api/v1/restaurants/items/{itemId}` | Get item by id (used by cart-service) |

### OWNER (JWT required, role=OWNER)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/restaurants` | Register restaurant |
| GET | `/api/v1/restaurants/my` | Get my restaurants |
| PUT | `/api/v1/restaurants/{id}` | Update restaurant |
| PUT | `/api/v1/restaurants/{id}/toggle-open` | Open/Close restaurant |
| POST | `/api/v1/restaurants/{id}/categories` | Add menu category |
| PUT | `/api/v1/restaurants/{id}/categories/{catId}` | Update category |
| DELETE | `/api/v1/restaurants/{id}/categories/{catId}` | Delete category |
| POST | `/api/v1/restaurants/{id}/categories/{catId}/items` | Add menu item |
| PUT | `/api/v1/restaurants/{id}/items/{itemId}` | Update item |
| PUT | `/api/v1/restaurants/{id}/items/{itemId}/toggle-availability` | Toggle stock |
| DELETE | `/api/v1/restaurants/{id}/items/{itemId}` | Delete item |

### ADMIN (JWT required, role=ADMIN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/restaurants/pending` | Pending approval list |
| PUT | `/api/v1/restaurants/{id}/approve` | Approve/Reject |
| DELETE | `/api/v1/restaurants/{id}` | Soft delete |

### Internal (Service-to-Service)

| Method | Endpoint | Called By |
|--------|----------|-----------|
| PUT | `/api/v1/restaurants/{id}/rating` | review-service (after each review) |

---

## Sample API Calls (cURL)

### Register Restaurant (OWNER)
```bash
curl -X POST http://localhost:8082/api/v1/restaurants \
  -H "Authorization: Bearer <owner-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Punjabi Dhaba",
    "cuisine": "North Indian",
    "address": "12 Connaught Place",
    "city": "Delhi",
    "latitude": 28.6315,
    "longitude": 77.2167,
    "phone": "9811111111",
    "deliveryRadius": 8.0,
    "minOrderAmount": 100.0,
    "estimatedDeliveryMin": 25
  }'
```

### Find Nearby Restaurants
```bash
curl "http://localhost:8082/api/v1/restaurants/nearby?lat=28.6139&lng=77.2090&radius=5"
```

### Admin Approve Restaurant
```bash
curl -X PUT http://localhost:8082/api/v1/restaurants/1/approve \
  -H "Authorization: Bearer <admin-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"status": "APPROVED"}'
```

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.x running on port 3306
- Redis running on port 6379
- eureka-service running on port 8761
- auth-service running on port 8081

### Steps
```bash
cd restaurant-service
mvn clean package -DskipTests
java -jar target/restaurant-service-1.0.0.jar
```

### Access
- API: http://localhost:8082/api/v1/restaurants
- Swagger: http://localhost:8082/swagger-ui.html
- Actuator: http://localhost:8082/actuator/health

---

## ⚠️ CHANGES WHEN OTHER SERVICES ARE ADDED

### When `menu-service` is added (if separated later)
- Currently menu (categories + items) lives INSIDE restaurant-service.
- If menu is extracted to its own service: remove MenuCategory, MenuItem entities from here.
- restaurant-service will then Feign-call menu-service for `/menu`.

### When `order-service` is added
**NO change in restaurant-service code needed.**
- order-service will call `GET /api/v1/restaurants/items/{itemId}` to validate items.
- order-service will call `GET /api/v1/restaurants/{id}` to get restaurant details.
- These public endpoints already exist.

### When `review-service` is added
**ONE endpoint already prepared:**
- `PUT /api/v1/restaurants/{id}/rating` — review-service will call this after each review.
- Add `X-Internal-Service: quickbite-internal` header in review-service's Feign client.

### When `delivery-service` is added
- No changes needed in restaurant-service.
- delivery-service will query restaurant lat/lng from `GET /api/v1/restaurants/{id}`.

### When `notification-service` is added
- NotificationClient Feign interface already exists and is ready.
- Just ensure notification-service registers with Eureka as "notification-service".
- The fallback (NotificationClientFallback) ensures graceful degradation.

### api-gateway changes when restaurant-service is added:
```yaml
# In api-gateway application.yml — add this route:
spring:
  cloud:
    gateway:
      routes:
        - id: restaurant-service
          uri: lb://restaurant-service
          predicates:
            - Path=/api/v1/restaurants/**
          filters:
            - StripPrefix=0
```

### auth-service changes: NONE
- auth-service issues JWT. restaurant-service validates it independently.
- No new roles needed for restaurant-service.

---

## Database Schema

```
restaurants (1) ──────── (many) menu_categories
                                      │
                                  (many)
                                      │
                               menu_items
```

All tables use soft-delete (`is_active = false`) — no hard deletes.

---

## Tech Stack Used
- Spring Boot 3.2.4
- Spring Security (JWT — stateless)
- Spring Data JPA (MySQL)
- Spring Cloud Netflix Eureka (Client)
- Spring Cloud OpenFeign (notification-service calls)
- Spring Cache + Redis (restaurant listing cache)
- Lombok
- ModelMapper
- Springdoc OpenAPI 3.0 (Swagger UI)
- JUnit 5 + Mockito (Tests)
