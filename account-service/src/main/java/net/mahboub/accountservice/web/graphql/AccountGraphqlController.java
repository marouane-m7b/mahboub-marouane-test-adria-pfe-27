package net.mahboub.accountservice.web.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import jakarta.validation.ConstraintViolationException;
import net.mahboub.accountservice.domain.Account;
import net.mahboub.accountservice.service.AccountNotFoundException;
import net.mahboub.accountservice.service.AccountService;
import net.mahboub.accountservice.service.CreateAccountRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;

import java.util.List;
import java.util.UUID;

@Controller
public class AccountGraphqlController {

    private final AccountService accountService;

    public AccountGraphqlController(AccountService accountService) {
        this.accountService = accountService;
    }

    @QueryMapping
    public List<Account> accounts() {
        return accountService.listAccounts();
    }

    @QueryMapping
    public Account account(@Argument UUID id) {
        return accountService.getAccount(id);
    }

    @MutationMapping
    public Account createAccount(@Argument("input") CreateAccountRequest input) {
        return accountService.createAccount(input);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(AccountNotFoundException exception, GraphqlErrorBuilder<?> builder) {
        return builder.errorType(ErrorType.NOT_FOUND).message(exception.getMessage()).build();
    }

    @GraphQlExceptionHandler({ConstraintViolationException.class, BindException.class})
    public GraphQLError handleInvalidInput(Exception exception, GraphqlErrorBuilder<?> builder) {
        return builder.errorType(ErrorType.BAD_REQUEST).message("Donnees du compte invalides").build();
    }
}
