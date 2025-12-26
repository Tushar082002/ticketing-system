package org.example.ticketingapplication.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketingapplication.dto.CreateTicketRequest;
import org.example.ticketingapplication.entity.Ticket;
import org.example.ticketingapplication.repository.TicketRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise-Grade Kafka Consumer for Bulk Ticket Processing
 *
 * Consumes batches of ticket events from Kafka topic and persists them to database
 * with transactional guarantees, retry mechanism, and comprehensive error handling.
 *
 * Architecture:
 * - Kafka Topic: ticket.bulk.requests
 * - Consumer Group: ticket-bulk-consumers
 * - Batch Size: 100 records per poll
 * - Concurrency: 3 parallel consumers
 * - Retry Strategy: 3 attempts with exponential backoff
 * - Error Handling: DLQ routing on persistent failure
 *
 * Processing Flow:
 * 1. Receive batch of TicketEvent objects
 * 2. Convert events to Ticket entities
 * 3. Batch insert to database via saveAll()
 * 4. On success: Acknowledge offset
 * 5. On error: Automatic retry via Spring Kafka error handler
 * 6. On persistent failure: Route to Dead Letter Queue
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BulkTicketConsumer {

    private final TicketRepository ticketRepository;

    /**
     * Consume and process batch of ticket events.
     *
     * Processing:
     * 1. Validate batch is not null/empty
     * 2. Convert TicketEvent → Ticket entities
     * 3. Persist batch atomically
     * 4. Acknowledge offset on success
     * 5. Rethrow exception on error (triggers retry)
     *
     * @param events batch of CreateTicketRequest (size: 1-100)
     * @param ack manual acknowledgment handler
     * @throws RuntimeException if processing fails (triggers retry)
     */
    @KafkaListener(
            topics = "ticket.bulk.requests",
            groupId = "ticket-bulk-consumers",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(List<CreateTicketRequest> events, Acknowledgment ack) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate batch
            if (events == null || events.isEmpty()) {
                log.warn("Received empty batch from ticket.bulk.requests");
                ack.acknowledge();
                return;
            }

            log.debug("Processing batch with {} records", events.size());

            // Convert events to entities
            List<Ticket> tickets = convertToTickets(events);

            // Persist batch atomically
            ticketRepository.saveAll(tickets);

            // Acknowledge offset only after successful commit
            ack.acknowledge();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Batch processed successfully: {} records in {}ms", events.size(), duration);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Batch processing failed after {}ms: {}", duration, ex.getMessage(), ex);

            // Rethrow to trigger error handler retry logic
            throw new RuntimeException("Batch processing failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Convert CreateTicketRequest DTOs to Ticket entities.
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
     * @param requests list of CreateTicketRequest objects
     * @return list of Ticket entities ready for persistence
     * @throws RuntimeException if conversion fails for any request
     */
    private List<Ticket> convertToTickets(List<CreateTicketRequest> requests) {
        List<Ticket> tickets = new ArrayList<>();

        for (CreateTicketRequest request : requests) {
            if (request == null) {
                log.warn("Null request encountered, skipping");
                continue;
            }

            try {
                Ticket ticket = Ticket.builder()
                        .ticketNumber(request.getTicketNumber())
                        .status(request.getStatus())
                        .priority(request.getPriority())
                        .customerId(request.getCustomerId())
                        .assignedTo(request.getAssignedTo())
                        .createdAt(LocalDateTime.now())
                        .build();

                tickets.add(ticket);
                log.debug("Converted CreateTicketRequest {} to Ticket entity", request.getTicketNumber());

            } catch (Exception ex) {
                log.error("Error converting request {}: {}", request.getTicketNumber(), ex.getMessage());
                throw new RuntimeException(
                        "Failed to convert request: " + ex.getMessage(), ex);
            }
        }

        return tickets;
    }

    /**
     * Handle messages from Dead Letter Queue.
     *
     * Purpose:
     * - Log permanently failed batches
     * - Enable manual investigation
     * - Preserve original data for recovery
     */
    @KafkaListener(
            topics = "ticket.bulk.requests.DLT",
            groupId = "ticket-bulk-consumers-dlt",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDlq(List<CreateTicketRequest> events) {
        log.error("CRITICAL: {} records reached DLQ after max retries. " +
                        "Manual intervention required for recovery.",
                events != null ? events.size() : 0);

        // TODO: Integrate with monitoring/alerting system
        // TODO: Store in separate DLQ storage for manual reprocessing
    }
}

