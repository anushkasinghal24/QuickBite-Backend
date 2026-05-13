package com.quickbite.payment.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WalletResponse {
    private Long walletId;
    private Long customerId;
    private Double balance;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
