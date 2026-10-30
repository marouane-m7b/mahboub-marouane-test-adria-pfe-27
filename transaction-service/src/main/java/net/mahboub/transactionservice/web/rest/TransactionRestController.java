package net.mahboub.transactionservice.web.rest;

import jakarta.validation.Valid;
import net.mahboub.transactionservice.domain.BankTransaction;
import net.mahboub.transactionservice.service.CreateTransactionRequest;
import net.mahboub.transactionservice.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionRestController {

    private final TransactionService transactionService;

    public TransactionRestController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<BankTransaction> listTransactions(@RequestParam(required = false) UUID accountId) {
        return transactionService.listTransactions(accountId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankTransaction createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.createTransaction(request);
    }
}
