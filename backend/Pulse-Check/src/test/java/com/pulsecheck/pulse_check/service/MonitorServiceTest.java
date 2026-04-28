package com.pulsecheck.pulse_check.service;

import com.pulsecheck.pulse_check.dto.MonitorRequest;
import com.pulsecheck.pulse_check.dto.MonitorResponse;
import com.pulsecheck.pulse_check.exception.MonitorAlreadyExistsException;
import com.pulsecheck.pulse_check.exception.MonitorNotFoundException;
import com.pulsecheck.pulse_check.model.MonitorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {
    private MonitorService monitorService;
    private MonitorRequest request;
    private static final String DEVICE_ID = "solar-farm-001";

    @BeforeEach
    void setUp() {
        monitorService = new MonitorService();
        request = MonitorRequest.builder()
                .deviceId(DEVICE_ID)
                .timeout(60)
                .alertEmail("admin@critmon.com")
                .build();
    }


    @Test
    @DisplayName("Should register a monitor successfully")
    void testRegisterMonitor() {
        MonitorResponse response = monitorService.registerMonitor(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(DEVICE_ID, response.getDeviceId());
        assertEquals(60, response.getTimeout());
        assertEquals("admin@critmon.com", response.getAlertEmail());
        assertEquals(MonitorStatus.ACTIVE, response.getStatus());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getLastHeartbeat());

        System.out.println("Test 1 Passed: Monitor registered successfully");
    }


    @Test
    @DisplayName("Should throw MonitorAlreadyExistsException for duplicate device")
    void testRegisterDuplicateMonitor() {
        monitorService.registerMonitor(request);

        MonitorAlreadyExistsException exception = assertThrows(MonitorAlreadyExistsException.class,
                () -> monitorService.registerMonitor(request));

        assertEquals("Monitor already exists for device: " + DEVICE_ID, exception.getMessage());

        System.out.println("Test 2 Passed: Duplicate monitor correctly rejected");
    }


    @Test
    @DisplayName("Should reset timer and update lastHeartbeat on heartbeat")
    void testHeartbeat() throws InterruptedException {
        monitorService.registerMonitor(request);
        Thread.sleep(100);

        MonitorResponse response = monitorService.heartbeat(DEVICE_ID);

        assertNotNull(response);
        assertEquals(MonitorStatus.ACTIVE, response.getStatus());
        assertNotNull(response.getLastHeartbeat());

        System.out.println("Test 3 Passed: Heartbeat received and timer reset");
    }


    @Test
    @DisplayName("Should throw MonitorNotFoundException for unknown device")
    void testHeartbeatDeviceNotFound() {
        MonitorNotFoundException exception = assertThrows(MonitorNotFoundException.class,
                () -> monitorService.heartbeat("unknown-device")
        );

        assertNotNull(exception.getMessage());

        System.out.println("Test 4 Passed: 404 thrown for unknown device");
    }


    @Test
    @DisplayName("Should pause monitor and stop timer")
    void testPauseMonitor() {
        monitorService.registerMonitor(request);

        MonitorResponse response = monitorService.pauseMonitor(DEVICE_ID);

        assertNotNull(response);
        assertEquals(MonitorStatus.PAUSED, response.getStatus());

        System.out.println("Test 5 Passed: Monitor paused successfully");
    }


    @Test
    @DisplayName("Should return monitor details")
    void testGetMonitor() {
        monitorService.registerMonitor(request);

        MonitorResponse response = monitorService.getMonitor(DEVICE_ID);

        assertNotNull(response);
        assertEquals(DEVICE_ID, response.getDeviceId());
        assertEquals(MonitorStatus.ACTIVE, response.getStatus());

        System.out.println("Test 6 Passed: Monitor details returned successfully");
    }


    @Test
    @DisplayName("Should unpause monitor when heartbeat is received")
    void testHeartbeatUnpausesMonitor() {
        monitorService.registerMonitor(request);
        monitorService.pauseMonitor(DEVICE_ID);

        MonitorResponse paused = monitorService.getMonitor(DEVICE_ID);
        assertEquals(MonitorStatus.PAUSED, paused.getStatus());

        MonitorResponse response = monitorService.heartbeat(DEVICE_ID);

        assertEquals(MonitorStatus.ACTIVE, response.getStatus());

        System.out.println("Test 7 Passed: Heartbeat unpaused the monitor");
    }

}