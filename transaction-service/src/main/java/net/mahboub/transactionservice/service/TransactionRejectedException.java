package net.mahboub.transactionservice.service;

public class TransactionRejectedException extends RuntimeException {

    public TransactionRejectedException(String message) {
        super(message);
    }
}
