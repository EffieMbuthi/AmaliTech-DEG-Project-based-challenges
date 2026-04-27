package com.pulsecheck.pulse_check.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MonitorAlreadyExistsException extends RuntimeException {
    public MonitorAlreadyExistsException(String deviceId) {
        super("Monitor already exists for device: " + deviceId);
    }
}