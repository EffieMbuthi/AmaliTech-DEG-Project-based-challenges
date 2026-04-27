package com.pulsecheck.pulse_check.dto;

import com.pulsecheck.pulse_check.model.MonitorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class MonitorResponse {
    private UUID id;
    private String deviceId;
    private int timeout;
    private String alertEmail;
    private MonitorStatus status;
    private Instant createdAt;
    private Instant lastHeartbeat;

}