package com.pulsecheck.pulse_check.controller;

import com.pulsecheck.pulse_check.dto.MonitorRequest;
import com.pulsecheck.pulse_check.dto.MonitorResponse;
import com.pulsecheck.pulse_check.service.MonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MonitorController {
    private final MonitorService monitorService;

    @PostMapping("/monitors")
    public ResponseEntity<MonitorResponse> registerMonitor(@Valid @RequestBody MonitorRequest request) {
        log.info("Registering monitor for device: {}", request.getDeviceId());
        MonitorResponse response = monitorService.registerMonitor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/monitors/{deviceId}/heartbeat")
    public ResponseEntity<MonitorResponse> heartbeat(@PathVariable String deviceId) {
        log.info("Heartbeat received for device: {}", deviceId);
        MonitorResponse response = monitorService.heartbeat(deviceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/monitors/{deviceId}/pause")
    public ResponseEntity<MonitorResponse> pauseMonitor(@PathVariable String deviceId) {
        log.info("Pausing monitor for device: {}", deviceId);
        MonitorResponse response = monitorService.pauseMonitor(deviceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monitors/{deviceId}")
    public ResponseEntity<MonitorResponse> getMonitor(@PathVariable String deviceId) {
        log.info("Getting monitor for device: {}", deviceId);
        MonitorResponse response = monitorService.getMonitor(deviceId);
        return ResponseEntity.ok(response);
    }

}