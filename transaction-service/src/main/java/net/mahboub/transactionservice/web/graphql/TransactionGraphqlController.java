package net.mahboub.transactionservice.web.graphql;

import graphql.GraphQLError;
import net.mahboub.transactionservice.service.TransferRequest;
import net.mahboub.transactionservice.service.TransferResult;
import graphql.GraphqlErrorBuilder;
import jakarta.validation.ConstraintViolationException;
import net.mahboub.transactionservice.domain.BankTransaction;
import net.mahboub.transactionservice.service.AccountUnavailableException;
import net.mahboub.transactionservice.service.CreateTransactionRequest;
import net.mahboub.transactionservice.service.TransactionRejectedException;
import net.mahboub.transactionservice.service.TransactionService;
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
public class TransactionGraphqlController {

    private final TransactionService transactionService;

    public TransactionGraphqlController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public List<BankTransaction> transactions(@Argument UUID accountId) {
        return transactionService.listTransactions(accountId);
    }

    @MutationMapping
    public BankTransaction createTransaction(@Argument("input") CreateTransactionRequest input) {
        return transactionService.createTransaction(input);
    }

    @MutationMapping
    public TransferResult transfer(@Argument("input") TransferRequest input) {
        return transactionService.transfer(input);
    }

    @GraphQlExceptionHandler(TransactionRejectedException.class)
    public GraphQLError handleRejected(TransactionRejectedException exception, GraphqlErrorBuilder<?> builder) {
        return builder.errorType(ErrorType.BAD_REQUEST).message(exception.getMessage()).build();
    }

    @GraphQlExceptionHandler({ConstraintViolationException.class, BindException.class})
    public GraphQLError handleInvalid(Exception exception, GraphqlErrorBuilder<?> builder) {
        return builder.errorType(ErrorType.BAD_REQUEST).message("Donnees de transaction invalides").build();
    }

    @GraphQlExceptionHandler(AccountUnavailableException.class)
    public GraphQLError handleUnavailable(AccountUnavailableException exception, GraphqlErrorBuilder<?> builder) {
        return builder.errorType(ErrorType.INTERNAL_ERROR).message(exception.getMessage()).build();
    }
}
