package com.quickbite.order.dto;

import java.util.List;
import lombok.*;

/**
 * RevenueAnalyticsDTO
 *
 * Returned by earnings analytics endpoint for restaurant owners.
 * As per PDF Section 2.3:
 *  "View earnings analytics: daily/weekly/monthly revenue, top-selling items, peak hours"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueAnalyticsDTO {

    private int restaurantId;
    private String restaurantName;

    // â”€â”€ Revenue totals â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private double totalRevenue;
    private double dailyRevenue;
    private double weeklyRevenue;
    private double monthlyRevenue;

    // â”€â”€ Order counts â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private long totalOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long pendingOrders;

    private List<TopSellingItemDTO> topSellingItems;
    private List<PeakHourDTO> peakHours;
}
