package com.quickbite.delivery_service.resource;

import com.quickbite.delivery_service.dto.*;
import com.quickbite.delivery_service.entity.DeliveryAgent;
import com.quickbite.delivery_service.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DeliveryResource — REST Controller
 *
 * PDF Section 4.7: DeliveryResource exposes /agents endpoints:
 *   POST (register), GET (by id/nearby),
 *   PUT (location/availability/verify/rating),
 *   POST (assignOrder/completeDelivery),
 *   GET (activeDeliveries)
 *
 * Base URL: /api/v1/agents
 * Gateway route: /api/v1/agents/** → lb://delivery-service
 *
 * ┌──────────────────────────────────────────┬────────────┬──────────────────────┐
 * │ Endpoint                                  │ Auth       │ Role                 │
 * ├──────────────────────────────────────────┼────────────┼──────────────────────┤
 * │ POST   /register                          │ JWT        │ AGENT                │
 * │ GET    /my                                │ JWT        │ AGENT                │
 * │ GET    /{agentId}                         │ JWT        │ AGENT/ADMIN          │
 * │ GET    /nearby                            │ JWT        │ Any (for assignment) │
 * │ PUT    /{agentId}/location               │ JWT        │ AGENT                │
 * │ PUT    /{agentId}/availability           │ JWT        │ AGENT                │
 * │ PUT    /{agentId}/verify                 │ JWT        │ ADMIN                │
 * │ POST   /{agentId}/assign-order           │ JWT        │ ADMIN/Internal       │
 * │ POST   /{agentId}/complete/{orderId}     │ JWT        │ AGENT                │
 * │ PUT    /{agentId}/rating                 │ JWT        │ Internal             │
 * │ GET    /{agentId}/location              │ JWT        │ CUSTOMER (tracking)  │
 * │ GET    /{agentId}/earnings              │ JWT        │ AGENT                │
 * │ GET    /active                           │ JWT        │ ADMIN                │
 * │ GET    /all                              │ JWT        │ ADMIN                │
 * │ GET    /status/{status}                  │ JWT        │ ADMIN                │
 * │ DELETE /{agentId}                        │ JWT        │ ADMIN                │
 * └──────────────────────────────────────────┴────────────┴──────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Delivery Agent API", description = "Delivery agent registration, tracking and management")
public class DeliveryResource {

    private final DeliveryService deliveryService;

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v1/agents/register
    // AGENT registers themselves — userId extracted from JWT
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/register")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Register as delivery agent (AGENT)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> register(
            @Valid @RequestBody RegisterAgentRequest request,
            Authentication authentication) {

        Integer userId = (Integer) authentication.getPrincipal();
        log.info("POST /api/v1/agents/register — userId={}", userId);

        AgentResponse response = deliveryService.registerAgent(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Agent registration submitted. Awaiting admin verification.", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/agents/my
    // Agent views own profile — userId from JWT
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Get my agent profile (AGENT)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> getMyProfile(Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        AgentResponse response = deliveryService.getAgentByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/agents/{agentId}
    // Get agent by ID — AGENT (own), ADMIN (any)
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{agentId}")
    @Operation(summary = "Get agent by ID (AGENT/ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> getById(@PathVariable Integer agentId) {
        AgentResponse response = deliveryService.getAgentById(agentId);
        return ResponseEntity.ok(ApiResponse.success("Agent fetched", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/agents/nearby?lat=28.6&lng=77.2&radius=5
    // Find nearby available agents — used by order-service for assignment
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/nearby")
    @Operation(summary = "Find nearby available agents (GPS)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<AgentResponse>>> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false) Double radius) {

        log.info("GET /api/v1/agents/nearby lat={}, lng={}, radius={}", lat, lng, radius);
        List<AgentResponse> agents = deliveryService.getNearbyAgents(lat, lng, radius);
        return ResponseEntity.ok(ApiResponse.success("Nearby agents found: " + agents.size(), agents));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/v1/agents/{agentId}/location
    // Agent updates live GPS — called by agent app every 15 seconds (PDF NFR)
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{agentId}/location")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Update live GPS location (AGENT)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> updateLocation(
            @PathVariable Integer agentId,
            @Valid @RequestBody UpdateLocationRequest request) {

        log.debug("PUT /api/v1/agents/{}/location lat={}, lng={}", agentId, request.getLatitude(), request.getLongitude());
        AgentResponse response = deliveryService.updateLocation(agentId, request);
        return ResponseEntity.ok(ApiResponse.success("Location updated", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/v1/agents/{agentId}/availability
    // Toggle online/offline — AGENT
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{agentId}/availability")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Toggle availability online/offline (AGENT)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> setAvailability(
            @PathVariable Integer agentId,
            @Valid @RequestBody SetAvailabilityRequest request) {

        log.info("PUT /api/v1/agents/{}/availability available={}", agentId, request.getAvailable());
        AgentResponse response = deliveryService.setAvailability(agentId, request);
        String msg = Boolean.TRUE.equals(request.getAvailable()) ? "Agent is now ONLINE" : "Agent is now OFFLINE";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/v1/agents/{agentId}/verify
    // Admin verifies/rejects/suspends agent
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{agentId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verify/Reject/Suspend agent (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> verifyAgent(
            @PathVariable Integer agentId,
            @Valid @RequestBody VerifyAgentRequest request) {

        log.info("PUT /api/v1/agents/{}/verify action={}", agentId, request.getAction());
        AgentResponse response = deliveryService.verifyAgent(agentId, request);
        return ResponseEntity.ok(ApiResponse.success("Agent status updated: " + request.getAction(), response));
    }

    @PutMapping("/{agentId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve agent (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> approveAgent(@PathVariable Integer agentId) {
        log.info("PUT /api/v1/agents/{}/approve", agentId);
        AgentResponse response = deliveryService.verifyAgent(agentId, new VerifyAgentRequest("VERIFY", null));
        return ResponseEntity.ok(ApiResponse.success("Agent approved", response));
    }

    @PutMapping("/{agentId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject agent (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> rejectAgent(
            @PathVariable Integer agentId,
            @RequestBody(required = false) VerifyAgentRequest request) {

        String remarks = request != null ? request.getRemarks() : null;
        log.info("PUT /api/v1/agents/{}/reject", agentId);
        AgentResponse response = deliveryService.verifyAgent(agentId, new VerifyAgentRequest("REJECT", remarks));
        return ResponseEntity.ok(ApiResponse.success("Agent rejected", response));
    }

    @PutMapping("/{agentId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend agent (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> suspendAgent(
            @PathVariable Integer agentId,
            @RequestBody(required = false) VerifyAgentRequest request) {

        String remarks = request != null ? request.getRemarks() : null;
        log.info("PUT /api/v1/agents/{}/suspend", agentId);
        AgentResponse response = deliveryService.verifyAgent(agentId, new VerifyAgentRequest("SUSPEND", remarks));
        return ResponseEntity.ok(ApiResponse.success("Agent suspended", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // POST /api/v1/agents/{agentId}/assign-order
    // Assign order to agent — called by order-service (internal)
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/{agentId}/assign-order")
    @Operation(summary = "Assign order to agent (internal — order-service calls this)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> assignOrder(
            @PathVariable Integer agentId,
            @Valid @RequestBody AssignOrderRequest request) {

        log.info("POST /api/v1/agents/{}/assign-order orderId={}", agentId, request.getOrderId());
        AgentResponse response = deliveryService.assignOrder(agentId, request);
        return ResponseEntity.ok(ApiResponse.success("Order #" + request.getOrderId() + " assigned to agent", response));
    }

    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{agentId}/assigned-order")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @Operation(summary = "Get currently assigned order (AGENT)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AssignedOrderResponse>> getAssignedOrder(@PathVariable Integer agentId) {
        AssignedOrderResponse response = deliveryService.getAssignedOrder(agentId);
        return ResponseEntity.ok(ApiResponse.success("Assigned order fetched", response));
    }

    @PostMapping("/{agentId}/pickup/{orderId}")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Mark order picked up (AGENT)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> pickUpOrder(
            @PathVariable Integer agentId,
            @PathVariable Integer orderId) {

        log.info("POST /api/v1/agents/{}/pickup/{}", agentId, orderId);
        AgentResponse response = deliveryService.pickUpOrder(agentId, orderId);
        return ResponseEntity.ok(ApiResponse.success("Order #" + orderId + " picked up", response));
    }

    // POST /api/v1/agents/{agentId}/complete/{orderId}
    // Agent marks delivery as complete
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/{agentId}/complete/{orderId}")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Mark delivery complete (AGENT)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> completeDelivery(
            @PathVariable Integer agentId,
            @PathVariable Integer orderId) {

        log.info("POST /api/v1/agents/{}/complete/{}", agentId, orderId);
        AgentResponse response = deliveryService.completeDelivery(agentId, orderId);
        return ResponseEntity.ok(ApiResponse.success("Delivery completed for order #" + orderId, response));
    }

    // ─────────────────────────────────────────────────────────────────
    // PUT /api/v1/agents/{agentId}/rating
    // Update average rating — called by review-service after customer rates
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{agentId}/rating")
    @Operation(summary = "Update agent rating (internal — review-service calls this)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentResponse>> updateRating(
            @PathVariable Integer agentId,
            @Valid @RequestBody UpdateRatingRequest request,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalHeader) {

        log.info("PUT /api/v1/agents/{}/rating = {}", agentId, request.getAvgRating());
        AgentResponse response = deliveryService.updateRating(agentId, request);
        return ResponseEntity.ok(ApiResponse.success("Rating updated", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/agents/{agentId}/location
    // Customer views agent live location during order tracking
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{agentId}/location")
    @Operation(summary = "Get agent live location (CUSTOMER tracking)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<AgentLocationResponse>> getLocation(@PathVariable Integer agentId) {
        AgentLocationResponse response = deliveryService.getAgentLocation(agentId);
        return ResponseEntity.ok(ApiResponse.success("Agent location fetched", response));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/agents/{agentId}/earnings
    // Agent views earnings and stats
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{agentId}/earnings")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get earnings summary (AGENT)", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<ApiResponse<EarningsSummaryResponse>> getEarnings(@PathVariable Integer agentId) {
      EarningsSummaryResponse response = deliveryService.getEarningsSummary(agentId);
      return ResponseEntity.ok(ApiResponse.success("Earnings fetched", response));
  }

  @GetMapping("/{agentId}/history")
  @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
  @Operation(summary = "Get completed delivery history (AGENT)", security = @SecurityRequirement(name = "bearerAuth"))
  public ResponseEntity<ApiResponse<List<DeliveryHistoryResponse>>> getDeliveryHistory(@PathVariable Integer agentId) {
      List<DeliveryHistoryResponse> history = deliveryService.getDeliveryHistory(agentId);
      return ResponseEntity.ok(ApiResponse.success("Delivery history fetched (" + history.size() + ")", history));
  }

  // ─────────────────────────────────────────────────────────────────
  // GET /api/v1/agents/active
    // ADMIN: agents currently delivering
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get agents with active deliveries (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<AgentResponse>>> getActiveDeliveries() {
        List<AgentResponse> agents = deliveryService.getActiveDeliveries();
        return ResponseEntity.ok(ApiResponse.success("Active deliveries: " + agents.size(), agents));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/agents/all
    // ADMIN: all agents
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all delivery agents (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<AgentResponse>>> getAllAgents() {
        List<AgentResponse> agents = deliveryService.getAllAgents();
        return ResponseEntity.ok(ApiResponse.success("Total agents: " + agents.size(), agents));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/v1/agents/status/{status}
    // ADMIN: agents by status (PENDING/VERIFIED/SUSPENDED/REJECTED)
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get agents by status (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<AgentResponse>>> getByStatus(
            @PathVariable DeliveryAgent.AgentStatus status) {

        List<AgentResponse> agents = deliveryService.getAgentsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Agents with status " + status + ": " + agents.size(), agents));
    }

    // ─────────────────────────────────────────────────────────────────
    // DELETE /api/v1/agents/{agentId}
    // ADMIN: delete agent
    // ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{agentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete delivery agent (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteAgent(@PathVariable Integer agentId) {
        deliveryService.deleteAgent(agentId);
        return ResponseEntity.ok(ApiResponse.success("Agent deleted"));
    }
}
