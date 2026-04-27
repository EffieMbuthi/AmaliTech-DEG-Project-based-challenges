package com.finsafe.idempotency_gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IdempotencyKeyMissingException extends RuntimeException{
    public IdempotencyKeyMissingException(String message){
        super(message);
    }
}
