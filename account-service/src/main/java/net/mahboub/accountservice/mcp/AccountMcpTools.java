package net.mahboub.accountservice.mcp;

import net.mahboub.accountservice.domain.Account;
import net.mahboub.accountservice.service.AccountService;
import net.mahboub.accountservice.service.CreateAccountRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AccountMcpTools {

    private final AccountService accountService;

    public AccountMcpTools(AccountService accountService) {
        this.accountService = accountService;
    }

    @McpTool(name = "list_accounts", description = "Lister les comptes bancaires")
    public List<Account> listAccounts() {
        return accountService.listAccounts();
    }

    @McpTool(name = "get_account", description = "Consulter un compte bancaire par son UUID")
    public Account getAccount(@McpToolParam(description = "UUID du compte", required = true) UUID id) {
        return accountService.getAccount(id);
    }

    @McpTool(name = "create_account", description = "Creer un compte bancaire avec un solde initial positif ou nul")
    public Account createAccount(
            @McpToolParam(description = "Reference client, etat ACTIVE ou BLOCKED, devise et solde initial",
                    required = true) CreateAccountRequest request) {
        return accountService.createAccount(request);
    }
}
