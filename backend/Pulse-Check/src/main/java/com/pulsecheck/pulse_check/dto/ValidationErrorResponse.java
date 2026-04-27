package com.pulsecheck.pulse_check.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ValidationErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private Map<String, String> details;

}