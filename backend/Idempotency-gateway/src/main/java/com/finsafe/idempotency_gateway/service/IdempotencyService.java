package com.finsafe.idempotency_gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsafe.idempotency_gateway.dto.PaymentRequest;
import com.finsafe.idempotency_gateway.dto.PaymentResponse;
import com.finsafe.idempotency_gateway.exception.DuplicateRequestException;
import com.finsafe.idempotency_gateway.exception.PaymentProcessingException;
import com.finsafe.idempotency_gateway.model.IdempotencyRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ReentrantLock> inFlightRequests
            = new ConcurrentHashMap<>();

    @Value("${idempotency.key.ttl}")
    private long keyTtl;

    private static final String REDIS_KEY_PREFIX = "idempotency:";

    public PaymentResponse processPayment(String idempotencyKey, PaymentRequest request) {
        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
        String requestHash = hashRequest(request);

        ReentrantLock lock = inFlightRequests.computeIfAbsent(idempotencyKey, k -> new ReentrantLock());
        lock.lock();

        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                IdempotencyRecord record = objectMapper.convertValue(cached, IdempotencyRecord.class);

                if (!record.getRequestBodyHash().equals(requestHash)) {
                    log.warn("Fraud attempt detected for key: {}", idempotencyKey);
                    throw new DuplicateRequestException(
                            "Idempotency key already used for a different request body."
                    );
                }

                log.info("Cache hit for idempotency key: {}", idempotencyKey);
                return buildPaymentResponse(record, idempotencyKey);
            }

            log.info("Processing new payment for key: {}", idempotencyKey);
            PaymentResponse response = simulatePaymentProcessing(request);
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .requestBodyHash(requestHash)
                    .responseStatus(201)
                    .responseBody(response.getMessage())
                    .createdAt(Instant.now())
                    .build();

            redisTemplate.opsForValue().set(
                    redisKey, record, keyTtl, TimeUnit.SECONDS
            );

            log.info("Payment processed and saved for key: {}", idempotencyKey);
            return response;

        } finally {
            lock.unlock();
            inFlightRequests.remove(idempotencyKey);
        }
    }

    public boolean isCacheHit(String idempotencyKey) {
        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    private PaymentResponse simulatePaymentProcessing(PaymentRequest request) {
        try {
            log.info("Simulating payment processing...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentProcessingException(
                    "Payment processing was interrupted."
            );
        }
        return PaymentResponse.builder()
                .message("Charged " + request.getAmount()
                        + " " + request.getCurrency())
                .status("SUCCESS")
                .processedAt(Instant.now())
                .build();
    }

    private PaymentResponse buildPaymentResponse(
            IdempotencyRecord record, String idempotencyKey) {

        return PaymentResponse.builder()
                .message(record.getResponseBody())
                .status("SUCCESS")
                .idempotencyKey(idempotencyKey)
                .processedAt(record.getCreatedAt())
                .build();
    }

    private String hashRequest(PaymentRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.error("Failed to hash request body: {}", e.getMessage());
            throw new PaymentProcessingException(
                    "Failed to process payment request."
            );
        }
    }

}