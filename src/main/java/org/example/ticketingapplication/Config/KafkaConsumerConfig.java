package org.example.ticketingapplication.Config;

import org.example.ticketingapplication.event.TicketEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;

@Configuration
public class KafkaConsumerConfig {
        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, TicketEvent>
        kafkaListenerContainerFactory(
                ConsumerFactory<String, TicketEvent> consumerFactory,
                DefaultErrorHandler errorHandler) {

            ConcurrentKafkaListenerContainerFactory<String, TicketEvent> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();

            factory.setConsumerFactory(consumerFactory);
            factory.setBatchListener(true);
            factory.setConcurrency(3);
            factory.setCommonErrorHandler(errorHandler);

            return factory;
        }
}


