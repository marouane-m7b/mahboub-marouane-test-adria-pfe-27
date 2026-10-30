package net.mahboub.accountservice.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import net.mahboub.accountservice.domain.Account;
import net.mahboub.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import net.mahboub.accountservice.domain.AccountStatus;
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

    @Transactional
    public Account debit(@NotNull UUID id, @NotNull @Valid DebitRequest request) {
        Account account = accountRepository.findLockedById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountOperationException("Le compte est bloque");
        }
        if (!account.getCurrency().equals(request.currency())) {
            throw new AccountOperationException("La devise ne correspond pas au compte");
        }
        BigDecimal balance = BigDecimal.valueOf(account.getBalance());
        if (balance.compareTo(request.amount()) < 0) {
            throw new AccountOperationException("Solde insuffisant");
        }
        account.setBalance(balance.subtract(request.amount()).doubleValue());
        return accountRepository.save(account);
    }

    @Transactional
    public List<Account> transfer(@NotNull @Valid TransferRequest request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new AccountOperationException("Les comptes doivent etre differents");
        }
        // Stable lock order prevents opposite transfers from locking accounts in reverse order.
        List<UUID> ids = java.util.stream.Stream.of(request.sourceAccountId(), request.destinationAccountId())
                .sorted().toList();
        Account first = accountRepository.findLockedById(ids.get(0))
                .orElseThrow(() -> new AccountNotFoundException(ids.get(0)));
        Account second = accountRepository.findLockedById(ids.get(1))
                .orElseThrow(() -> new AccountNotFoundException(ids.get(1)));
        Account source = first.getId().equals(request.sourceAccountId()) ? first : second;
        Account destination = source == first ? second : first;
        if (source.getStatus() != AccountStatus.ACTIVE || destination.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountOperationException("Les deux comptes doivent etre actifs");
        }
        if (!source.getCurrency().equals(request.currency()) || !destination.getCurrency().equals(request.currency())) {
            throw new AccountOperationException("Les comptes doivent avoir la meme devise que le virement");
        }
        BigDecimal balance = BigDecimal.valueOf(source.getBalance());
        if (balance.compareTo(request.amount()) < 0) {
            throw new AccountOperationException("Solde insuffisant");
        }
        source.setBalance(balance.subtract(request.amount()).doubleValue());
        destination.setBalance(BigDecimal.valueOf(destination.getBalance()).add(request.amount()).doubleValue());
        return List.of(source, destination);
    }
}
