# QuickBite — Delivery Service

## Overview
PDF Section 4.7 — DeliveryAgent-Service manages agent profiles, GPS tracking,
order assignment, admin verification, and earnings.

---

## Service Info

| Property       | Value                              |
|----------------|------------------------------------|
| Port           | **8087**                           |
| App name       | `delivery-service`                 |
| Base package   | `com.quickbite.delivery_service`   |
| Database       | `quickbite_delivery` (MySQL)       |
| Gateway route  | `/api/v1/agents/**`                |

---

## Startup Order
```
1. eureka-server    :8761
2. auth-service     :8081
3. api-gateway      :8080
4. restaurant-service :8082
5. cart-service     :8083
6. delivery-service :8087   ← this service
```

---

## Run
```bash
# Create DB (auto-created by ddl-auto=update)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS quickbite_delivery;"

# Build
mvn clean install -DskipTests

# Run
mvn spring-boot:run
```

---

## API Endpoints

| Method | Endpoint                                  | Role         | Description                          |
|--------|-------------------------------------------|--------------|--------------------------------------|
| POST   | /api/v1/agents/register                   | AGENT        | Register new delivery agent          |
| GET    | /api/v1/agents/my                         | AGENT        | Get own profile (userId from JWT)    |
| GET    | /api/v1/agents/{agentId}                  | AGENT/ADMIN  | Get agent by ID                      |
| GET    | /api/v1/agents/nearby?lat=&lng=&radius=   | Internal     | Find nearby available agents         |
| PUT    | /api/v1/agents/{agentId}/location         | AGENT        | Update live GPS location             |
| PUT    | /api/v1/agents/{agentId}/availability     | AGENT        | Toggle online/offline                |
| PUT    | /api/v1/agents/{agentId}/verify           | ADMIN        | Verify/Reject/Suspend agent          |
| POST   | /api/v1/agents/{agentId}/assign-order     | Internal     | Assign order to agent                |
| POST   | /api/v1/agents/{agentId}/complete/{ordId} | AGENT        | Mark delivery complete               |
| PUT    | /api/v1/agents/{agentId}/rating           | Internal     | Update avg rating (review-service)   |
| GET    | /api/v1/agents/{agentId}/location         | Any (auth)   | Get live location (customer tracking)|
| GET    | /api/v1/agents/{agentId}/earnings         | AGENT/ADMIN  | Earnings + stats summary             |
| GET    | /api/v1/agents/active                     | ADMIN        | Agents with active deliveries        |
| GET    | /api/v1/agents/all                        | ADMIN        | All delivery agents                  |
| GET    | /api/v1/agents/status/{status}            | ADMIN        | Filter by PENDING/VERIFIED/SUSPENDED |
| DELETE | /api/v1/agents/{agentId}                  | ADMIN        | Delete agent                         |

---

## Sample Requests

### Register Agent (AGENT role JWT required)
```json
POST /api/v1/agents/register
Authorization: Bearer <agentJwtToken>
{
  "fullName": "Arjun Kumar",
  "phone": "9876543210",
  "vehicleType": "BIKE",
  "vehicleNumber": "DL01AB1234"
}
```

### Update GPS Location
```json
PUT /api/v1/agents/1/location
Authorization: Bearer <agentJwtToken>
{
  "latitude": 28.6139,
  "longitude": 77.2090
}
```

### Admin Verify Agent
```json
PUT /api/v1/agents/1/verify
Authorization: Bearer <adminJwtToken>
{
  "action": "VERIFY",
  "remarks": "Documents verified"
}
```

### Assign Order (called by order-service)
```json
POST /api/v1/agents/1/assign-order
Authorization: Bearer <token>
{
  "orderId": 55,
  "restaurantLatitude": 28.6139,
  "restaurantLongitude": 77.2090
}
```

---

## Error Codes

| HTTP | Exception               | When                                    |
|------|-------------------------|-----------------------------------------|
| 404  | AgentNotFoundException  | Agent not found by ID or userId         |
| 409  | DuplicateAgentException | User already has agent profile          |
| 403  | AgentNotVerifiedException | Agent tries to go online before verify|
| 409  | AgentBusyException      | Go offline while delivering             |
| 400  | AgentNotEligibleException | Assign order to non-eligible agent    |
| 400  | InvalidDeliveryException | Complete delivery with wrong orderId  |

---

## ═══════════════════════════════════════════════════════
## CHANGES NEEDED AS OTHER SERVICES ARE BUILT
## ═══════════════════════════════════════════════════════

### 1. When ORDER-SERVICE is built (Port: 8085)
**Current:** delivery-service assigns orders independently.
**Change needed in `DeliveryServiceImpl.assignOrder()`:**
```java
// ADD this validation before assigning:
Map<String, Object> order = orderServiceClient.getOrderById(request.getOrderId());
if (!"PLACED".equals(order.get("status"))) {
    throw new InvalidDeliveryException("Order is not in PLACED status");
}
```
**Change needed in `DeliveryServiceImpl.completeDelivery()`:**
```java
// ADD this to update order status to DELIVERED:
orderServiceClient.updateOrderStatus(orderId, Map.of("status", "DELIVERED"));
```
**File:** `feign/OrderServiceClient.java` — Feign client already written, just uncomment usage.

---

### 2. When NOTIFICATION-SERVICE is built (Port: 8089)
**No changes needed.** `NotificationServiceClient` Feign client is already wired.
Notifications are already being sent (with fallback if service is down).
Just ensure notification-service exposes: `POST /api/v1/notifications/send`
with payload: `{ type, recipientId, title, message, relatedId, relatedType }`

---

### 3. When REVIEW-SERVICE is built (Port: 8088)
**review-service will call delivery-service to update agent rating:**
```
PUT /api/v1/agents/{agentId}/rating
{ "avgRating": 4.5 }
Header: X-Internal-Service: quickbite-internal
```
`updateRating()` endpoint already exists — no changes needed.

---

### 4. When WEBSOCKET is added (Real-time tracking)
**PDF NFR:** "Delivery agent location updates pushed via WebSocket (STOMP) every 15 seconds"
**Add to `DeliveryServiceImpl.updateLocation()`:**
```java
// After saving location, publish to WebSocket topic:
messagingTemplate.convertAndSend(
    "/topic/order/" + agent.getCurrentOrderId() + "/location",
    AgentLocationResponse.from(saved)
);
```
**Add dependency:** `spring-boot-starter-websocket`
**Add config:** `WebSocketConfig.java` with STOMP endpoint `/ws`

---

### 5. When PAYMENT-SERVICE is built (Port: 8086)
Delivery earnings are currently tracked internally (`totalEarnings += 50.0`).
When payment-service is built, add Feign call to credit earnings to agent's wallet.

---

### 6. When API-GATEWAY route needs updating
Gateway already has: `Path=/api/v1/agents/** → lb://delivery-service`
No changes needed in gateway.

---

### 7. When QUICKBITE-WEB (MVC Layer) is built
`AdminController` will call:
- `GET /api/v1/agents/status/PENDING` → list pending verifications
- `PUT /api/v1/agents/{id}/verify`    → verify agent
- `GET /api/v1/agents/all`            → manage all agents

`CustomerController` will call:
- `GET /api/v1/agents/{agentId}/location` → show live tracking map
```
