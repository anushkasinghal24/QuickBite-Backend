package com.quickbite.restaurant;

import com.quickbite.restaurant.dto.request.RegisterRestaurantRequest;
import com.quickbite.restaurant.dto.response.RestaurantResponse;
import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.exception.DuplicateResourceException;
import com.quickbite.restaurant.repository.MenuCategoryRepository;
import com.quickbite.restaurant.repository.MenuItemRepository;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.service.impl.RestaurantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock private RestaurantRepository restaurantRepository;
    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private MenuItemRepository itemRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private RegisterRestaurantRequest request;
    private Restaurant savedRestaurant;

    @BeforeEach
    void setUp() {
        request = new RegisterRestaurantRequest();
        request.setName("Spice Corner");
        request.setCuisine("Indian");
        request.setAddress("MG Road, Near Metro Station");
        request.setCity("Delhi");
        request.setLatitude(28.6448);
        request.setLongitude(77.2167);
        request.setPhone("9876543214");

        savedRestaurant = Restaurant.builder()
                .restaurantId(7L)
                .ownerId(36L)
                .name("Spice Corner")
                .cuisine("Indian")
                .city("Delhi")
                .approvalStatus("PENDING")
                .isOpen(false)
                .isActive(true)
                .build();
    }

    @Test
    void registerRestaurant_shouldSaveAndReturnResponse() {
        when(restaurantRepository.existsByOwnerIdAndNameIgnoreCase(36L, "Spice Corner"))
                .thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(savedRestaurant);

        RestaurantResponse response = restaurantService.registerRestaurant(request, 36L);

        assertNotNull(response);
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    void registerRestaurant_shouldThrowWhenDuplicateRestaurantExists() {
        when(restaurantRepository.existsByOwnerIdAndNameIgnoreCase(36L, "Spice Corner"))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> restaurantService.registerRestaurant(request, 36L));

        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }
}
