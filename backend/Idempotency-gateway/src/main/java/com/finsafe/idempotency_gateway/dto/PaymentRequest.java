package com.finsafe.idempotency_gateway.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class PaymentRequest {
    @NotNull(message= "Amount is required")
    @DecimalMin(value= "0.01", message= "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message= "Currency is required")
    @Size(min=3, max=3, message= "Currency must be a 3Letter ISO code eg: KES, USD")
    private String currency;
}
