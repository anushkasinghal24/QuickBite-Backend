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
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock private RestaurantRepository restaurantRepository;
    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private MenuItemRepository itemRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private RegisterRestaurantRequest validRequest;
    private Restaurant savedRestaurant;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRestaurantRequest();
        validRequest.setName("Test Restaurant");
        validRequest.setCuisine("North Indian");
        validRequest.setAddress("123 Test Street");
        validRequest.setCity("Delhi");
        validRequest.setLatitude(28.6139);
        validRequest.setLongitude(77.2090);
        validRequest.setPhone("9876543210");

        savedRestaurant = Restaurant.builder()
                .restaurantId(1L)
                .ownerId(10L)
                .name("Test Restaurant")
                .cuisine("North Indian")
                .city("Delhi")
                .latitude(28.6139)
                .longitude(77.2090)
                .phone("9876543210")
                .approvalStatus("PENDING")
                .isOpen(false)
                .isActive(true)
                .build();
    }

    @Test
    void registerRestaurant_Success() {
        when(restaurantRepository.existsByOwnerIdAndNameIgnoreCase(10L, "Test Restaurant"))
                .thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(savedRestaurant);
        when(modelMapper.map(any(Restaurant.class), eq(RestaurantResponse.class)))
                .thenReturn(new RestaurantResponse());

        RestaurantResponse response = restaurantService.registerRestaurant(validRequest, 10L);

        assertNotNull(response);
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    void registerRestaurant_Duplicate_ThrowsException() {
        when(restaurantRepository.existsByOwnerIdAndNameIgnoreCase(10L, "Test Restaurant"))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> restaurantService.registerRestaurant(validRequest, 10L));

        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(com.quickbite.restaurant.exception.ResourceNotFoundException.class,
                () -> restaurantService.getById(999L));
    }

    @Test
    void toggleOpen_NotApproved_ThrowsException() {
        Restaurant pendingRestaurant = Restaurant.builder()
                .restaurantId(1L)
                .ownerId(10L)
                .approvalStatus("PENDING")
                .isActive(true)
                .build();

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(pendingRestaurant));

        assertThrows(IllegalArgumentException.class,
                () -> restaurantService.toggleOpen(1L, 10L));
    }
}
