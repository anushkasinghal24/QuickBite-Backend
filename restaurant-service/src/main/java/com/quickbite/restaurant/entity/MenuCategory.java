package com.quickbite.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * MenuCategory Entity
 *
 * PDF Section 4.3:
 * categoryId, restaurantId, name, description, imageUrl, displayOrder
 * Groups MenuItems into sections like Starters, Main Course, Desserts.
 */
@Entity
@Table(name = "menu_categories", indexes = {
        @Index(name = "idx_cat_restaurant_id", columnList = "restaurant_id"),
        @Index(name = "idx_display_order",     columnList = "display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Controls display order in the menu (1=top, 2=second, etc.) */
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 1;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Many Categories -> One Restaurant
     * @JsonIgnore on this side to avoid circular JSON serialization
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Restaurant restaurant;

    /** One Category -> Many MenuItems */
    @OneToMany(mappedBy = "menuCategory", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MenuItem> menuItems = new ArrayList<>();

    // ===== Helpers =====
    public void addMenuItem(MenuItem item) {
        menuItems.add(item);
        item.setMenuCategory(this);
    }

    public void removeMenuItem(MenuItem item) {
        menuItems.remove(item);
        item.setMenuCategory(null);
    }
}
