package net.mahboub.transactionservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

@Component
public class TransactionEventPublisher {

    public static final String TOPIC = "bank.transactions.created";
    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public TransactionEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TransactionCreatedEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.accountId().toString(), mapper.writeValueAsString(event))
                    .whenComplete((result, failure) -> {
                        if (failure == null) {
                            log.info("TransactionCreated published: transactionId={} key={}", event.transactionId(), event.accountId());
                        } else {
                            log.error("Kafka publication failed after commit: eventId={} transactionId={}",
                                    event.eventId(), event.transactionId());
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("Kafka publication failed after commit: eventId={} transactionId={}",
                    event.eventId(), event.transactionId());
        }
    }
}
