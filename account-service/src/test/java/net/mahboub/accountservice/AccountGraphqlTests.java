package net.mahboub.accountservice;

import net.mahboub.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountGraphqlTests {

    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository repository;

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @BeforeEach
    void clearAccounts() {
        repository.deleteAll();
    }

    @Test
    void createsAccountVisibleThroughGraphqlAndRest() throws Exception {
        var result = execute("""
                mutation {
                  createAccount(input: {customerReference: "CLIENT-GQL", status: ACTIVE,
                      currency: "MAD", balance: 1500}) { id customerReference balance }
                }
                """);
        assertFalse(result.has("errors"), result.toString());
        String id = result.path("data").path("createAccount").path("id").asString();
        assertEquals(1, repository.count());
        var detail = execute("{ account(id: \"" + id + "\") { id customerReference } }");
        assertFalse(detail.has("errors"), detail.toString());
        assertEquals("CLIENT-GQL", detail.path("data").path("account").path("customerReference").asString());
        var list = execute("{ accounts { id } }");
        assertFalse(list.has("errors"), list.toString());
        assertEquals(id, list.path("data").path("accounts").get(0).path("id").asString());
        var rest = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/accounts/" + id))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, rest.statusCode());
        assertTrue(rest.body().contains("CLIENT-GQL"));
    }

    @Test
    void returnsNotFoundForUnknownAccount() throws Exception {
        var result = execute("{ account(id: \"00000000-0000-0000-0000-000000000000\") { id } }");
        assertTrue(result.path("data").path("account").isNull());
        assertEquals("NOT_FOUND", result.path("errors").get(0).path("extensions").path("classification").asString());
    }

    @Test
    void rejectsInvalidInputsWithoutSaving() throws Exception {
        for (String input : new String[]{
                "customerReference: \"C1\", status: ACTIVE, currency: \"MAD\", balance: -100",
                "customerReference: \"\", status: ACTIVE, currency: \"MAD\", balance: 100",
                "customerReference: \"C1\", status: ACTIVE, currency: \"mad\", balance: 100"}) {
            var result = execute("mutation { createAccount(input: {" + input + "}) { id } }");
            assertTrue(result.path("data").path("createAccount").isNull());
            assertEquals("BAD_REQUEST", result.path("errors").get(0).path("extensions").path("classification").asString());
        }
        assertEquals(0, repository.count());
    }

    @Test
    void rejectsMalformedUuid() throws Exception {
        var result = execute("{ account(id: \"incorrect\") { id } }");
        assertEquals("BAD_REQUEST", result.path("errors").get(0).path("extensions").path("classification").asString());
    }

    private JsonNode execute(String query) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/graphql"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("query", query))))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }
}
