package org.example.ticketingapplication.exception;

import lombok.Getter;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Enterprise-Grade Exception for Kafka Producer Errors
 *
 * Provides detailed context for errors occurring during Kafka message publishing:
 * - Target topic for correlation
 * - Message key for identification
 * - Correlation ID for distributed tracing
 * - Timestamp for audit trails
 * - Retryability assessment
 *
 * Used when:
 * - Message publishing to Kafka fails
 * - Network/connectivity issues occur
 * - Serialization errors happen
 * - Broker rejects the message
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Getter
public class KafkaProducerException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Target Kafka topic.
     */
    private final String topic;

    /**
     * Message key.
     */
    private final String messageKey;

    /**
     * Correlation ID for distributed tracing.
     */
    private final String correlationId;

    /**
     * Error code classification (e.g., "SERIALIZATION_ERROR", "NETWORK_ERROR", etc).
     */
    private final String errorCode;

    /**
     * Timestamp of the failure.
     */
    private final LocalDateTime failureTimestamp;

    /**
     * Whether retry is recommended.
     */
    private final boolean retryable;

    /**
     * Constructs a KafkaProducerException with full context.
     *
     * @param message       Error message
     * @param topic         Target Kafka topic
     * @param messageKey    Message key
     * @param correlationId Correlation ID for tracing
     * @param cause         Underlying cause of the exception
     */
    public KafkaProducerException(String message, String topic, String messageKey,
                                  String correlationId, Throwable cause) {
        super(message, cause);
        this.topic = topic;
        this.messageKey = messageKey;
        this.correlationId = correlationId;
        this.errorCode = classifyError(cause);
        this.failureTimestamp = LocalDateTime.now();
        this.retryable = isRetryableError(cause);
    }

    /**
     * Constructs a simplified KafkaProducerException.
     */
    public KafkaProducerException(String message, String topic, Throwable cause) {
        this(message, topic, null, null, cause);
    }

    /**
     * Constructs with message and topic only.
     */
    public KafkaProducerException(String message, String topic) {
        this(message, topic, null, null, null);
    }

    /**
     * Classify error type from exception.
     *
     * @param cause the exception to classify
     * @return error code string
     */
    private static String classifyError(Throwable cause) {
        if (cause == null) {
            return "KAFKA_PRODUCER_ERROR";
        }

        String causeType = cause.getClass().getSimpleName();

        if (causeType.contains("Timeout")) {
            return "TIMEOUT_ERROR";
        } else if (causeType.contains("Serialization")) {
            return "SERIALIZATION_ERROR";
        } else if (causeType.contains("Connection") || causeType.contains("Network")) {
            return "NETWORK_ERROR";
        } else if (causeType.contains("Authentication") || causeType.contains("Authorization")) {
            return "AUTH_ERROR";
        } else if (causeType.contains("BufferFull")) {
            return "BUFFER_FULL_ERROR";
        } else {
            return "KAFKA_PRODUCER_ERROR";
        }
    }

    /**
     * Determine if an error is retryable.
     *
     * @param cause the exception to check
     * @return true if retryable
     */
    private static boolean isRetryableError(Throwable cause) {
        if (cause == null) {
            return false;
        }

        String causeType = cause.getClass().getSimpleName();

        // Retry on transient errors
        return causeType.contains("Timeout") ||
                causeType.contains("Connection") ||
                causeType.contains("Network") ||
                causeType.contains("BufferFull") ||
                causeType.contains("Retry") ||
                causeType.contains("InterruptedException");
    }

    /**
     * Gets a detailed error description for logging and monitoring.
     *
     * @return formatted error description
     */
    public String getDetailedDescription() {
        return String.format(
                "[KafkaProducerError] Topic: %s, Key: %s, CorrelationId: %s, " +
                        "ErrorCode: %s, Retryable: %s, Timestamp: %s - %s",
                topic, messageKey, correlationId, errorCode, retryable, failureTimestamp, getMessage()
        );
    }

    @Override
    public String toString() {
        return getDetailedDescription();
    }
}

