package org.example.ticketingapplication.exception;

/**
 * Thrown when ticket retrieval fails due to
 * a technical/system issue.
 */
public class TicketRetrievalException extends RuntimeException {

    public TicketRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
