package net.mahboub.transactionservice.mcp;

import net.mahboub.transactionservice.domain.BankTransaction;
import net.mahboub.transactionservice.service.TransferRequest;
import net.mahboub.transactionservice.service.TransferResult;
import net.mahboub.transactionservice.service.CreateTransactionRequest;
import net.mahboub.transactionservice.service.TransactionService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TransactionMcpTools {

    private final TransactionService transactionService;

    public TransactionMcpTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @McpTool(name = "list_transactions", description = "Lister toutes les transactions bancaires")
    public List<BankTransaction> listTransactions() {
        return transactionService.listTransactions(null);
    }

    @McpTool(name = "get_account_transactions", description = "Lister les transactions d'un compte")
    public List<BankTransaction> getAccountTransactions(
            @McpToolParam(description = "UUID du compte", required = true) UUID accountId) {
        return transactionService.listTransactions(accountId);
    }

    @McpTool(name = "create_transaction", description = "Enregistrer une transaction et debiter le compte")
    public BankTransaction createTransaction(
            @McpToolParam(description = "Compte, montant positif, devise, categorie commercant et pays",
                    required = true) CreateTransactionRequest request) {
        return transactionService.createTransaction(request);
    }

    @McpTool(name = "transfer_money", description = "Effectuer un virement entre deux comptes actifs de meme devise")
    public TransferResult transfer(
            @McpToolParam(description = "Compte source, compte destination, montant et devise", required = true)
            TransferRequest request) {
        return transactionService.transfer(request);
    }
}
