package org.example.ticketingapplication.exception;

/**
 * Thrown when ticket update fails due to
 * a technical or system-level issue.
 */
public class TicketUpdateException extends RuntimeException {

    public TicketUpdateException(String message) {
        super(message);
    }

    public TicketUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
