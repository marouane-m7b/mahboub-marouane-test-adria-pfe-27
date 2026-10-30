package net.mahboub.ebankbot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class BankAssistant {

    private final ChatClient chatClient;

    public BankAssistant(ChatClient.Builder builder, ToolCallbackProvider tools) {
        this.chatClient = builder.defaultSystem("""
                Tu es un assistant bancaire pour un TP. Reponds en francais de facon concise.
                Utilise obligatoirement les outils MCP pour consulter les comptes, soldes et transactions.
                N'invente jamais de donnees ni d'identifiants. Si les donnees sont indisponibles, dis-le.
                Pour une operation d'ecriture, exige une demande explicite de l'utilisateur et les parametres
                necessaires. Ne cree pas de transaction ou de virement si l'utilisateur demande seulement
                des informations. N'annonce un succes que si l'outil confirme le resultat.
                En cas d'erreur ou de resultat incertain d'une ecriture, ne repete pas l'appel.
                Chaque requete REST est independante et ne conserve pas d'historique conversationnel.
                """).defaultToolCallbacks(tools).build();
    }

    public String chat(String query) {
        return chatClient.prompt().user(query).call().content();
    }
}
