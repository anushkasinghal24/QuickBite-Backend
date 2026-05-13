package com.quickbite.restaurant.constants;

/**
 * Platform-wide constants used across restaurant-service.
 * Jab order-service, review-service aate hain tab ye sab reuse hote hain.
 */
public class AppConstants {

    private AppConstants() {}

    // ===== USER ROLES (auth-service ke saath match hone chahiye) =====
    public static final String ROLE_CUSTOMER        = "CUSTOMER";
    public static final String ROLE_OWNER           = "OWNER";
    public static final String ROLE_AGENT           = "AGENT";
    public static final String ROLE_ADMIN           = "ADMIN";

    // ===== APPROVAL STATUS =====
    public static final String STATUS_PENDING       = "PENDING";
    public static final String STATUS_APPROVED      = "APPROVED";
    public static final String STATUS_REJECTED      = "REJECTED";

    // ===== PAGINATION DEFAULTS =====
    public static final int    DEFAULT_PAGE         = 0;
    public static final int    DEFAULT_PAGE_SIZE    = 10;
    public static final String DEFAULT_SORT_BY      = "avgRating";
    public static final String DEFAULT_SORT_DIR     = "desc";

    // ===== GEO =====
    public static final double DEFAULT_RADIUS_KM    = 10.0;   // default nearby search radius
    public static final double EARTH_RADIUS_KM      = 6371.0; // Haversine formula

    // ===== CACHE KEYS =====
    public static final String CACHE_RESTAURANT_ALL = "restaurants_all";
    public static final String CACHE_RESTAURANT_ID  = "restaurant_";

    // ===== JWT HEADER =====
    public static final String AUTH_HEADER          = "Authorization";
    public static final String TOKEN_PREFIX         = "Bearer ";

    // ===== SUCCESS MESSAGES =====
    public static final String RESTAURANT_REGISTERED   = "Restaurant registered successfully. Awaiting admin approval.";
    public static final String RESTAURANT_UPDATED      = "Restaurant updated successfully.";
    public static final String RESTAURANT_APPROVED     = "Restaurant approved successfully.";
    public static final String RESTAURANT_REJECTED     = "Restaurant rejected.";
    public static final String RESTAURANT_DELETED      = "Restaurant deleted successfully.";
    public static final String TOGGLE_OPEN             = "Restaurant is now OPEN.";
    public static final String TOGGLE_CLOSED           = "Restaurant is now CLOSED.";

    // ===== MENU MESSAGES =====
    public static final String CATEGORY_ADDED          = "Menu category added successfully.";
    public static final String CATEGORY_UPDATED        = "Menu category updated successfully.";
    public static final String CATEGORY_DELETED        = "Menu category deleted successfully.";
    public static final String ITEM_ADDED              = "Menu item added successfully.";
    public static final String ITEM_UPDATED            = "Menu item updated successfully.";
    public static final String ITEM_DELETED            = "Menu item deleted successfully.";
    public static final String ITEM_AVAILABILITY_ON    = "Item marked as AVAILABLE.";
    public static final String ITEM_AVAILABILITY_OFF   = "Item marked as UNAVAILABLE.";
}
