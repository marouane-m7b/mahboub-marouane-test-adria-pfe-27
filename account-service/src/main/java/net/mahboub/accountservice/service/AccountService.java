package net.mahboub.accountservice.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import net.mahboub.accountservice.domain.Account;
import net.mahboub.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccount(@NotNull UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional
    public Account createAccount(@NotNull @Valid CreateAccountRequest request) {
        Account account = Account.builder()
                .customerReference(request.customerReference())
                .status(request.status())
                .currency(request.currency())
                .balance(request.balance())
                .build();
        return accountRepository.save(account);
    }
}
