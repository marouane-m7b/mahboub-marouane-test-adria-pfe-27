package net.mahboub.ebankbot.telegram;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.mahboub.ebankbot.service.BankAssistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true")
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final BankAssistant assistant;
    private BotSession session;

    @Value("${telegram.token}")
    private String token;

    public TelegramBot(BankAssistant assistant) {
        this.assistant = assistant;
    }

    @PostConstruct
    public void register() throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        session = api.registerBot(this);
        log.info("Telegram bot registered: {}", getBotUsername());
    }

    @PreDestroy
    public void stop() {
        if (session != null) {
            session.stop();
        }
    }

    @Override
    public String getBotUsername() {
        return "springproject45bot";
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        String text = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();
        try {
            if (text.startsWith("/start")) {
                send(chatId, "Bonjour ! Je suis l'assistant bancaire du TP. Demandez-moi la liste des comptes ou des transactions.");
                return;
            }
            SendChatAction typing = new SendChatAction();
            typing.setChatId(chatId);
            typing.setAction(ActionType.TYPING);
            execute(typing);
            String answer;
            try {
                answer = assistant.chat(text);
            } catch (RuntimeException exception) {
                log.warn("Assistant request failed: {}", exception.getClass().getSimpleName());
                answer = "Le service IA est indisponible. Si vous avez demande une operation, verifiez le solde avant de reessayer.";
            }
            send(chatId, answer == null || answer.isBlank() ? "Je ne sais pas." : answer);
        } catch (TelegramApiException exception) {
            log.warn("Unable to send Telegram response: {}", exception.getClass().getSimpleName());
        }
    }

    private void send(String chatId, String text) throws TelegramApiException {
        for (int start = 0; start < text.length(); start += 4000) {
            execute(new SendMessage(chatId, text.substring(start, Math.min(start + 4000, text.length()))));
        }
    }
}
