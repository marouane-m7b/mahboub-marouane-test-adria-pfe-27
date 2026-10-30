package net.mahboub.transactionservice.web.rest;

import net.mahboub.transactionservice.service.AccountUnavailableException;
import net.mahboub.transactionservice.service.TransactionRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class TransactionExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TransactionRejectedException.class)
    public ProblemDetail handleRejected(TransactionRejectedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(AccountUnavailableException.class)
    public ProblemDetail handleUnavailable(AccountUnavailableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }
}
