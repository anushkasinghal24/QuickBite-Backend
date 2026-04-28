package com.quickbite.order_service;

import com.quickbite.order.OrderServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = OrderServiceApplication.class)
@ActiveProfiles("test")
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
