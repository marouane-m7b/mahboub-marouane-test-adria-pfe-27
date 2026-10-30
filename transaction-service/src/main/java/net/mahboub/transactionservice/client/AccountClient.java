package net.mahboub.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "account-service", url = "${bank.account-service.url}")
public interface AccountClient {

    @GetMapping("/accounts/{id}")
    AccountSnapshot getAccount(@PathVariable("id") UUID id);

    @PostMapping("/accounts/{id}/debit")
    AccountSnapshot debit(@PathVariable("id") UUID id, @RequestBody DebitRequest request);
}
