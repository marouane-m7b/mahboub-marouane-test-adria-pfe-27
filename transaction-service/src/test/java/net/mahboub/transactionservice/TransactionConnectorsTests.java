package net.mahboub.transactionservice;

import net.mahboub.transactionservice.client.AccountGateway;
import net.mahboub.transactionservice.client.AccountSnapshot;
import net.mahboub.transactionservice.client.DebitRequest;
import net.mahboub.transactionservice.repository.TransactionRepository;
import net.mahboub.transactionservice.service.AccountUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionConnectorsTests {

    @LocalServerPort
    private int port;

    @MockitoBean
    private AccountGateway gateway;

    @Autowired
    private TransactionRepository repository;

    private final UUID accountId = UUID.randomUUID();
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(gateway.getAccount(accountId)).thenReturn(new AccountSnapshot(accountId, "ACTIVE", "MAD", 2500));
    }

    @Test
    void createsThroughGraphqlAndListsThroughRest() throws Exception {
        var result = graphql(mutation(250));
        assertFalse(result.has("errors"), result.toString());
        assertEquals(1, repository.count());
        assertFalse(result.path("data").path("createTransaction").path("createdAt").asString().isBlank());
        verify(gateway).debit(accountId, new DebitRequest(new BigDecimal("250.0"), "MAD"));
        var list = graphql("{ transactions(accountId: \"" + accountId + "\") { id amount } }");
        assertFalse(list.has("errors"), list.toString());
        assertEquals(1, list.path("data").path("transactions").size());
        var rest = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/transactions?accountId=" + accountId))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, rest.statusCode());
        assertTrue(rest.body().contains(accountId.toString()));
    }

    @Test
    void graphqlRejectsInvalidAmountAndLimit() throws Exception {
        for (int amount : new int[]{-100, 1500}) {
            var result = graphql(mutation(amount));
            assertEquals("BAD_REQUEST", result.path("errors").get(0).path("extensions").path("classification").asString());
        }
        assertEquals(0, repository.count());
        verify(gateway, never()).debit(any(), any());
    }

    @Test
    void rollsBackTransactionWhenRemoteDebitFails() throws Exception {
        doThrow(new AccountUnavailableException()).when(gateway).debit(any(), any());
        var result = graphql(mutation(250));
        assertTrue(result.has("errors"));
        assertEquals(0, repository.count());
    }

    private String mutation(int amount) {
        return "mutation { createTransaction(input: {accountId: \"" + accountId
                + "\", amount: " + amount + ", currency: \"MAD\", merchantCategory: \"SHOPPING\", country: \"MA\"})"
                + " { id amount createdAt } }";
    }

    private JsonNode graphql(String query) throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/graphql"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("query", query)))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }
}
