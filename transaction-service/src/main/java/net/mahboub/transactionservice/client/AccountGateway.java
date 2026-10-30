package net.mahboub.transactionservice.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import net.mahboub.transactionservice.service.AccountUnavailableException;
import net.mahboub.transactionservice.service.TransactionRejectedException;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Component
public class AccountGateway {

    private final AccountClient accountClient;
    private final CircuitBreaker circuitBreaker;

    public AccountGateway(AccountClient accountClient, CircuitBreakerRegistry registry) {
        this.accountClient = accountClient;
        this.circuitBreaker = registry.circuitBreaker("accounts");
    }

    public AccountSnapshot getAccount(UUID id) {
        return execute(() -> accountClient.getAccount(id));
    }

    public void debit(UUID id, DebitRequest request) {
        execute(() -> accountClient.debit(id, request));
    }

    public void transfer(net.mahboub.transactionservice.service.TransferRequest request) {
        execute(() -> accountClient.transfer(request));
    }

    private <T> T execute(Supplier<T> operation) {
        try {
            return circuitBreaker.executeSupplier(operation);
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new TransactionRejectedException("Compte introuvable");
            }
            if (exception.status() == 400) {
                throw new TransactionRejectedException("Operation refusee par le service comptes");
            }
            throw new AccountUnavailableException();
        } catch (CallNotPermittedException exception) {
            throw new AccountUnavailableException();
        }
    }
}
