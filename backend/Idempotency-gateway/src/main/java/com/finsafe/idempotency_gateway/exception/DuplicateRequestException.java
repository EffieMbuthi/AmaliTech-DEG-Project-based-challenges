package com.finsafe.idempotency_gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class DuplicateRequestException extends RuntimeException  {
    public DuplicateRequestException (String message){
        super(message);

    }
}
