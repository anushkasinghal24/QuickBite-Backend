package com.quickbite.restaurant.service.impl;

import com.quickbite.restaurant.constants.AppConstants;
import com.quickbite.restaurant.dto.request.*;
import com.quickbite.restaurant.dto.response.*;
import com.quickbite.restaurant.entity.MenuCategory;
import com.quickbite.restaurant.entity.MenuItem;
import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.exception.DuplicateResourceException;
import com.quickbite.restaurant.exception.ResourceNotFoundException;
import com.quickbite.restaurant.exception.UnauthorizedException;
import com.quickbite.restaurant.repository.MenuCategoryRepository;
import com.quickbite.restaurant.repository.MenuItemRepository;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RestaurantServiceImpl
 *
 * Complete implementation of all restaurant + menu operations.
 * Redis caching applied on high-frequency read operations.
 * Feign calls to notification-service on approval/rejection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final ModelMapper modelMapper;
    private final RestTemplate restTemplate;

    @Value("${quickbite.notification.base-url:http://localhost:8084}")
    private String notificationBaseUrl;

    // ==================== RESTAURANT CRUD ====================

    @Override
    @CacheEvict(value = "restaurant_list", allEntries = true)
    public RestaurantResponse registerRestaurant(RegisterRestaurantRequest request, Long ownerId) {
        log.info("Registering restaurant '{}' for ownerId={}", request.getName(), ownerId);

        // Duplicate check: same owner can't register same restaurant name twice
        if (restaurantRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, request.getName())) {
            throw new DuplicateResourceException(
                "You already have a restaurant with name: " + request.getName());
        }

        Restaurant restaurant = Restaurant.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .description(request.getDescription())
                .cuisine(request.getCuisine())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .phone(request.getPhone())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .deliveryRadius(request.getDeliveryRadius() != null ? request.getDeliveryRadius() : 5.0)
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : 0.0)
                .estimatedDeliveryMin(request.getEstimatedDeliveryMin() != null ? request.getEstimatedDeliveryMin() : 30)
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .approvalStatus(AppConstants.STATUS_PENDING)
                .isOpen(false)
                .isActive(true)
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant registered with id={}, status=PENDING", saved.getRestaurantId());
        return mapToResponse(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "restaurant_detail", key = "#restaurantId")
    public RestaurantResponse getById(Long restaurantId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        return mapToResponse(restaurant, true);  // includes full menu
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getByOwner(Long ownerId) {
        return restaurantRepository.findByOwnerIdAndIsActiveTrue(ownerId)
                .stream()
                .map(r -> mapToResponse(r, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "restaurant_list", key = "'cuisine_' + #cuisine + '_' + #pageable.pageNumber")
    public PagedResponse<RestaurantResponse> getByCuisine(String cuisine, Pageable pageable) {
        Page<Restaurant> page = restaurantRepository
                .findByCuisineIgnoreCaseAndApprovalStatusAndIsActiveTrue(
                        cuisine, AppConstants.STATUS_APPROVED, pageable);
        return PagedResponse.of(page.map(r -> mapToResponse(r, false)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "restaurant_list", key = "'city_' + #city + '_' + #pageable.pageNumber")
    public PagedResponse<RestaurantResponse> getByCity(String city, Pageable pageable) {
        Page<Restaurant> page = restaurantRepository
                .findByCityIgnoreCaseAndApprovalStatusAndIsActiveTrue(
                        city, AppConstants.STATUS_APPROVED, pageable);
        return PagedResponse.of(page.map(r -> mapToResponse(r, false)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getNearby(Double lat, Double lng, Double radiusKm) {
        double radius = (radiusKm != null) ? radiusKm : AppConstants.DEFAULT_RADIUS_KM;
        log.debug("Finding nearby restaurants: lat={}, lng={}, radius={}km", lat, lng, radius);
        return restaurantRepository.findNearby(lat, lng, radius)
                .stream()
                .map(r -> mapToResponse(r, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RestaurantResponse> searchRestaurants(String keyword, Pageable pageable) {
        Page<Restaurant> page = restaurantRepository.globalSearch(keyword, pageable);
        return PagedResponse.of(page.map(r -> mapToResponse(r, false)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "restaurant_list", key = "'all_' + #pageable.pageNumber")
    public PagedResponse<RestaurantResponse> getAllApproved(Pageable pageable) {
        Page<Restaurant> page = restaurantRepository
                .findByApprovalStatusAndIsOpenAndIsActiveTrue(
                        AppConstants.STATUS_APPROVED, true, pageable);
        return PagedResponse.of(page.map(r -> mapToResponse(r, false)));
    }

    @Override
    @CacheEvict(value = {"restaurant_detail", "restaurant_list"}, allEntries = true)
    public RestaurantResponse updateRestaurant(Long restaurantId,
                                               UpdateRestaurantRequest request,
                                               Long ownerId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        verifyOwnership(restaurant, ownerId);

        // Partial update — only non-null fields
        if (request.getName() != null)               restaurant.setName(request.getName());
        if (request.getDescription() != null)        restaurant.setDescription(request.getDescription());
        if (request.getCuisine() != null)            restaurant.setCuisine(request.getCuisine());
        if (request.getAddress() != null)            restaurant.setAddress(request.getAddress());
        if (request.getCity() != null)               restaurant.setCity(request.getCity());
        if (request.getState() != null)              restaurant.setState(request.getState());
        if (request.getPincode() != null)            restaurant.setPincode(request.getPincode());
        if (request.getLatitude() != null)           restaurant.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)          restaurant.setLongitude(request.getLongitude());
        if (request.getPhone() != null)              restaurant.setPhone(request.getPhone());
        if (request.getEmail() != null)              restaurant.setEmail(request.getEmail());
        if (request.getImageUrl() != null)           restaurant.setImageUrl(request.getImageUrl());
        if (request.getDeliveryRadius() != null)     restaurant.setDeliveryRadius(request.getDeliveryRadius());
        if (request.getMinOrderAmount() != null)     restaurant.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getEstimatedDeliveryMin() != null) restaurant.setEstimatedDeliveryMin(request.getEstimatedDeliveryMin());
        if (request.getOpeningTime() != null)        restaurant.setOpeningTime(request.getOpeningTime());
        if (request.getClosingTime() != null)        restaurant.setClosingTime(request.getClosingTime());

        Restaurant updated = restaurantRepository.save(restaurant);
        log.info("Restaurant id={} updated by ownerId={}", restaurantId, ownerId);
        return mapToResponse(updated, false);
    }

    // ==================== ADMIN OPERATIONS ====================

    @Override
    @CacheEvict(value = {"restaurant_detail", "restaurant_list"}, allEntries = true)
    public void approveRestaurant(Long restaurantId, ApprovalRequest request) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        String status = request.getStatus().toUpperCase();

        if (!status.equals(AppConstants.STATUS_APPROVED) &&
            !status.equals(AppConstants.STATUS_REJECTED)) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }

        restaurantRepository.updateApprovalStatus(
                restaurantId, status, request.getRejectionReason());

        log.info("Restaurant id={} status changed to {}", restaurantId, status);

        // Notify restaurant owner via notification-service (Feign)
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("recipientId", restaurant.getOwnerId());
            payload.put("type", "RESTAURANT_" + status);
            payload.put("title", status.equals(AppConstants.STATUS_APPROVED)
                    ? "Restaurant Approved!" : "Restaurant Rejected");
            payload.put("message", status.equals(AppConstants.STATUS_APPROVED)
                    ? "Your restaurant '" + restaurant.getName() + "' is now live on QuickBite!"
                    : "Your restaurant was rejected. Reason: " + request.getRejectionReason());
            payload.put("relatedId", restaurantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String authHeader = currentAuthorizationHeader();
            if (authHeader != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authHeader);
            }
            restTemplate.postForEntity(
                    notificationBaseUrl + "/api/v1/notifications/send",
                    new HttpEntity<>(payload, headers),
                    String.class);
        } catch (Exception e) {
            log.warn("Failed to send approval notification: {}", e.getMessage());
            // Don't fail the main operation — graceful degradation
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RestaurantResponse> getPendingRestaurants(Pageable pageable) {
        Page<Restaurant> page = restaurantRepository
                .findByApprovalStatus(AppConstants.STATUS_PENDING, pageable);
        return PagedResponse.of(page.map(r -> mapToResponse(r, false)));
    }

    @Override
    @CacheEvict(value = {"restaurant_detail", "restaurant_list"}, allEntries = true)
    public void deleteRestaurant(Long restaurantId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        restaurant.setIsActive(false);  // Soft delete
        itemRepository.deactivateAllByRestaurantId(restaurantId);
        categoryRepository.deactivateAllByRestaurantId(restaurantId);
        restaurantRepository.save(restaurant);
        log.info("Restaurant id={} soft-deleted", restaurantId);
    }

    // ==================== OWNER OPERATIONS ====================

    @Override
    @CacheEvict(value = {"restaurant_detail", "restaurant_list"}, allEntries = true)
    public void toggleOpen(Long restaurantId, Long ownerId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        verifyOwnership(restaurant, ownerId);

        if (!restaurant.getApprovalStatus().equals(AppConstants.STATUS_APPROVED)) {
            throw new IllegalArgumentException(
                "Cannot open restaurant that is not approved by admin.");
        }

        boolean newState = !restaurant.getIsOpen();
        restaurantRepository.updateIsOpen(restaurantId, newState);
        log.info("Restaurant id={} toggled to isOpen={}", restaurantId, newState);
    }

    // ==================== CALLED BY review-service ====================

    @Override
    @CacheEvict(value = "restaurant_detail", key = "#restaurantId")
    public void updateRating(Long restaurantId, UpdateRatingRequest request) {
        findRestaurantById(restaurantId);  // validate exists
        restaurantRepository.updateRating(
                restaurantId, request.getAvgRating(), request.getTotalReviews());
        log.info("Rating updated for restaurantId={}: avg={}, total={}",
                restaurantId, request.getAvgRating(), request.getTotalReviews());
    }

    // ==================== MENU CATEGORY ====================

    @Override
    @CacheEvict(value = "restaurant_detail", key = "#restaurantId")
    public MenuCategoryResponse addCategory(Long restaurantId,
                                            AddCategoryRequest request,
                                            Long ownerId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        verifyOwnership(restaurant, ownerId);

        if (categoryRepository.existsByNameIgnoreCaseAndRestaurant_RestaurantId(
                request.getName(), restaurantId)) {
            throw new DuplicateResourceException(
                "Category '" + request.getName() + "' already exists in this restaurant.");
        }

        MenuCategory category = MenuCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1)
                .restaurant(restaurant)
                .isActive(true)
                .build();

        MenuCategory saved = categoryRepository.save(category);
        log.info("Category '{}' added to restaurantId={}", saved.getName(), restaurantId);
        return mapCategoryToResponse(saved, false);
    }

    @Override
    @CacheEvict(value = "restaurant_detail", key = "#restaurantId")
    public MenuCategoryResponse updateCategory(Long restaurantId, Long categoryId,
                                               AddCategoryRequest request, Long ownerId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        verifyOwnership(restaurant, ownerId);

        MenuCategory category = categoryRepository
                .findByCategoryIdAndRestaurant_RestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", categoryId));

        if (request.getName() != null)         category.setName(request.getName());
        if (request.getDescription() != null)  category.setDescription(request.getDescription());
        if (request.getImageUrl() != null)     category.setImageUrl(request.getImageUrl());
        if (request.getDisplayOrder() != null) category.setDisplayOrder(request.getDisplayOrder());

        return mapCategoryToResponse(categoryRepository.save(category), false);
    }

    @Override
    @CacheEvict(value = "restaurant_detail", key = "#restaurantId")
    public void deleteCategory(Long restaurantId, Long categoryId, Long ownerId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        verifyOwnership(restaurant, ownerId);

        MenuCategory category = categoryRepository
                .findByCategoryIdAndRestaurant_RestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", categoryId));

        category.setIsActive(false);  // soft delete
        itemRepository.deactivateAllByRestaurantId(restaurantId);
        categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getCategoriesByRestaurant(Long restaurantId) {
        findRestaurantById(restaurantId);  // validate
        return categoryRepository
                .findByRestaurant_RestaurantIdAndIsActiveTrueOrderByDisplayOrderAsc(restaurantId)
                .stream()
                .map(c -> mapCategoryToResponse(c, true))
                .collect(Collectors.toList());
    }

    // ==================== MENU ITEMS ====================

    @Override
    @CacheEvict(value = "restaurant_detail", key = "#restaurantId")
    public MenuItemResponse addMenuItem(Long restaurantId, Long categoryId,
                                        AddMenuItemRequest request, Long ownerId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        verifyOwnership(restaurant, ownerId);

        MenuCategory category = categoryRepository
                .findByCategoryIdAndRestaurant_RestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory", categoryId));

        MenuItem item = MenuItem.builder()
                .restaurantId(restaurantId)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountedPrice(request.getDiscountedPrice())
                .imageUrl(request.getImageUrl())
                .isVeg(request.getIsVeg() != null ? request.getIsVeg() : true)
                .calories(request.getCalories())
                .tags(request.getTags())
                .isAvailable(true)
                .isActive(true)
                .menuCategory(category)
                .build();

        MenuItem saved = itemRepository.save(item);
        log.info("MenuItem '{}' added to categoryId={}, restaurantId={}", saved.getName(), categoryId, restaurantId);
        return mapItemToResponse(saved);
    }

    @Override
    @CacheEvict(value = "restaurant_detail", key = "#restaurantId")
    public MenuItemResponse updateMenuItem(Long restaurantId, Long itemId,
                                           AddMenuItemRequest request, Long ownerId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        verifyOwnership(restaurant, ownerId);

        MenuItem item = itemRepository.findByItemIdAndIsActiveTrue(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));

        if (!item.getRestaurantId().equals(restaurantId)) {
            throw new UnauthorizedException("This item does not belong to your restaurant.");
        }

        if (request.getName() != null)            item.setName(request.getName());
        if (request.getDescription() != null)     item.setDescription(request.getDescription());
        if (request.getPrice() != null)           item.setPrice(request.getPrice());
        if (request.getDiscountedPrice() != null) item.setDiscountedPrice(request.getDiscountedPrice());
        if (request.getImageUrl() != null)        item.setImageUrl(request.getImageUrl());
        if (request.getIsVeg() != null)           item.setIsVeg(request.getIsVeg());
        if (request.getCalories() != null)        item.setCalories(request.getCalories());
        if (request.getTags() != null)            item.setTags(request.getTags());

        return mapItemToResponse(itemRepository.save(item));
    }

    @Override
    @CacheEvict(value = "restaurant_detail", key = "#restaurantId")
    public void deleteMenuItem(Long restaurantId, Long itemId, Long ownerId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        verifyOwnership(restaurant, ownerId);

        MenuItem item = itemRepository.findByItemIdAndIsActiveTrue(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));

        if (!item.getRestaurantId().equals(restaurantId)) {
            throw new UnauthorizedException("This item does not belong to your restaurant.");
        }

        item.setIsActive(false);
        itemRepository.save(item);
    }

    @Override
    @CacheEvict(value = "restaurant_detail", key = "#restaurantId")
    public void toggleItemAvailability(Long restaurantId, Long itemId, Long ownerId) {
        Restaurant restaurant = findRestaurantById(restaurantId);
        verifyOwnership(restaurant, ownerId);

        MenuItem item = itemRepository.findByItemIdAndIsActiveTrue(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));

        if (!item.getRestaurantId().equals(restaurantId)) {
            throw new UnauthorizedException("This item does not belong to your restaurant.");
        }

        boolean newState = !item.getIsAvailable();
        itemRepository.updateAvailability(itemId, newState);
        log.info("MenuItem id={} availability toggled to {}", itemId, newState);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenuByRestaurant(Long restaurantId) {
        findRestaurantById(restaurantId);
        return itemRepository
                .findByRestaurantIdAndIsActiveTrueOrderByMenuCategory_DisplayOrderAsc(restaurantId)
                .stream().map(this::mapItemToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getVegItems(Long restaurantId) {
        findRestaurantById(restaurantId);
        return itemRepository.findByRestaurantIdAndIsVegAndIsActiveTrue(restaurantId, true)
                .stream().map(this::mapItemToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> searchMenuItems(Long restaurantId, String keyword) {
        findRestaurantById(restaurantId);
        return itemRepository.searchByName(restaurantId, keyword)
                .stream().map(this::mapItemToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getItemById(Long itemId) {
        MenuItem item = itemRepository.findByItemIdAndIsActiveTrue(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", itemId));
        return mapItemToResponse(item);
    }

    // ==================== PRIVATE HELPERS ====================

    private Restaurant findRestaurantById(Long id) {
        return restaurantRepository.findById(id)
                .filter(Restaurant::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
    }

    private void verifyOwnership(Restaurant restaurant, Long ownerId) {
        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedException(
                "You are not the owner of restaurant: " + restaurant.getName());
        }
    }

    private String currentAuthorizationHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }

    /** Map Restaurant entity → RestaurantResponse DTO */
    private RestaurantResponse mapToResponse(Restaurant r, boolean includeMenu) {
        RestaurantResponse response = modelMapper.map(r, RestaurantResponse.class);
        if (includeMenu) {
            List<MenuCategoryResponse> categories =
                categoryRepository
                    .findByRestaurant_RestaurantIdAndIsActiveTrueOrderByDisplayOrderAsc(r.getRestaurantId())
                    .stream()
                    .map(c -> mapCategoryToResponse(c, true))
                    .collect(Collectors.toList());
            response.setMenuCategories(categories);
        }
        return response;
    }

    private MenuCategoryResponse mapCategoryToResponse(MenuCategory c, boolean includeItems) {
        MenuCategoryResponse response = MenuCategoryResponse.builder()
                .categoryId(c.getCategoryId())
                .restaurantId(c.getRestaurant() != null ? c.getRestaurant().getRestaurantId() : null)
                .name(c.getName())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .displayOrder(c.getDisplayOrder())
                .isActive(c.getIsActive())
                .build();

        if (includeItems) {
            List<MenuItemResponse> items =
                itemRepository.findByMenuCategory_CategoryIdAndIsActiveTrue(c.getCategoryId())
                    .stream().map(this::mapItemToResponse).collect(Collectors.toList());
            response.setMenuItems(items);
        }
        return response;
    }

    private MenuItemResponse mapItemToResponse(MenuItem i) {
        return MenuItemResponse.builder()
                .itemId(i.getItemId())
                .restaurantId(i.getRestaurantId())
                .categoryId(i.getMenuCategory() != null ? i.getMenuCategory().getCategoryId() : null)
                .name(i.getName())
                .description(i.getDescription())
                .price(i.getPrice())
                .discountedPrice(i.getDiscountedPrice())
                .effectivePrice(i.getEffectivePrice())
                .imageUrl(i.getImageUrl())
                .isVeg(i.getIsVeg())
                .isAvailable(i.getIsAvailable())
                .rating(i.getRating())
                .calories(i.getCalories())
                .tags(i.getTags())
                .build();
    }
}
