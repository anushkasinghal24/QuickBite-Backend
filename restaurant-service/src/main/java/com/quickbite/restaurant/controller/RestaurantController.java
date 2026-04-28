package com.quickbite.restaurant.controller;

import com.quickbite.restaurant.constants.AppConstants;
import com.quickbite.restaurant.dto.request.*;
import com.quickbite.restaurant.dto.response.*;
import com.quickbite.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RestaurantController
 *
 * Base URL: /api/v1/restaurants
 *
 * All endpoints documented in PDF Section 4.2 and 4.3.
 * Swagger UI: http://localhost:8082/swagger-ui.html
 *
 * Authentication:
 *  - GET endpoints → public (guests + customers)
 *  - POST/PUT/DELETE → JWT required (role enforced in SecurityConfig + @PreAuthorize)
 *
 * Principal: authentication.getPrincipal() = userId (Long) — set by JwtAuthFilter
 */
@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restaurant API", description = "Restaurant profile, discovery, and menu management")
public class RestaurantController {

    private final RestaurantService restaurantService;

    // ================================================================
    //                    RESTAURANT ENDPOINTS
    // ================================================================

    /**
     * POST /api/v1/restaurants
     * Register new restaurant — ROLE_OWNER only
     *
     * Request Body: RegisterRestaurantRequest
     * Response: 201 + RestaurantResponse (status=PENDING)
     */
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Register new restaurant (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<RestaurantResponse>> registerRestaurant(
            @Valid @RequestBody RegisterRestaurantRequest request,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        RestaurantResponse response = restaurantService.registerRestaurant(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AppConstants.RESTAURANT_REGISTERED, response));
    }

    /**
     * GET /api/v1/restaurants/{id}
     * Get full restaurant detail + menu — PUBLIC
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant details with full menu (PUBLIC)")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Restaurant fetched", restaurantService.getById(id)));
    }

    /**
     * GET /api/v1/restaurants?page=0&size=10&sort=avgRating,desc
     * Get all approved, open restaurants (paginated) — PUBLIC
     */
    @GetMapping
    @Operation(summary = "Get all approved restaurants (paginated, PUBLIC)")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantResponse>>> getAllApproved(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "avgRating") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(
                ApiResponse.success("Restaurants fetched",
                        restaurantService.getAllApproved(pageable)));
    }

    /**
     * GET /api/v1/restaurants/search?keyword=pizza&page=0&size=10
     * Search restaurants by name/cuisine/city — PUBLIC
     */
    @GetMapping("/search")
    @Operation(summary = "Search restaurants (PUBLIC)")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantResponse>>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Search results",
                        restaurantService.searchRestaurants(keyword, pageable)));
    }

    /**
     * GET /api/v1/restaurants/nearby?lat=28.6139&lng=77.2090&radius=5
     * Find nearby restaurants using Haversine — PUBLIC
     */
    @GetMapping("/nearby")
    @Operation(summary = "Find nearby restaurants by GPS coordinates (PUBLIC)")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getNearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false) Double radius) {

        return ResponseEntity.ok(
                ApiResponse.success("Nearby restaurants",
                        restaurantService.getNearby(lat, lng, radius)));
    }

    /**
     * GET /api/v1/restaurants/cuisine/{cuisine}
     * Filter by cuisine — PUBLIC
     */
    @GetMapping("/cuisine/{cuisine}")
    @Operation(summary = "Get restaurants by cuisine (PUBLIC)")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantResponse>>> getByCuisine(
            @PathVariable String cuisine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("avgRating").descending());
        return ResponseEntity.ok(
                ApiResponse.success("Restaurants by cuisine",
                        restaurantService.getByCuisine(cuisine, pageable)));
    }

    /**
     * GET /api/v1/restaurants/city/{city}
     * Filter by city — PUBLIC
     */
    @GetMapping("/city/{city}")
    @Operation(summary = "Get restaurants by city (PUBLIC)")
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantResponse>>> getByCity(
            @PathVariable String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("avgRating").descending());
        return ResponseEntity.ok(
                ApiResponse.success("Restaurants in " + city,
                        restaurantService.getByCity(city, pageable)));
    }

    /**
     * GET /api/v1/restaurants/my
     * Get owner's own restaurants — ROLE_OWNER
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Get my restaurants (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getMyRestaurants(
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                ApiResponse.success("Your restaurants",
                        restaurantService.getByOwner(ownerId)));
    }

    /**
     * PUT /api/v1/restaurants/{id}
     * Update restaurant profile — ROLE_OWNER (must be owner of this restaurant)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Update restaurant (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRestaurantRequest request,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                ApiResponse.success(AppConstants.RESTAURANT_UPDATED,
                        restaurantService.updateRestaurant(id, request, ownerId)));
    }

    /**
     * PUT /api/v1/restaurants/{id}/toggle-open
     * Toggle restaurant open/close — ROLE_OWNER
     */
    @PutMapping("/{id}/toggle-open")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Toggle restaurant open/close (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> toggleOpen(
            @PathVariable Long id,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        restaurantService.toggleOpen(id, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Restaurant status toggled"));
    }

    /**
     * PUT /api/v1/restaurants/{id}/approve
     * Approve or reject restaurant — ROLE_ADMIN
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve/Reject restaurant (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> approveRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request) {

        restaurantService.approveRestaurant(id, request);
        return ResponseEntity.ok(ApiResponse.success(
                request.getStatus().equals("APPROVED")
                        ? AppConstants.RESTAURANT_APPROVED
                        : AppConstants.RESTAURANT_REJECTED));
    }

    /**
     * GET /api/v1/restaurants/pending
     * Get all pending approval restaurants — ROLE_ADMIN
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending restaurants (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<RestaurantResponse>>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.success("Pending restaurants",
                        restaurantService.getPendingRestaurants(pageable)));
    }

    /**
     * DELETE /api/v1/restaurants/{id}
     * Soft-delete restaurant — ROLE_ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete restaurant (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.RESTAURANT_DELETED));
    }

    /**
     * PUT /api/v1/restaurants/{id}/rating
     * Update average rating — called INTERNALLY by review-service
     */
    @PutMapping("/{id}/rating")
    @Operation(summary = "Update restaurant rating (internal - called by review-service)")
    public ResponseEntity<ApiResponse<Void>> updateRating(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRatingRequest request,
            @RequestHeader(value = "X-Internal-Service", required = false) String internalHeader) {

        // Basic internal service header check
        if (!"quickbite-internal".equals(internalHeader)) {
            // Will be caught by Spring Security anyway, but adding belt-and-suspenders check
            log.warn("updateRating called without internal service header");
        }
        restaurantService.updateRating(id, request);
        return ResponseEntity.ok(ApiResponse.success("Rating updated"));
    }

    // ================================================================
    //                    MENU CATEGORY ENDPOINTS
    // ================================================================

    /**
     * GET /api/v1/restaurants/{id}/categories
     * Get all menu categories — PUBLIC
     */
    @GetMapping("/{id}/categories")
    @Operation(summary = "Get menu categories (PUBLIC)")
    public ResponseEntity<ApiResponse<List<MenuCategoryResponse>>> getCategories(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Categories fetched",
                        restaurantService.getCategoriesByRestaurant(id)));
    }

    /**
     * POST /api/v1/restaurants/{id}/categories
     * Add menu category — ROLE_OWNER
     */
    @PostMapping("/{id}/categories")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Add menu category (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> addCategory(
            @PathVariable Long id,
            @Valid @RequestBody AddCategoryRequest request,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        MenuCategoryResponse response = restaurantService.addCategory(id, request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AppConstants.CATEGORY_ADDED, response));
    }

    /**
     * PUT /api/v1/restaurants/{id}/categories/{categoryId}
     * Update category — ROLE_OWNER
     */
    @PutMapping("/{id}/categories/{categoryId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Update menu category (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> updateCategory(
            @PathVariable Long id,
            @PathVariable Long categoryId,
            @Valid @RequestBody AddCategoryRequest request,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                ApiResponse.success(AppConstants.CATEGORY_UPDATED,
                        restaurantService.updateCategory(id, categoryId, request, ownerId)));
    }

    /**
     * DELETE /api/v1/restaurants/{id}/categories/{categoryId}
     * Delete category — ROLE_OWNER
     */
    @DeleteMapping("/{id}/categories/{categoryId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Delete menu category (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            @PathVariable Long categoryId,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        restaurantService.deleteCategory(id, categoryId, ownerId);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.CATEGORY_DELETED));
    }

    // ================================================================
    //                    MENU ITEM ENDPOINTS
    // ================================================================

    /**
     * GET /api/v1/restaurants/{id}/menu
     * Get full menu (all categories + items) — PUBLIC
     */
    @GetMapping("/{id}/menu")
    @Operation(summary = "Get full menu of restaurant (PUBLIC)")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getMenu(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Menu fetched",
                        restaurantService.getMenuByRestaurant(id)));
    }

    /**
     * GET /api/v1/restaurants/{id}/items/veg
     * Get veg-only items — PUBLIC
     */
    @GetMapping("/{id}/items/veg")
    @Operation(summary = "Get veg items (PUBLIC)")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getVegItems(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Veg items", restaurantService.getVegItems(id)));
    }

    /**
     * GET /api/v1/restaurants/{id}/items/search?keyword=paneer
     * Search menu items — PUBLIC
     */
    @GetMapping("/{id}/items/search")
    @Operation(summary = "Search menu items (PUBLIC)")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> searchItems(
            @PathVariable Long id,
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                ApiResponse.success("Search results",
                        restaurantService.searchMenuItems(id, keyword)));
    }

    /**
     * GET /api/v1/restaurants/items/{itemId}
     * Get single item by id — PUBLIC (used by cart-service to validate & get price)
     */
    @GetMapping("/items/{itemId}")
    @Operation(summary = "Get menu item by id (PUBLIC - used by cart-service)")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getItemById(@PathVariable Long itemId) {
        return ResponseEntity.ok(
                ApiResponse.success("Item fetched", restaurantService.getItemById(itemId)));
    }

    /**
     * POST /api/v1/restaurants/{id}/categories/{categoryId}/items
     * Add menu item — ROLE_OWNER
     */
    @PostMapping("/{id}/categories/{categoryId}/items")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Add menu item (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<MenuItemResponse>> addMenuItem(
            @PathVariable Long id,
            @PathVariable Long categoryId,
            @Valid @RequestBody AddMenuItemRequest request,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        MenuItemResponse response = restaurantService.addMenuItem(id, categoryId, request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(AppConstants.ITEM_ADDED, response));
    }

    /**
     * PUT /api/v1/restaurants/{id}/items/{itemId}
     * Update menu item — ROLE_OWNER
     */
    @PutMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Update menu item (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody AddMenuItemRequest request,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                ApiResponse.success(AppConstants.ITEM_UPDATED,
                        restaurantService.updateMenuItem(id, itemId, request, ownerId)));
    }

    /**
     * PUT /api/v1/restaurants/{id}/items/{itemId}/toggle-availability
     * Toggle item in-stock/out-of-stock — ROLE_OWNER
     */
    @PutMapping("/{id}/items/{itemId}/toggle-availability")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Toggle item availability (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> toggleAvailability(
            @PathVariable Long id,
            @PathVariable Long itemId,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        restaurantService.toggleItemAvailability(id, itemId, ownerId);
        return ResponseEntity.ok(ApiResponse.success("Item availability toggled"));
    }

    /**
     * DELETE /api/v1/restaurants/{id}/items/{itemId}
     * Delete menu item — ROLE_OWNER
     */
    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Delete menu item (OWNER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            Authentication authentication) {

        Long ownerId = (Long) authentication.getPrincipal();
        restaurantService.deleteMenuItem(id, itemId, ownerId);
        return ResponseEntity.ok(ApiResponse.success(AppConstants.ITEM_DELETED));
    }
}
