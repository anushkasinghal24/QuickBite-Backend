package com.quickbite.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PaymentAlreadyRefundedException extends RuntimeException {
    public PaymentAlreadyRefundedException(String message) { super(message); }
}
