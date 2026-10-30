package net.mahboub.accountservice.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import net.mahboub.accountservice.domain.AccountStatus;

public record CreateAccountRequest(
        @NotBlank String customerReference,
        @NotNull AccountStatus status,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull @DecimalMin("0.0") Double balance) {
}
