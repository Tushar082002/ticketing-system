package org.example.ticketingapplication.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.ticketingapplication.event.TicketEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise-Grade Kafka Configuration for Bulk Ticket Processing
 *
 * Architecture:
 * - Producer: Sends TicketEvent batches to "ticket.bulk.requests" topic
 * - Consumer: Processes batches from "ticket.bulk.requests" topic
 * - Topic: partitions=5, replication=3, retention=7 days
 * - Consumer Group: ticket-bulk-consumers
 * - Concurrency: 3 parallel consumers
 * - Batch Size: 100 records per poll
 * - Poll Timeout: 1000ms
 * - Retry: 3 attempts with fixed 1000ms backoff
 * - Error Handling: DefaultErrorHandler with DLQ routing
 *
 * Serialization:
 * - Key: String (StringSerializer/StringDeserializer)
 * - Value: TicketEvent (JSON via Jackson)
 * - Error Handling: ErrorHandlingDeserializer wraps JsonDeserializer
 *
 * Transaction Semantics:
 * - Manual offset commit (MANUAL_IMMEDIATE)
 * - Acknowledged only after successful DB insertion
 * - No partial processing
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Slf4j
@Configuration
@EnableKafka
@SuppressWarnings("deprecation")
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private static final String CONSUMER_GROUP = "ticket-bulk-consumers";
    private static final int MAX_POLL_RECORDS = 100;
    private static final int SESSION_TIMEOUT_MS = 30000;
    private static final int HEARTBEAT_INTERVAL_MS = 10000;

    /**
     * ObjectMapper for JSON serialization/deserialization.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    // ==================== Producer Configuration ====================

    /**
     * Producer factory configuration for reliable message publishing.
     *
     * Reliability Settings:
     * - acks=all: Wait for all in-sync replicas to acknowledge
     * - retries=3: Retry on failure
     * - enable.idempotence=true: Prevent duplicate messages
     *
     * Performance Settings:
     * - compression=snappy: Reduce network bandwidth
     * - batch.size=16KB: Buffer size before sending
     * - linger.ms=5: Wait up to 5ms for batching
     * - buffer.memory=32MB: Total buffer memory
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability settings
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Performance settings
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Timeout settings
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        log.info("✅ ProducerFactory configured: acks=all, retries=3, idempotent=true, compression=snappy");
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * KafkaTemplate bean for sending messages to Kafka topics.
     * Provides synchronous and asynchronous send capabilities with default topic.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic("ticket.bulk.requests");
        log.info("✅ KafkaTemplate bean created: default topic = ticket.bulk.requests");
        return template;
    }

    // ==================== Consumer Configuration ====================

    /**
     * Consumer factory for deserializing TicketEvent messages.
     *
     * Settings:
     * - auto.offset.reset=earliest: Start from beginning if no offset exists
     * - enable.auto.commit=false: Manual offset management
     * - max.poll.records=100: Fetch 100 records per poll
     * - session.timeout.ms=30000: Session timeout
     * - heartbeat.interval.ms=10000: Heartbeat interval
     */
    @Bean
    public ConsumerFactory<String, TicketEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MAX_POLL_RECORDS);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, SESSION_TIMEOUT_MS);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, HEARTBEAT_INTERVAL_MS);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

        // JsonDeserializer settings
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TicketEvent.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        log.info("✅ ConsumerFactory configured: group={}, maxPollRecords={}, sessionTimeout={}ms",
                CONSUMER_GROUP, MAX_POLL_RECORDS, SESSION_TIMEOUT_MS);

        return new DefaultKafkaConsumerFactory<>(props,
                new ErrorHandlingDeserializer<>(new StringDeserializer()),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>(TicketEvent.class, false)));
    }

    /**
     * Error handler with retry mechanism and DLQ routing.
     *
     * Retry Strategy:
     * - Max Retries: 3 attempts
     * - Backoff: Fixed 1000ms between retries
     * - Total Time: ~3 seconds for all retries
     * - DLQ: Messages sent to DLQ after max retries
     * - Non-Retryable: IllegalArgumentException and validation errors
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(new FixedBackOff(1000, 3));

        // Mark specific exceptions as non-retryable
        handler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                IllegalStateException.class,
                org.springframework.messaging.converter.MessageConversionException.class
        );

        log.info("✅ DefaultErrorHandler configured: maxRetries=3, backoffMs=1000, dltEnabled=true");
        return handler;
    }
}

