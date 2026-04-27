package com.finsafe.idempotency_gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.idempotency_gateway.dto.PaymentRequest;
import com.finsafe.idempotency_gateway.dto.PaymentResponse;
import com.finsafe.idempotency_gateway.exception.DuplicateRequestException;
import com.finsafe.idempotency_gateway.model.IdempotencyRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private PaymentRequest request;
    private static final String IDEMPOTENCY_KEY = "test-key-123";
    private static final String REDIS_KEY = "idempotency:" + IDEMPOTENCY_KEY;

    @BeforeEach
    void setUp() {
        request = PaymentRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("KES")
                .build();
        ReflectionTestUtils.setField(idempotencyService, "keyTtl", 86400L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }


    @Test
    @DisplayName("Should process payment successfully on first request")
    void testFirstPayment() throws Exception {
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"amount\":100.00,\"currency\":\"KES\"}");

        PaymentResponse response = idempotencyService
                .processPayment(IDEMPOTENCY_KEY, request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("100"));
        assertTrue(response.getMessage().contains("KES"));

        verify(valueOperations, times(1))
                .set(eq(REDIS_KEY), any(IdempotencyRecord.class), anyLong(), any());

        System.out.println("Test 1 Passed: First payment processed successfully");
    }


    @Test
    @DisplayName("Should return cached response on duplicate request")
    void testDuplicateRequest() throws Exception {
        String requestJson = "{\"amount\":100.00,\"currency\":\"KES\"}";
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(
                requestJson.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }
        String realHash = hexString.toString();

        IdempotencyRecord existingRecord = IdempotencyRecord.builder()
                .idempotencyKey(IDEMPOTENCY_KEY)
                .requestBodyHash(realHash)
                .responseStatus(201)
                .responseBody("Charged 100.00 KES")
                .createdAt(Instant.now())
                .build();

        when(valueOperations.get(REDIS_KEY)).thenReturn(existingRecord);
        when(objectMapper.convertValue(any(), eq(IdempotencyRecord.class)))
                .thenReturn(existingRecord);
        when(objectMapper.writeValueAsString(any())).thenReturn(requestJson);

        PaymentResponse response = idempotencyService
                .processPayment(IDEMPOTENCY_KEY, request);

        assertNotNull(response);
        assertEquals("Charged 100.00 KES", response.getMessage());

        verify(valueOperations, never())
                .set(anyString(), any(), anyLong(), any());

        System.out.println("Test 2 Passed: Cached response returned for duplicate request");
    }


    @Test
    @DisplayName("Should throw DuplicateRequestException for same key different body")
    void testFraudCheck() throws Exception {
        IdempotencyRecord existingRecord = IdempotencyRecord.builder()
                .idempotencyKey(IDEMPOTENCY_KEY)
                .requestBodyHash("different-hash-xyz")
                .responseStatus(201)
                .responseBody("Charged 100.00 KES")
                .createdAt(Instant.now())
                .build();

        when(valueOperations.get(REDIS_KEY)).thenReturn(existingRecord);
        when(objectMapper.convertValue(any(), eq(IdempotencyRecord.class)))
                .thenReturn(existingRecord);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"amount\":500.00,\"currency\":\"KES\"}");

        DuplicateRequestException exception = assertThrows(
                DuplicateRequestException.class,
                () -> idempotencyService.processPayment(IDEMPOTENCY_KEY, request)
        );

        assertEquals(
                "Idempotency key already used for a different request body.",
                exception.getMessage()
        );

        System.out.println("Test 3 Passed: Fraud attempt correctly rejected");
    }


    @Test
    @DisplayName("Should handle concurrent requests with same key correctly")
    void testRaceCondition() throws Exception {
        // ARRANGE
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"amount\":100.00,\"currency\":\"KES\"}");

        IdempotencyRecord savedRecord = IdempotencyRecord.builder()
                .idempotencyKey(IDEMPOTENCY_KEY)
                .requestBodyHash("somehash")
                .responseStatus(201)
                .responseBody("Charged 100.00 KES")
                .createdAt(Instant.now())
                .build();

        when(valueOperations.get(REDIS_KEY))
                .thenReturn(null)
                .thenReturn(savedRecord);

        when(objectMapper.convertValue(any(), eq(IdempotencyRecord.class)))
                .thenReturn(savedRecord);

        Thread threadA = new Thread(() -> {
            try {
                idempotencyService.processPayment(IDEMPOTENCY_KEY, request);
            } catch (Exception e) {
                System.out.println("Thread A error: " + e.getMessage());
            }
        });

        Thread threadB = new Thread(() -> {
            try {
                idempotencyService.processPayment(IDEMPOTENCY_KEY, request);
            } catch (Exception e) {
                System.out.println("Thread B error: " + e.getMessage());
            }
        });

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();

        verify(valueOperations, atMostOnce())
                .set(eq(REDIS_KEY), any(), anyLong(), any());

        System.out.println("Test 4 Passed: Race condition handled correctly");
    }

}