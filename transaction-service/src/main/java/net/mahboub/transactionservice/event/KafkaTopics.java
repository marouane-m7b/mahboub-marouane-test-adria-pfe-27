package net.mahboub.transactionservice.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopics {

    @Bean
    public NewTopic transactionCreatedTopic() {
        return TopicBuilder.name(TransactionEventPublisher.TOPIC).partitions(3).replicas(1).build();
    }
}
