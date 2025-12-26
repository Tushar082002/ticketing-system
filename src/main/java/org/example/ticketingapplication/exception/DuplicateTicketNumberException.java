package org.example.ticketingapplication.exception;

/**
 * DuplicateTicketNumberException
 *
 * Thrown when attempting to create a ticket with a ticket number that already exists.
 * This is a checked business-level exception that should result in a 409 (Conflict) HTTP response.
 *
 * Exception Type: Business Logic Exception (checked)
 * HTTP Status: 409 Conflict
 * Error Code: DUPLICATE_TICKET_NUMBER
 *
 * Exception Hierarchy:
 * - java.lang.Throwable
 *   └── java.lang.Exception
 *       └── java.lang.RuntimeException
 *           └── DuplicateTicketNumberException
 *
 * Usage:
 * try {
 *     ticketService.createTicket(request);
 * } catch (DuplicateTicketNumberException e) {
 *     // Handle duplicate ticket number error
 *     // Typically caught by GlobalExceptionHandler and converted to ErrorResponse
 * }
 *
 * Handling:
 * - Automatically caught by GlobalExceptionHandler
 * - Converted to ErrorResponse with:
 *   - statusCode: 409
 *   - errorCode: DUPLICATE_TICKET_NUMBER
 *   - message: "A ticket with number 'XXX' already exists"
 *   - timestamp: Current timestamp
 *
 * Serialization:
 * - Implements Serializable via parent class
 * - serialVersionUID ensures compatibility across versions
 *
 * @author Enterprise Backend Team
 * @version 1.0
 * @see GlobalExceptionHandler for centralized exception handling
 * @see ErrorResponse for API error response structure
 */
public class DuplicateTicketNumberException extends RuntimeException {

    /**
     * Serial version UID for serialization compatibility.
     * Update this when the exception structure changes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Business error code for this exception.
     * Used by API clients to identify specific error types.
     * Value: "DUPLICATE_TICKET_NUMBER"
     */
    private static final String ERROR_CODE = "DUPLICATE_TICKET_NUMBER";

    /**
     * Default error message.
     * Used when no ticket number is provided.
     */
    private static final String DEFAULT_MESSAGE = "Duplicate ticket number";

    /**
     * No-argument constructor.
     * Uses default error message.
     * Useful when ticket number is not available.
     */
    public DuplicateTicketNumberException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructor with ticket number only.
     * Creates a descriptive error message including the ticket number.
     *
     * @param ticketNumber the ticket number that is already in use
     */
    public DuplicateTicketNumberException(String ticketNumber) {
        super(String.format("A ticket with number '%s' already exists", ticketNumber));
    }

    /**
     * Constructor with ticket number and root cause.
     * Includes the underlying exception for debugging and logging.
     * Used when DuplicateTicketNumberException is caused by another exception
     * (e.g., DataIntegrityViolationException from database).
     *
     * @param ticketNumber the ticket number that is already in use
     * @param cause the underlying cause of this exception
     */
    public DuplicateTicketNumberException(String ticketNumber, Throwable cause) {
        super(String.format("A ticket with number '%s' already exists", ticketNumber), cause);
    }

    /**
     * Gets the business error code for this exception.
     * Used by ErrorResponse and API clients to identify error type.
     *
     * @return the error code: "DUPLICATE_TICKET_NUMBER"
     */
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
