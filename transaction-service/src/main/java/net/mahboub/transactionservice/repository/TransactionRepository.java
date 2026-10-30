package net.mahboub.transactionservice.repository;

import net.mahboub.transactionservice.domain.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<BankTransaction, UUID> {

    List<BankTransaction> findByAccountId(UUID accountId);
}
