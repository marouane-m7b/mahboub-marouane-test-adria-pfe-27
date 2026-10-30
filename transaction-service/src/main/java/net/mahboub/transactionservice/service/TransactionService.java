package net.mahboub.transactionservice.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import net.mahboub.transactionservice.client.AccountGateway;
import net.mahboub.transactionservice.client.AccountSnapshot;
import net.mahboub.transactionservice.client.DebitRequest;
import net.mahboub.transactionservice.domain.BankTransaction;
import net.mahboub.transactionservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository repository;
    private final AccountGateway accountGateway;
    private final BigDecimal maxAmount;

    public TransactionService(TransactionRepository repository, AccountGateway accountGateway,
            @Value("${bank.transaction.max-amount}") BigDecimal maxAmount) {
        this.repository = repository;
        this.accountGateway = accountGateway;
        this.maxAmount = maxAmount;
    }

    public List<BankTransaction> listTransactions(UUID accountId) {
        return accountId == null ? repository.findAll() : repository.findByAccountId(accountId);
    }

    @Transactional
    public BankTransaction createTransaction(@NotNull @Valid CreateTransactionRequest request) {
        if (request.amount().compareTo(maxAmount) > 0) {
            throw new TransactionRejectedException("Le montant depasse le plafond de " + maxAmount);
        }
        AccountSnapshot account = accountGateway.getAccount(request.accountId());
        if (!"ACTIVE".equals(account.status())) {
            throw new TransactionRejectedException("Le compte est bloque");
        }
        if (!request.currency().equals(account.currency())) {
            throw new TransactionRejectedException("La devise ne correspond pas au compte");
        }
        if (BigDecimal.valueOf(account.balance()).compareTo(request.amount()) < 0) {
            throw new TransactionRejectedException("Solde insuffisant");
        }
        BankTransaction transaction = BankTransaction.builder()
                .accountId(request.accountId())
                .amount(request.amount())
                .currency(request.currency())
                .merchantCategory(request.merchantCategory())
                .country(request.country())
                .createdAt(Instant.now())
                .build();
        transaction = repository.saveAndFlush(transaction);
        accountGateway.debit(request.accountId(), new DebitRequest(request.amount(), request.currency()));
        return transaction;
    }
}
