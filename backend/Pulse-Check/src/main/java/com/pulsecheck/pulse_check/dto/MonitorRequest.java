package com.pulsecheck.pulse_check.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class MonitorRequest {
    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotNull(message = "Timeout is required")
    @Min(value = 1, message = "Timeout must be at least 1 second")
    private Integer timeout;

    @NotBlank(message = "Alert email is required")
    @Email(message = "Alert email must be a valid email address")
    private String alertEmail;

}