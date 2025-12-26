package org.example.ticketingapplication.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * GlobalExceptionHandler - Centralized Exception Handling for the Entire Application
 *
 * Purpose:
 * - Ensures consistent and meaningful error responses across all REST APIs
 * - Provides unified error response structure for all exceptions
 * - Logs all exceptions for monitoring and debugging
 * - Maps business exceptions to appropriate HTTP status codes
 *
 * Design Pattern:
 * - @RestControllerAdvice: Applies exception handling to all @RestController beans
 * - Method-level @ExceptionHandler: Specific handlers for each exception type
 * - Handler Precedence: More specific exceptions handled first, then generic ones
 *
 * Exception Handling Hierarchy:
 * ┌─ Business Logic Exceptions (4xx status codes)
 * │  ├─ TicketNotFoundException → 404 Not Found
 * │  ├─ DuplicateTicketNumberException → 409 Conflict
 * │  ├─ TicketCreationException → 422 Unprocessable Entity
 * │  ├─ TicketUpdateException → 422 Unprocessable Entity
 * │  ├─ TicketDeletionException → 422 Unprocessable Entity
 * │  └─ TicketRetrievalException → 500 Internal Server Error (retry-able)
 * │
 * ├─ Validation Exceptions (400 Bad Request)
 * │  ├─ MethodArgumentNotValidException → 400 with field-level errors
 * │  ├─ MethodArgumentTypeMismatchException → 400 with type info
 * │  └─ HttpMessageNotReadableException → 400 invalid body
 * │
 * └─ Unexpected Exceptions (500 Internal Server Error)
 *    └─ Exception (catch-all) → 500
 *
 * Response Format:
 * - ErrorResponse: Standard error response with errorCode, message, status, timestamp, path
 * - ValidationErrorResponse: Extended error response with field-level validation errors
 *
 * Logging Strategy:
 * - WARN level: Expected business exceptions (validation, not found, duplicate)
 * - ERROR level: Unexpected exceptions and server errors
 * - All exceptions include stack traces for debugging
 *
 * Error Codes Reference:
 * - TICKET_NOT_FOUND: Ticket with given ID does not exist
 * - DUPLICATE_TICKET_NUMBER: Ticket number already exists in system
 * - TICKET_CREATION_FAILED: Failed to create ticket due to data issues
 * - TICKET_UPDATE_FAILED: Failed to update ticket
 * - TICKET_DELETION_FAILED: Failed to delete ticket
 * - TICKET_RETRIEVAL_FAILED: Failed to retrieve ticket from database
 * - VALIDATION_ERROR: Request validation failed (field-level errors provided)
 * - INVALID_PARAMETER_TYPE: Path/query parameter has invalid type
 * - REQUEST_BODY_MISSING: Request body is missing or malformed
 * - INTERNAL_SERVER_ERROR: Unexpected server error
 *
 * @author Enterprise Backend Team
 * @version 1.0
 * @see ErrorResponse
 * @see ValidationErrorResponse
 * @see TicketNotFoundException
 * @see DuplicateTicketNumberException
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Error Code Constants
    private static final String TICKET_NOT_FOUND = "TICKET_NOT_FOUND";
    private static final String DUPLICATE_TICKET_NUMBER = "DUPLICATE_TICKET_NUMBER";
    private static final String TICKET_CREATION_FAILED = "TICKET_CREATION_FAILED";
    private static final String TICKET_UPDATE_FAILED = "TICKET_UPDATE_FAILED";
    private static final String TICKET_DELETION_FAILED = "TICKET_DELETION_FAILED";
    private static final String TICKET_RETRIEVAL_FAILED = "TICKET_RETRIEVAL_FAILED";
    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String INVALID_PARAMETER_TYPE = "INVALID_PARAMETER_TYPE";
    private static final String REQUEST_BODY_MISSING = "REQUEST_BODY_MISSING";
    private static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    /**
     * Handle TicketNotFoundException
     *
     * HTTP Status: 404 Not Found
     * Error Code: TICKET_NOT_FOUND
     * Severity: WARN (expected exception)
     *
     * Triggered when:
     * - GET /api/v1/tickets/{id} with non-existent ID
     * - UPDATE /api/v1/tickets/{id} with non-existent ID
     * - DELETE /api/v1/tickets/{id} with non-existent ID
     *
     * @param ex the TicketNotFoundException
     * @param request the current web request
     * @return ErrorResponse with 404 status
     */
    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTicketNotFoundException(
            TicketNotFoundException ex,
            WebRequest request) {

        log.warn("Ticket not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(TICKET_NOT_FOUND)
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * Handle DuplicateTicketNumberException
     *
     * HTTP Status: 409 Conflict
     * Error Code: DUPLICATE_TICKET_NUMBER
     * Severity: WARN (expected exception)
     *
     * Triggered when:
     * - POST /api/v1/tickets with ticketNumber that already exists
     *
     * @param ex the DuplicateTicketNumberException
     * @param request the current web request
     * @return ErrorResponse with 409 status
     */
    @ExceptionHandler(DuplicateTicketNumberException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTicketNumberException(
            DuplicateTicketNumberException ex,
            WebRequest request) {

        log.warn("Duplicate ticket number attempted: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(DUPLICATE_TICKET_NUMBER)
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    /**
     * Handle TicketCreationException
     *
     * HTTP Status: 422 Unprocessable Entity
     * Error Code: TICKET_CREATION_FAILED
     * Severity: ERROR (indicates data integrity or service issue)
     *
     * Triggered when:
     * - Database constraint violation during ticket creation
     * - Service layer validation fails
     * - Data processing error during ticket creation
     *
     * @param ex the TicketCreationException
     * @param request the current web request
     * @return ErrorResponse with 422 status
     */
    @ExceptionHandler(TicketCreationException.class)
    public ResponseEntity<ErrorResponse> handleTicketCreationException(
            TicketCreationException ex,
            WebRequest request) {

        log.error("Ticket creation failed: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(TICKET_CREATION_FAILED)
                .message(ex.getMessage())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorResponse);
    }

    /**
     * Handle TicketUpdateException
     *
     * HTTP Status: 422 Unprocessable Entity
     * Error Code: TICKET_UPDATE_FAILED
     * Severity: ERROR (indicates service issue)
     *
     * Triggered when:
     * - Database update fails
     * - Concurrent modification detected
     * - Service layer update validation fails
     *
     * @param ex the TicketUpdateException
     * @param request the current web request
     * @return ErrorResponse with 422 status
     */
    @ExceptionHandler(TicketUpdateException.class)
    public ResponseEntity<ErrorResponse> handleTicketUpdateException(
            TicketUpdateException ex,
            WebRequest request) {

        log.error("Ticket update failed: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(TICKET_UPDATE_FAILED)
                .message(ex.getMessage())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorResponse);
    }

    /**
     * Handle TicketDeletionException
     *
     * HTTP Status: 422 Unprocessable Entity
     * Error Code: TICKET_DELETION_FAILED
     * Severity: ERROR (indicates service issue)
     *
     * Triggered when:
     * - Database deletion fails
     * - Foreign key constraint prevents deletion
     * - Service layer deletion validation fails
     *
     * @param ex the TicketDeletionException
     * @param request the current web request
     * @return ErrorResponse with 422 status
     */
    @ExceptionHandler(TicketDeletionException.class)
    public ResponseEntity<ErrorResponse> handleTicketDeletionException(
            TicketDeletionException ex,
            WebRequest request) {

        log.error("Ticket deletion failed: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(TICKET_DELETION_FAILED)
                .message(ex.getMessage())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorResponse);
    }

    /**
     * Handle TicketRetrievalException
     *
     * HTTP Status: 500 Internal Server Error
     * Error Code: TICKET_RETRIEVAL_FAILED
     * Severity: ERROR (indicates service issue, may be retry-able)
     *
     * Triggered when:
     * - Database connection fails during retrieval
     * - Timeout during query execution
     * - Data consistency issues
     *
     * Note: Returns 500 to indicate server-side issue (not client error).
     * Client may retry the request.
     *
     * @param ex the TicketRetrievalException
     * @param request the current web request
     * @return ErrorResponse with 500 status
     */
    @ExceptionHandler(TicketRetrievalException.class)
    public ResponseEntity<ErrorResponse> handleTicketRetrievalException(
            TicketRetrievalException ex,
            WebRequest request) {

        log.error("Ticket retrieval failed: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(TICKET_RETRIEVAL_FAILED)
                .message("Failed to retrieve ticket. Please try again later.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * Handle validation errors from @Valid annotation
     *
     * HTTP Status: 400 Bad Request
     * Error Code: VALIDATION_ERROR
     * Severity: WARN (expected client error)
     *
     * Triggered when:
     * - @Valid validation fails on request body
     * - Field constraints violated (e.g., @NotBlank, @Size, @NotNull)
     *
     * Response includes:
     * - Field-level error details
     * - Rejected values
     * - Validation error messages
     *
     * @param ex the MethodArgumentNotValidException
     * @param request the current web request
     * @return ValidationErrorResponse with 400 status and field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        log.warn("Validation failed: {} violations", ex.getBindingResult().getErrorCount());

        List<ValidationErrorResponse.FieldError> fieldErrors = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.add(
                        ValidationErrorResponse.FieldError.builder()
                                .field(error.getField())
                                .message(error.getDefaultMessage())
                                .rejectedValue(error.getRejectedValue())
                                .build()
                )
        );

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .errorCode(VALIDATION_ERROR)
                .message("Request validation failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Handle invalid path or request parameter types
     *
     * HTTP Status: 400 Bad Request
     * Error Code: INVALID_PARAMETER_TYPE
     * Severity: WARN (expected client error)
     *
     * Triggered when:
     * - Path parameter {id} is not a valid number (e.g., /tickets/abc)
     * - Query parameter has invalid type
     *
     * Response includes:
     * - Provided value
     * - Parameter name
     * - Expected type
     *
     * @param ex the MethodArgumentTypeMismatchException
     * @param request the current web request
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {

        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType().getSimpleName()
        );

        log.warn("Parameter type mismatch: {}", message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(INVALID_PARAMETER_TYPE)
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Handle missing or malformed request body
     *
     * HTTP Status: 400 Bad Request
     * Error Code: REQUEST_BODY_MISSING
     * Severity: WARN (expected client error)
     *
     * Triggered when:
     * - Request body is missing when expected
     * - JSON parsing fails (malformed JSON)
     * - Content-Type mismatch
     *
     * @param ex the HttpMessageNotReadableException
     * @param request the current web request
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        log.warn("Request body missing or malformed: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(REQUEST_BODY_MISSING)
                .message("Request body is missing or invalid JSON")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Fallback handler for unexpected exceptions
     *
     * HTTP Status: 500 Internal Server Error
     * Error Code: INTERNAL_SERVER_ERROR
     * Severity: ERROR (unexpected exception)
     *
     * This is the last resort handler that catches all exceptions
     * not handled by specific @ExceptionHandler methods above.
     *
     * Triggered when:
     * - Any unexpected exception is thrown
     * - No specific handler is found
     *
     * Note:
     * - Generic message is returned to client (no internal details exposed)
     * - Full stack trace is logged for debugging
     * - Error is logged at ERROR level
     *
     * @param ex the unexpected exception
     * @param request the current web request
     * @return ErrorResponse with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {

        log.error("Unhandled exception occurred", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(INTERNAL_SERVER_ERROR)
                .message("An unexpected error occurred. Please try again later.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

}
