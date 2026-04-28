package com.quickbite.payment.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WalletStatementResponse {
    private Long statementId;
    private Long customerId;
    private Double amount;
    private String type;          // CREDIT / DEBIT
    private String description;
    private Double closingBalance;
    private String referenceId;
    private LocalDateTime createdAt;
}
