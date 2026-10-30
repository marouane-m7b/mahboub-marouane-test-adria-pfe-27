package net.mahboub.transactionservice;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import net.mahboub.transactionservice.client.AccountClient;
import net.mahboub.transactionservice.client.AccountGateway;
import net.mahboub.transactionservice.service.AccountUnavailableException;
import net.mahboub.transactionservice.service.TransactionRejectedException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountGatewayTests {

    @Test
    void opensCircuitAfterFailuresAndStopsRemoteCalls() {
        var client = mock(AccountClient.class);
        UUID id = UUID.randomUUID();
        var failure = mock(FeignException.class);
        when(failure.status()).thenReturn(503);
        when(client.getAccount(id)).thenThrow(failure);
        var registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(3).minimumNumberOfCalls(3).failureRateThreshold(50).build());
        var gateway = new AccountGateway(client, registry);
        for (int i = 0; i < 4; i++) {
            assertThrows(AccountUnavailableException.class, () -> gateway.getAccount(id));
        }
        verify(client, times(3)).getAccount(id);
        assertEquals("OPEN", registry.circuitBreaker("accounts").getState().name());
    }

    @Test
    void translatesUnknownAccountToBusinessRejection() {
        var client = mock(AccountClient.class);
        UUID id = UUID.randomUUID();
        var failure = mock(FeignException.class);
        when(failure.status()).thenReturn(404);
        when(client.getAccount(id)).thenThrow(failure);
        var gateway = new AccountGateway(client, CircuitBreakerRegistry.ofDefaults());
        assertThrows(TransactionRejectedException.class, () -> gateway.getAccount(id));
    }
}
