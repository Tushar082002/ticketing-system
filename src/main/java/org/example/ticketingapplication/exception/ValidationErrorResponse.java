package org.example.ticketingapplication.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ValidationErrorResponse DTO
 *
 * Extended error response for validation errors.
 * Includes details about which fields failed validation.
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {

    /**
     * Error code
     */
    private String errorCode;

    /**
     * General error message
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
     * Path that caused the error
     */
    private String path;

    /**
     * List of field-specific validation errors
     */
    private List<FieldError> fieldErrors;

    /**
     * Inner class representing a single field validation error
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}

