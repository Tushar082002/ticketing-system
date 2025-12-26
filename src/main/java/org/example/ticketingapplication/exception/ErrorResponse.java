package org.example.ticketingapplication.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ErrorResponse DTO
 *
 * Standard error response format for the API.
 * Used across all endpoints to provide consistent error information.
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Error code - Machine-readable identifier
     * e.g., "TICKET_NOT_FOUND", "DUPLICATE_TICKET_NUMBER"
     */
    private String errorCode;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Timestamp when the error occurred
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Optional: Path that caused the error
     */
    private String path;
}

