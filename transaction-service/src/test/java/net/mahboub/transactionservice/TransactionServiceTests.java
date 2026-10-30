package net.mahboub.transactionservice;

import net.mahboub.transactionservice.client.AccountGateway;
import net.mahboub.transactionservice.client.AccountSnapshot;
import net.mahboub.transactionservice.client.DebitRequest;
import net.mahboub.transactionservice.domain.BankTransaction;
import net.mahboub.transactionservice.repository.TransactionRepository;
import net.mahboub.transactionservice.service.AccountUnavailableException;
import net.mahboub.transactionservice.service.CreateTransactionRequest;
import net.mahboub.transactionservice.service.TransactionRejectedException;
import net.mahboub.transactionservice.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTests {

    private TransactionRepository repository;
    private AccountGateway gateway;
    private TransactionService service;
    private final UUID accountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(TransactionRepository.class);
        gateway = mock(AccountGateway.class);
        service = new TransactionService(repository, gateway, new BigDecimal("1000"), mock(org.springframework.context.ApplicationEventPublisher.class));
    }

    @Test
    void savesTransactionThenDebitsAccount() {
        when(gateway.getAccount(accountId)).thenReturn(new AccountSnapshot(accountId, "ACTIVE", "MAD", 2000));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        BankTransaction result = service.createTransaction(request("250", "MAD"));
        assertEquals(new BigDecimal("250"), result.getAmount());
        assertEquals(accountId, result.getAccountId());
        assertNotNull(result.getCreatedAt());
        var order = inOrder(gateway, repository);
        order.verify(gateway).getAccount(accountId);
        order.verify(repository).saveAndFlush(any());
        order.verify(gateway).debit(accountId, new DebitRequest(new BigDecimal("250"), "MAD"));
    }

    @Test
    void rejectsAmountAboveLimitBeforeCallingAccountService() {
        assertThrows(TransactionRejectedException.class, () -> service.createTransaction(request("1001", "MAD")));
        verifyNoInteractions(gateway, repository);
    }

    @Test
    void rejectsBlockedAccountCurrencyMismatchAndInsufficientBalance() {
        for (AccountSnapshot account : new AccountSnapshot[]{
                new AccountSnapshot(accountId, "BLOCKED", "MAD", 2000),
                new AccountSnapshot(accountId, "ACTIVE", "EUR", 2000),
                new AccountSnapshot(accountId, "ACTIVE", "MAD", 100)}) {
            when(gateway.getAccount(accountId)).thenReturn(account);
            assertThrows(TransactionRejectedException.class, () -> service.createTransaction(request("250", "MAD")));
        }
        verifyNoInteractions(repository);
        verify(gateway, never()).debit(any(), any());
    }

    @Test
    void failsWithoutSavingWhenAccountServiceIsUnavailable() {
        when(gateway.getAccount(accountId)).thenThrow(new AccountUnavailableException());
        assertThrows(AccountUnavailableException.class, () -> service.createTransaction(request("250", "MAD")));
        verifyNoInteractions(repository);
    }

    private CreateTransactionRequest request(String amount, String currency) {
        return new CreateTransactionRequest(accountId, new BigDecimal(amount), currency, "SHOPPING", "MA");
    }
}
