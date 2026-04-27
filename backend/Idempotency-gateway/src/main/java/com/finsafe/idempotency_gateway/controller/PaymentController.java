package com.finsafe.idempotency_gateway.controller;

import com.finsafe.idempotency_gateway.dto.PaymentRequest;
import com.finsafe.idempotency_gateway.dto.PaymentResponse;
import com.finsafe.idempotency_gateway.service.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PaymentController {

    private final IdempotencyService idempotencyService;

    @PostMapping("/process-payment")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        log.info("Received payment request with key: {}", idempotencyKey);

        boolean cacheHit = idempotencyService.isCacheHit(idempotencyKey);

        PaymentResponse response = idempotencyService.processPayment(idempotencyKey, request);

        HttpHeaders headers = new HttpHeaders();
        if (cacheHit) {
            headers.add("X-Cache-Hit", "true");
            log.info("Returning cached response for key: {}", idempotencyKey);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .headers(headers)
                    .body(response);
        }

        log.info("Returning fresh response for key: {}", idempotencyKey);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .headers(headers)
                .body(response);
    }

}