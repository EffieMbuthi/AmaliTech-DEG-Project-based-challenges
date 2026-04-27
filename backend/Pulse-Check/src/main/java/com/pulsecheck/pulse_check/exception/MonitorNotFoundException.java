package com.pulsecheck.pulse_check.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MonitorNotFoundException extends RuntimeException {
    public MonitorNotFoundException(String deviceId) {
        super("Monitor not found for device: " + deviceId);
    }

}