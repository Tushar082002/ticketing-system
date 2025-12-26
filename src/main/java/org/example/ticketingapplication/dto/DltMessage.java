package org.example.ticketingapplication.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DltMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Original topic the message was sent to.
     */
    private String topic;

    /**
     * Message key.
     */
    private String messageKey;

    /**
     * Message payload as string.
     */
    private String payload;

    /**
     * When the message was added to DLT.
     */
    private LocalDateTime timestamp;

    /**
     * Error message that caused the failure.
     */
    private String errorMessage;

    /**
     * Exception class name.
     */
    private String exceptionType;

    /**
     * Whether this message has been manually processed/reprocessed.
     */
    private boolean processed;

    /**
     * When the message was reprocessed (if applicable).
     */
    private LocalDateTime reprocessedAt;

    /**
     * Notes about manual processing.
     */
    private String processingNotes;
}
