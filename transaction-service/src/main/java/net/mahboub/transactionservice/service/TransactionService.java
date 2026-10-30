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
import org.springframework.context.ApplicationEventPublisher;
import net.mahboub.transactionservice.event.TransactionCreatedEvent;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Instant;
import net.mahboub.transactionservice.domain.TransactionDirection;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository repository;
    private final AccountGateway accountGateway;
    private final BigDecimal maxAmount;
    private final ApplicationEventPublisher events;

    public TransactionService(TransactionRepository repository, AccountGateway accountGateway,
            @Value("${bank.transaction.max-amount}") BigDecimal maxAmount, ApplicationEventPublisher events) {
        this.repository = repository;
        this.accountGateway = accountGateway;
        this.maxAmount = maxAmount;
        this.events = events;
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
        events.publishEvent(TransactionCreatedEvent.from(transaction));
        return transaction;
    }

    @Transactional
    public TransferResult transfer(@NotNull @Valid TransferRequest request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new TransactionRejectedException("Les comptes doivent etre differents");
        }
        if (request.amount().compareTo(maxAmount) > 0) {
            throw new TransactionRejectedException("Le montant depasse le plafond de " + maxAmount);
        }
        AccountSnapshot source = accountGateway.getAccount(request.sourceAccountId());
        AccountSnapshot destination = accountGateway.getAccount(request.destinationAccountId());
        if (!"ACTIVE".equals(source.status()) || !"ACTIVE".equals(destination.status())) {
            throw new TransactionRejectedException("Les deux comptes doivent etre actifs");
        }
        if (!request.currency().equals(source.currency()) || !request.currency().equals(destination.currency())) {
            throw new TransactionRejectedException("Les devises doivent correspondre");
        }
        if (BigDecimal.valueOf(source.balance()).compareTo(request.amount()) < 0) {
            throw new TransactionRejectedException("Solde insuffisant");
        }
        UUID transferId = UUID.randomUUID();
        Instant now = Instant.now();
        BankTransaction debit = repository.saveAndFlush(BankTransaction.builder()
                .accountId(request.sourceAccountId()).amount(request.amount()).currency(request.currency())
                .merchantCategory("TRANSFER").country("N/A").createdAt(now)
                .direction(TransactionDirection.DEBIT).transferId(transferId).build());
        BankTransaction credit = repository.saveAndFlush(BankTransaction.builder()
                .accountId(request.destinationAccountId()).amount(request.amount()).currency(request.currency())
                .merchantCategory("TRANSFER").country("N/A").createdAt(now)
                .direction(TransactionDirection.CREDIT).transferId(transferId).build());
        accountGateway.transfer(request);
        events.publishEvent(TransactionCreatedEvent.from(debit));
        events.publishEvent(TransactionCreatedEvent.from(credit));
        return new TransferResult(transferId, debit, credit);
    }
}
