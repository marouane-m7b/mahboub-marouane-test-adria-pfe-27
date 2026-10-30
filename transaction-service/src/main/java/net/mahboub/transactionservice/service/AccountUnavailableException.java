package net.mahboub.transactionservice.service;

public class AccountUnavailableException extends RuntimeException {

    public AccountUnavailableException() {
        super("Service comptes indisponible. Operation non confirmee : verifier le solde avant toute nouvelle tentative.");
    }
}
