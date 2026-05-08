package com.quickbite.payment.dto;

import lombok.Data;

@Data
public class UserContactDTO {
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
}
