package org.example.ticketingapplication.exception;

/**
 * Thrown when ticket deletion fails due to
 * a technical or system-level issue.
 */
public class TicketDeletionException extends RuntimeException {

    public TicketDeletionException(String message) {
        super(message);
    }

    public TicketDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
