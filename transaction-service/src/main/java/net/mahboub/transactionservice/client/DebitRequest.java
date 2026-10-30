package net.mahboub.transactionservice.client;

import java.math.BigDecimal;

public record DebitRequest(BigDecimal amount, String currency) {
}
