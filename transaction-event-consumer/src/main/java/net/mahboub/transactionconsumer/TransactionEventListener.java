package net.mahboub.transactionconsumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    @KafkaListener(topics = "bank.transactions.created", groupId = "bank-transaction-logger")
    public void receive(ConsumerRecord<String, String> record) {
        log.info("Transaction event received: key={} partition={} offset={} payload={}",
                record.key(), record.partition(), record.offset(), record.value());
    }
}
