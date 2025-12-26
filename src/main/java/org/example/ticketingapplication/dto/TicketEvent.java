package org.example.ticketingapplication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka DTO for Bulk Ticket Processing
 *
 * Represents a single ticket event published to Kafka topic "ticket.bulk.requests"
 * Contains all necessary fields to create a Ticket entity in the database.
 *
 * Usage:
 * - Producer: Creates TicketEvent from CSV records for Kafka publishing
 * - Consumer: Receives batch of TicketEvent and converts to Ticket entities
 *
 * Serialization: JSON (handled by Spring Kafka with Jackson)
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEvent {

    // Tracking
    private String jobId;

    // Core ticket fields
    private String ticketNumber;
    private String status;
    private String priority;

    // Optional fields
    private Long customerId;
    private Integer assignedTo;
}

