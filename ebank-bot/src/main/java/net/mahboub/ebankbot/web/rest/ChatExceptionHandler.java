package net.mahboub.ebankbot.web.rest;

import com.openai.errors.OpenAIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ChatExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatExceptionHandler.class);

    @ExceptionHandler(OpenAIException.class)
    public ProblemDetail handleProviderFailure(Exception exception) {
        log.warn("Groq failure: {}", exception.getMessage().replaceAll("gsk_[A-Za-z0-9]+", "[REDACTED]"));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Le fournisseur IA est indisponible ou la configuration API est invalide.");
    }
}
