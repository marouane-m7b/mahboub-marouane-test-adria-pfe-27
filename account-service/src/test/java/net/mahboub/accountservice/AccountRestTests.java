package net.mahboub.accountservice;

import net.mahboub.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountRestTests {

    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void clearAccounts() {
        repository.deleteAll();
    }

    @Test
    void createsAndReadsAccount() throws Exception {
        var response = post("""
                {"customerReference":"CLIENT-001","status":"ACTIVE","currency":"MAD","balance":2500}
                """);
        assertEquals(201, response.statusCode());
        var account = repository.findAll().getFirst();
        assertNotNull(account.getId());
        assertEquals(2500.0, account.getBalance());
        assertEquals("MAD", account.getCurrency());
        assertEquals("/accounts/" + account.getId(), response.headers().firstValue("Location").orElseThrow());
        var detail = get("/accounts/" + account.getId());
        assertEquals(200, detail.statusCode());
        assertTrue(detail.body().contains(account.getId().toString()));
        var list = get("/accounts");
        assertEquals(200, list.statusCode());
        assertTrue(list.body().contains("CLIENT-001"));
    }

    @Test
    void returns404ForUnknownAccountAnd400ForMalformedUuid() throws Exception {
        assertEquals(404, get("/accounts/" + UUID.randomUUID()).statusCode());
        assertEquals(400, get("/accounts/not-a-uuid").statusCode());
    }

    @Test
    void rejectsInvalidAccountsWithoutSaving() throws Exception {
        for (String body : new String[]{
                "{\"customerReference\":\"\",\"status\":\"ACTIVE\",\"currency\":\"MAD\",\"balance\":100}",
                "{\"customerReference\":\"C1\",\"status\":\"ACTIVE\",\"currency\":\"MAD\",\"balance\":-1}",
                "{\"customerReference\":\"C1\",\"status\":\"ACTIVE\",\"currency\":\"mad\",\"balance\":100}",
                "{\"customerReference\":\"C1\",\"currency\":\"MAD\",\"balance\":100}",
                "{\"customerReference\":\"C1\",\"status\":\"ACTIVE\",\"currency\":\"MAD\"}"}) {
            assertEquals(400, post(body).statusCode(), body);
        }
        assertEquals(0, repository.count());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/accounts"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
