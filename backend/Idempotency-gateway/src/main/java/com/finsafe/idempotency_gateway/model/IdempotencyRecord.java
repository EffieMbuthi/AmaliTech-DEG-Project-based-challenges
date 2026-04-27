package com.finsafe.idempotency_gateway.model;

import lombok.*;

import java.time.Instant;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class IdempotencyRecord {
    private String idempotencyKey;
    private String requestBodyHash;
    private int responseStatus;
    private String responseBody;
    private Instant createdAt;

}
