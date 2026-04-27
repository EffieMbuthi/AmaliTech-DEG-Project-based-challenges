package com.pulsecheck.pulse_check.service;

import com.pulsecheck.pulse_check.dto.MonitorRequest;
import com.pulsecheck.pulse_check.dto.MonitorResponse;
import com.pulsecheck.pulse_check.exception.MonitorAlreadyExistsException;
import com.pulsecheck.pulse_check.exception.MonitorNotFoundException;
import com.pulsecheck.pulse_check.model.Monitor;
import com.pulsecheck.pulse_check.model.MonitorStatus;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MonitorService {
    private final ConcurrentHashMap<String, Monitor> monitors= new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public MonitorResponse registerMonitor(MonitorRequest request) {
        if (monitors.containsKey(request.getDeviceId())) {
            throw new MonitorAlreadyExistsException(request.getDeviceId());
        }
        Monitor monitor = Monitor.builder()
                .id(UUID.randomUUID())
                .deviceId(request.getDeviceId())
                .timeout(request.getTimeout())
                .alertEmail(request.getAlertEmail())
                .status(MonitorStatus.ACTIVE)
                .createdAt(Instant.now())
                .lastHeartbeat(Instant.now())
                .build();

        monitors.put(request.getDeviceId(), monitor);
        startTimer(monitor);
        log.info("Monitor registered for device: {} with timeout: {}s",
                monitor.getDeviceId(), monitor.getTimeout());

        return mapToResponse(monitor);
    }


    public MonitorResponse heartbeat(String deviceId) {
        Monitor monitor = getMonitorOrThrow(deviceId);
        if (monitor.getStatus() == MonitorStatus.DOWN) {
            throw new MonitorNotFoundException(
                    deviceId + " is DOWN and cannot send heartbeats. Re-register the device."
            );
        }
        cancelTimer(deviceId);
        monitor.setLastHeartbeat(Instant.now());
        monitor.setStatus(MonitorStatus.ACTIVE);
        startTimer(monitor);
        log.info("Heartbeat received for device: {} - timer reset to {}s",
                deviceId, monitor.getTimeout());

        return mapToResponse(monitor);
    }


    public MonitorResponse pauseMonitor(String deviceId) {
        Monitor monitor = getMonitorOrThrow(deviceId);
        cancelTimer(deviceId);
        monitor.setStatus(MonitorStatus.PAUSED);
        log.info("Monitor paused for device: {}", deviceId);

        return mapToResponse(monitor);
    }


    public MonitorResponse getMonitor(String deviceId) {
        Monitor monitor = getMonitorOrThrow(deviceId);
        return mapToResponse(monitor);
    }

    private void startTimer(Monitor monitor) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {monitor.setStatus(MonitorStatus.DOWN);
            log.error("ALERT: Device {} is DOWN! - Alert will be sent to: {}", monitor.getDeviceId(), monitor.getAlertEmail());
            System.out.println("{\"ALERT\": \"Device " + monitor.getDeviceId() + " is down!\", \"time\": \"" + Instant.now() + "\"}");
        }, monitor.getTimeout(), TimeUnit.SECONDS);

        timers.put(monitor.getDeviceId(), future);
    }

    private void cancelTimer(String deviceId) {
        ScheduledFuture<?> future = timers.get(deviceId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            timers.remove(deviceId);
            log.info("Timer cancelled for device: {}", deviceId);
        }
    }

    private Monitor getMonitorOrThrow(String deviceId) {
        Monitor monitor = monitors.get(deviceId);
        if (monitor == null) {
            throw new MonitorNotFoundException(deviceId);
        }
        return monitor;
    }

    private MonitorResponse mapToResponse(Monitor monitor) {
        return MonitorResponse.builder()
                .id(monitor.getId())
                .deviceId(monitor.getDeviceId())
                .timeout(monitor.getTimeout())
                .alertEmail(monitor.getAlertEmail())
                .status(monitor.getStatus())
                .createdAt(monitor.getCreatedAt())
                .lastHeartbeat(monitor.getLastHeartbeat())
                .build();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down monitor scheduler...");
        scheduler.shutdown();
    }

}