package org.example.ticketingapplication.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.ticketingapplication.dto.CreateTicketRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Event for Ticket Creation
 *
 * Represents a ticket creation event published to Kafka topic "ticket-events".
 * Used for event-driven processing and audit trails.
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketCreatedEvent extends TicketEvent {

    private static final String EVENT_TYPE = "TICKET_CREATED";

    private String ticketNumber;
    private String status;
    private String priority;
    private Long customerId;
    private Integer assignedTo;

    /**
     * Create a TicketCreatedEvent from CreateTicketRequest.
     *
     * @param request the ticket creation request
     * @param correlationId unique correlation ID for tracing
     * @return TicketCreatedEvent
     */
    public static TicketCreatedEvent fromRequest(CreateTicketRequest request, String correlationId) {
        TicketCreatedEvent event = new TicketCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(EVENT_TYPE);
        event.setTimestamp(LocalDateTime.now());
        event.setCorrelationId(correlationId);
        event.setSource("ticketing-service");

        // Map request fields to event
        event.setTicketNumber(request.getTicketNumber());
        event.setStatus(request.getStatus());
        event.setPriority(request.getPriority());
        event.setCustomerId(request.getCustomerId());
        event.setAssignedTo(request.getAssignedTo());

        return event;
    }

    /**
     * Convert event back to CreateTicketRequest.
     *
     * @return CreateTicketRequest
     */
    public CreateTicketRequest toCreateRequest() {
        return CreateTicketRequest.builder()
                .ticketNumber(this.ticketNumber)
                .status(this.status)
                .priority(this.priority)
                .customerId(this.customerId)
                .assignedTo(this.assignedTo)
                .build();
    }
}


