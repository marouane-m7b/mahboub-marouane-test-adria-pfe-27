package net.mahboub.transactionservice.client;

import java.util.UUID;

public record AccountSnapshot(UUID id, String status, String currency, double balance) {
}
