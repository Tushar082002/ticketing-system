package org.example.ticketingapplication.event;

import org.apache.kafka.common.KafkaException;
import org.springframework.dao.DataAccessException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Error codes for bulk ticket processing operations.
 *
 * Categorized by prefix:
 * - V1xxx: Validation errors (non-retryable)
 * - P2xxx: Processing errors (mostly retryable)
 * - I3xxx: Infrastructure errors (mostly retryable)
 * - K4xxx: Kafka errors (mostly retryable)
 * - E9xxx: General/system errors (mostly retryable)
 */
public enum BulkProcessingErrorCode {

    // ==================== Validation Errors (1xxx) ====================
    EMPTY_FILE("V1001", "File is empty or contains no data", false),
    INVALID_FILE_FORMAT("V1002", "Invalid file format", false),
    MISSING_REQUIRED_COLUMNS("V1003", "Missing required columns in CSV", false),
    INVALID_ROW_DATA("V1004", "Invalid row data", false),
    MISSING_TICKET_NUMBER("V1005", "Ticket number is required", false),
    INVALID_CUSTOMER_ID("V1006", "Invalid customer ID", false),
    MISSING_TITLE("V1007", "Title is required", false),
    NULL_REQUEST("V1008", "Request payload is null", false),
    BATCH_SIZE_EXCEEDED("V1009", "Batch size exceeds maximum limit", false),

    // ==================== Processing Errors (2xxx) ====================
    DUPLICATE_TICKET("P2001", "Duplicate ticket number", false),
    TICKET_CREATION_FAILED("P2002", "Failed to create ticket", true),
    CHUNK_PROCESSING_FAILED("P2003", "Failed to process chunk", true),
    BATCH_PROCESSING_FAILED("P2004", "Failed to process batch", true),
    RECORD_PROCESSING_FAILED("P2005", "Failed to process record", true),
    INVALID_STATUS_TRANSITION("P2006", "Invalid status transition", false),
    INVALID_PRIORITY("P2007", "Invalid priority value", false),

    // ==================== Infrastructure Errors (3xxx) ====================
    DATABASE_ERROR("I3001", "Database error", true),
    REDIS_ERROR("I3002", "Redis cache error", true),
    IO_ERROR("I3003", "I/O error", true),
    TIMEOUT_ERROR("I3004", "Operation timeout", true),
    MEMORY_ERROR("I3005", "Out of memory", false),

    // ==================== Kafka Errors (4xxx) ====================
    KAFKA_PRODUCER_ERROR("K4001", "Kafka producer error", true),
    KAFKA_CONSUMER_ERROR("K4002", "Kafka consumer error", true),
    KAFKA_SERIALIZATION_ERROR("K4003", "Kafka serialization error", false),
    KAFKA_DESERIALIZATION_ERROR("K4004", "Kafka deserialization error", false),
    KAFKA_BROKER_UNAVAILABLE("K4005", "Kafka broker unavailable", true),
    KAFKA_TOPIC_NOT_FOUND("K4006", "Kafka topic not found", false),
    SENT_TO_DLT("K4007", "Message sent to Dead Letter Topic", false),
    KAFKA_COMMIT_FAILED("K4008", "Failed to commit offset", true),

    // ==================== General Errors (9xxx) ====================
    UNKNOWN_ERROR("E9001", "Unknown error occurred", true),
    INTERNAL_ERROR("E9002", "Internal system error", true),
    CONFIGURATION_ERROR("E9003", "Configuration error", false);

    private final String code;
    private final String description;
    private final boolean retryable;

    BulkProcessingErrorCode(String code, String description, boolean retryable) {
        this.code = code;
        this.description = description;
        this.retryable = retryable;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Determines the appropriate error code from an exception.
     */
    public static BulkProcessingErrorCode fromException(Throwable throwable) {
        if (throwable == null) {
            return UNKNOWN_ERROR;
        }

        if (throwable instanceof DataAccessException) {
            return DATABASE_ERROR;
        }
        if (throwable instanceof IOException) {
            return IO_ERROR;
        }
        if (throwable instanceof TimeoutException) {
            return TIMEOUT_ERROR;
        }
        if (throwable instanceof OutOfMemoryError) {
            return MEMORY_ERROR;
        }
        if (throwable instanceof KafkaException) {
            return KAFKA_PRODUCER_ERROR;
        }
        if (throwable instanceof IllegalArgumentException) {
            return INVALID_ROW_DATA;
        }
        if (throwable instanceof NullPointerException) {
            return NULL_REQUEST;
        }

        String message = throwable.getMessage();
        if (message != null) {
            message = message.toLowerCase();
            if (message.contains("duplicate")) {
                return DUPLICATE_TICKET;
            }
            if (message.contains("validation")) {
                return INVALID_ROW_DATA;
            }
            if (message.contains("timeout")) {
                return TIMEOUT_ERROR;
            }
            if (message.contains("redis")) {
                return REDIS_ERROR;
            }
            if (message.contains("kafka") || message.contains("broker")) {
                return KAFKA_BROKER_UNAVAILABLE;
            }
        }

        return UNKNOWN_ERROR;
    }

    /**
     * Determines if an exception is retryable.
     */
    public static boolean isRetryable(Throwable throwable) {
        return fromException(throwable).isRetryable();
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s [retryable=%s]", name(), code, description, retryable);
    }
}
