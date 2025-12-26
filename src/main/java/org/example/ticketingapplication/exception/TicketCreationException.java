package org.example.ticketingapplication.exception;

/**
 * TicketCreationException
 *
 * Thrown when ticket creation fails due to
 * an unexpected technical or system issue.
 */
public class TicketCreationException extends RuntimeException {

    public TicketCreationException(String message) {
        super(message);
    }

    public TicketCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
