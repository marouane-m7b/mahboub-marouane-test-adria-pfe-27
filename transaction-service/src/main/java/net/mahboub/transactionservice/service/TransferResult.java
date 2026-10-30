package net.mahboub.transactionservice.service;

import net.mahboub.transactionservice.domain.BankTransaction;
import java.util.UUID;

public record TransferResult(UUID transferId, BankTransaction debit, BankTransaction credit) {
}
