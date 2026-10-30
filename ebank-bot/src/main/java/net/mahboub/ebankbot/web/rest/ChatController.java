package net.mahboub.ebankbot.web.rest;

import jakarta.validation.Valid;
import net.mahboub.ebankbot.service.BankAssistant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final BankAssistant assistant;

    public ChatController(BankAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(assistant.chat(request.query()));
    }
}
