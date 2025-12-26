package org.example.ticketingapplication.exception;

import java.io.Serial;

/**
 * Enterprise-Grade Exception for Kafka Bulk Processing Errors
 *
 * Provides detailed context for errors occurring during bulk ticket processing:
 * - Batch ID for tracking
 * - Chunk number (if applicable)
 * - Affected record count
 * - Retryability information
 *
 * Used for error handling in:
 * - BulkUploadProcessingService (CSV parsing)
 * - BulkTicketProducer (Kafka publishing)
 * - BulkTicketConsumer (Batch processing)
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
public class KafkaBulkProcessingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Unique batch identifier for tracking.
     */
    private final String batchId;

    /**
     * Chunk number that failed (0-based, -1 if not applicable).
     */
    private final int chunkNumber;

    /**
     * Whether this error is retryable.
     */
    private final boolean retryable;

    /**
     * Number of affected records.
     */
    private final int affectedRecords;

    /**
     * Constructs a new KafkaBulkProcessingException with detailed context.
     *
     * @param message         Human-readable error message
     * @param batchId         Unique batch identifier
     * @param chunkNumber     Chunk number that failed (-1 if not applicable)
     * @param retryable       Whether this error is retryable
     * @param affectedRecords Number of records affected
     * @param cause           The underlying cause of the exception
     */
    public KafkaBulkProcessingException(String message, String batchId, int chunkNumber,
                                        boolean retryable, int affectedRecords, Throwable cause) {
        super(message, cause);
        this.batchId = batchId;
        this.chunkNumber = chunkNumber;
        this.retryable = retryable;
        this.affectedRecords = affectedRecords;
    }

    /**
     * Constructs a KafkaBulkProcessingException without cause.
     */
    public KafkaBulkProcessingException(String message, String batchId, int chunkNumber,
                                        boolean retryable, int affectedRecords) {
        this(message, batchId, chunkNumber, retryable, affectedRecords, null);
    }

    /**
     * Constructs a simplified KafkaBulkProcessingException.
     */
    public KafkaBulkProcessingException(String message, String batchId) {
        this(message, batchId, -1, false, 0, null);
    }

    /**
     * Constructs from a cause.
     */
    public KafkaBulkProcessingException(String message, String batchId, Throwable cause) {
        this(message, batchId, -1, isRetryable(cause), 0, cause);
    }

    /**
     * Determine if an exception is retryable.
     *
     * @param cause the exception to check
     * @return true if retryable
     */
    private static boolean isRetryable(Throwable cause) {
        // Retry on network/timeout errors
        if (cause == null) {
            return false;
        }

        String causeType = cause.getClass().getSimpleName();
        return causeType.contains("Timeout") ||
                causeType.contains("Connection") ||
                causeType.contains("IO") ||
                causeType.contains("Retry");
    }

    /**
     * Creates a formatted error message including all context.
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Batch: ").append(batchId);
        if (chunkNumber >= 0) {
            sb.append(", Chunk: ").append(chunkNumber);
        }
        sb.append(", Retryable: ").append(retryable);
        if (affectedRecords > 0) {
            sb.append(", AffectedRecords: ").append(affectedRecords);
        }
        sb.append("] ").append(getMessage());
        return sb.toString();
    }

    // Getters
    public String getBatchId() {
        return batchId;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public int getAffectedRecords() {
        return affectedRecords;
    }

    @Override
    public String toString() {
        return getDetailedMessage();
    }
}

