package org.example.ticketingapplication.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.ticketingapplication.entity.Ticket;
import org.example.ticketingapplication.event.TicketCreatedEvent;
import org.example.ticketingapplication.repository.TicketRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Enterprise-Grade Kafka Consumer for Individual Ticket Events
 *
 * Consumes single ticket creation events from Kafka topic and persists them to database
 * with transactional guarantees and comprehensive error handling.
 *
 * Architecture:
 * - Kafka Topic: ticket-events
 * - Consumer Group: ticketing-group
 * - Processing: Individual ticket events (not bulk)
 * - Retry Strategy: 3 attempts with exponential backoff
 * - Error Handling: DLQ routing on persistent failure
 *
 * Processing Flow:
 * 1. Receive TicketCreatedEvent
 * 2. Convert event to Ticket entity
 * 3. Persist to database
 * 4. Acknowledge offset on success
 * 5. Rethrow exception on error (triggers retry)
 * 6. Route to DLQ after max retries
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketKafkaConsumer {

    private final TicketRepository ticketRepository;

    /**
     * Consume and process individual ticket creation events.
     *
     * Processing:
     * 1. Validate event is not null
     * 2. Convert TicketCreatedEvent to Ticket entity
     * 3. Persist to database
     * 4. Acknowledge offset on success
     * 5. Rethrow exception on error (triggers retry)
     *
     * @param event the ticket creation event
     * @param key message key (ticket number)
     * @param partition partition number
     * @param offset message offset
     * @param ack manual acknowledgment handler
     * @throws RuntimeException if processing fails (triggers retry)
     */
    @KafkaListener(
            topics = "ticket-events",
            groupId = "ticketing-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeTicketCreatedEvent(
            @Payload TicketCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        long startTime = System.currentTimeMillis();

        try {
            // Validate event
            if (event == null) {
                log.warn("Received null ticket event from partition {}, offset {}", partition, offset);
                ack.acknowledge();
                return;
            }

            log.info("Processing ticket event: {} from partition {}, offset {}, key: {}",
                    event.getEventId(), partition, offset, key);

            // Convert event to entity
            Ticket ticket = convertToTicket(event);

            // Persist to database
            ticketRepository.save(ticket);

            // Acknowledge offset only after successful commit
            ack.acknowledge();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Ticket event processed successfully: ticketNumber={}, id={}, duration={}ms",
                    ticket.getTicketNumber(), ticket.getId(), duration);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ticket event processing failed after {}ms: {}", duration, ex.getMessage(), ex);

            // Rethrow to trigger error handler retry logic
            throw new RuntimeException("Ticket event processing failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Convert TicketCreatedEvent to Ticket entity.
     *
     * Field Mapping:
     * - ticketNumber → ticketNumber
     * - status → status
     * - priority → priority
     * - customerId → customerId
     * - assignedTo → assignedTo
     * - createdAt → set to LocalDateTime.now()
     * - id → null (auto-generated)
     *
     * @param event the ticket created event
     * @return Ticket entity ready for persistence
     * @throws RuntimeException if conversion fails
     */
    private Ticket convertToTicket(TicketCreatedEvent event) {
        try {
            Ticket ticket = Ticket.builder()
                    .ticketNumber(event.getTicketNumber())
                    .status(event.getStatus())
                    .priority(event.getPriority())
                    .customerId(event.getCustomerId())
                    .assignedTo(event.getAssignedTo())
                    .createdAt(LocalDateTime.now())
                    .build();

            log.debug("Converted TicketCreatedEvent {} to Ticket entity", event.getTicketNumber());
            return ticket;

        } catch (Exception ex) {
            log.error("Error converting TicketCreatedEvent: {}", ex.getMessage());
            throw new RuntimeException("Failed to convert event: " + ex.getMessage(), ex);
        }
    }

    /**
     * Handle messages from Dead Letter Queue for ticket events.
     *
     * Purpose:
     * - Log permanently failed events
     * - Enable manual investigation
     * - Preserve original data for recovery
     */
    @KafkaListener(
            topics = "ticket-events.DLT",
            groupId = "ticketing-group-dlt",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTicketEventsDlq(
            ConsumerRecord<String, Object> record,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.error("CRITICAL: Ticket event reached DLQ after max retries - " +
                        "topic: {}, partition: {}, offset: {}, key: {}, value type: {}",
                record.topic(), partition, offset, record.key(),
                record.value() != null ? record.value().getClass().getSimpleName() : "null");

        // TODO: Integrate with monitoring/alerting system
        // TODO: Store in separate DLQ storage for manual reprocessing
    }
}

