package org.example.ticketingapplication.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Ticket Response.
 *
 * Used for returning ticket data via REST API endpoints:
 * - GET /api/v1/tickets/{id}
 * - POST /api/v1/tickets (create response)
 * - PUT /api/v1/tickets/{id} (update response)
 *
 * This DTO exposes only necessary fields to the client and never exposes
 * internal entity details, implementation specifics, or sensitive information.
 *
 * Serialization:
 * - Format: JSON
 * - DateTime Format: ISO 8601 (yyyy-MM-dd HH:mm:ss)
 * - Caching: Stored in Redis with TTL of 30 minutes
 *
 * Usage:
 * - This is a read-only response DTO (not used for input validation)
 * - Instances are cached in Redis with cache key "ticket:{id}"
 * - Cache is invalidated on ticket update or delete operations
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class TicketResponse {

    /**
     * Unique identifier for the ticket.
     * Primary key in the database.
     * Used for identifying tickets in cache (key: "ticket:{id}")
     */
    private Long id;

    /**
     * Human-readable ticket number.
     * Unique across all tickets in the system.
     * Example: "TKT-20251226-001"
     */
    private String ticketNumber;

    /**
     * Current status of the ticket.
     * Valid values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, ON_HOLD
     * Changes trigger cache invalidation
     */
    private String status;

    /**
     * Priority level of the ticket.
     * Valid values: LOW, MEDIUM, HIGH, CRITICAL
     * Determines urgency and handling priority
     */
    private String priority;

    /**
     * Creation timestamp of the ticket.
     * Serialized in format: yyyy-MM-dd HH:mm:ss
     * Set automatically by the system (not modifiable)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Customer ID who created the ticket.
     * References a valid customer in the system.
     * Used for customer-specific ticket filtering
     */
    private Long customerId;

    /**
     * Optional: ID of the user assigned to this ticket.
     * Null if ticket is unassigned.
     * Can be updated via PUT /api/v1/tickets/{id}
     */
    private Integer assignedTo;
}
