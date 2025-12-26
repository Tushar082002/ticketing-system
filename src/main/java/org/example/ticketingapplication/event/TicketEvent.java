package org.example.ticketingapplication.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base Kafka Event class for all ticket-related events.
 *
 * Common fields for event tracking, correlation, and auditing.
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEvent {

    // Event metadata
    private String eventId;
    private String eventType;
    private String source;
    private LocalDateTime timestamp;
    private String correlationId;
}


