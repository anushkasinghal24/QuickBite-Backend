package com.quickbite.restaurant.service;

import com.quickbite.restaurant.dto.request.*;
import com.quickbite.restaurant.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * RestaurantService Interface
 *
 * PDF Section 4.2 — all declared methods:
 * registerRestaurant, getById, getByOwner, getByCuisine, getByCity,
 * getNearby, searchRestaurants, updateRestaurant, approveRestaurant,
 * toggleOpen, deleteRestaurant, updateRating
 *
 * Plus menu operations (Section 4.3):
 * addCategory, updateCategory, deleteCategory,
 * addMenuItem, updateMenuItem, deleteMenuItem, toggleAvailability
 */
public interface RestaurantService {

    // ===== RESTAURANT CRUD =====
    RestaurantResponse registerRestaurant(RegisterRestaurantRequest request, Long ownerId);

    RestaurantResponse getById(Long restaurantId);

    List<RestaurantResponse> getByOwner(Long ownerId);

    PagedResponse<RestaurantResponse> getByCuisine(String cuisine, Pageable pageable);

    PagedResponse<RestaurantResponse> getByCity(String city, Pageable pageable);

    List<RestaurantResponse> getNearby(Double lat, Double lng, Double radiusKm);

    PagedResponse<RestaurantResponse> searchRestaurants(String keyword, Pageable pageable);

    PagedResponse<RestaurantResponse> getAllApproved(Pageable pageable);

    RestaurantResponse updateRestaurant(Long restaurantId, UpdateRestaurantRequest request, Long ownerId);

    // ===== ADMIN OPERATIONS =====
    void approveRestaurant(Long restaurantId, ApprovalRequest request);

    PagedResponse<RestaurantResponse> getPendingRestaurants(Pageable pageable);

    void deleteRestaurant(Long restaurantId);

    // ===== OWNER OPERATIONS =====
    void toggleOpen(Long restaurantId, Long ownerId);

    // ===== CALLED BY review-service (via Feign) =====
    void updateRating(Long restaurantId, UpdateRatingRequest request);

    // ===== MENU CATEGORY =====
    MenuCategoryResponse addCategory(Long restaurantId, AddCategoryRequest request, Long ownerId);

    MenuCategoryResponse updateCategory(Long restaurantId, Long categoryId,
                                        AddCategoryRequest request, Long ownerId);

    void deleteCategory(Long restaurantId, Long categoryId, Long ownerId);

    List<MenuCategoryResponse> getCategoriesByRestaurant(Long restaurantId);

    // ===== MENU ITEMS =====
    MenuItemResponse addMenuItem(Long restaurantId, Long categoryId,
                                 AddMenuItemRequest request, Long ownerId);

    MenuItemResponse updateMenuItem(Long restaurantId, Long itemId,
                                    AddMenuItemRequest request, Long ownerId);

    void deleteMenuItem(Long restaurantId, Long itemId, Long ownerId);

    void toggleItemAvailability(Long restaurantId, Long itemId, Long ownerId);

    List<MenuItemResponse> getMenuByRestaurant(Long restaurantId);

    List<MenuItemResponse> getVegItems(Long restaurantId);

    List<MenuItemResponse> searchMenuItems(Long restaurantId, String keyword);

    MenuItemResponse getItemById(Long itemId);
}
