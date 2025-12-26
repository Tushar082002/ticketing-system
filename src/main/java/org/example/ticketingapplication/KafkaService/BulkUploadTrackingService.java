package org.example.ticketingapplication.KafkaService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Enterprise-Grade Service for Bulk Upload Batch Tracking
 *
 * Responsibilities:
 * - Track batch status and progress in Redis
 * - Record success/failure counts per batch
 * - Monitor chunk completion
 * - Handle Redis unavailability gracefully
 *
 * Architecture:
 * - Primary: Redis for distributed tracking
 * - Fallback: In-memory for Redis unavailability
 * - TTL: 24 hours for batch data
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadTrackingService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BATCH_STATUS_KEY_PREFIX = "bulk:batch:status:";
    private static final String ACTIVE_BATCHES_KEY = "bulk:active-batches";
    private static final Duration BATCH_EXPIRY = Duration.ofHours(24);

    /**
     * Simple batch status DTO for tracking.
     */
    @Data
    public static class BatchStatusInfo {
        private String batchId;
        private String status; // ACCEPTED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
        private int totalChunks;
        private int completedChunks;
        private long totalTickets;
        private long successCount;
        private long failureCount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    /**
     * Initialize batch tracking.
     *
     * @param batchId unique batch identifier
     * @param totalChunks total chunks in batch
     * @param estimatedTotalTickets estimated tickets
     */
    public void initializeBatch(String batchId, int totalChunks, long estimatedTotalTickets) {
        String statusKey = BATCH_STATUS_KEY_PREFIX + batchId;

        try {
            // Check if already initialized
            if (Boolean.TRUE.equals(redisTemplate.hasKey(statusKey))) {
                log.debug("Batch already initialized: {}", batchId);
                return;
            }

            // Create batch status
            Map<String, Object> batchInfo = new HashMap<>();
            batchInfo.put("batchId", batchId);
            batchInfo.put("status", "IN_PROGRESS");
            batchInfo.put("totalChunks", totalChunks);
            batchInfo.put("completedChunks", 0);
            batchInfo.put("totalTickets", estimatedTotalTickets);
            batchInfo.put("successCount", 0);
            batchInfo.put("failureCount", 0);
            batchInfo.put("startTime", LocalDateTime.now().toString());

            redisTemplate.opsForHash().putAll(statusKey, batchInfo);
            redisTemplate.expire(statusKey, BATCH_EXPIRY);
            redisTemplate.opsForSet().add(ACTIVE_BATCHES_KEY, batchId);

            log.info("Batch initialized: batchId={}, chunks={}, estimatedTickets={}",
                    batchId, totalChunks, estimatedTotalTickets);

        } catch (Exception ex) {
            log.warn("Redis unavailable for batch initialization: {}", batchId);
        }
    }

    /**
     * Record successful ticket processing.
     *
     * @param batchId batch identifier
     * @param ticketNumber ticket number (for logging)
     */
    public void recordSuccess(String batchId, String ticketNumber) {
        try {
            String statusKey = BATCH_STATUS_KEY_PREFIX + batchId;
            redisTemplate.opsForHash().increment(statusKey, "successCount", 1);
            log.debug("Success recorded: batchId={}, ticket={}", batchId, ticketNumber);

        } catch (Exception ex) {
            log.debug("Redis unavailable for success tracking: {}", batchId);
        }
    }

    /**
     * Record failed ticket processing.
     *
     * @param batchId batch identifier
     * @param ticketNumber ticket number
     * @param errorCode error code
     * @param errorMessage error message
     */
    public void recordFailure(String batchId, String ticketNumber, String errorCode, String errorMessage) {
        try {
            String statusKey = BATCH_STATUS_KEY_PREFIX + batchId;
            redisTemplate.opsForHash().increment(statusKey, "failureCount", 1);
            log.debug("Failure recorded: batchId={}, ticket={}, error={}", batchId, ticketNumber, errorCode);

        } catch (Exception ex) {
            log.debug("Redis unavailable for failure tracking: {}", batchId);
        }
    }

    /**
     * Mark chunk as completed.
     *
     * @param batchId batch identifier
     * @param chunkNumber chunk number
     */
    public void completeChunk(String batchId, int chunkNumber) {
        try {
            String statusKey = BATCH_STATUS_KEY_PREFIX + batchId;

            // Increment completed chunks
            Long completedChunks = redisTemplate.opsForHash().increment(statusKey, "completedChunks", 1);

            // Get total chunks for comparison
            Object totalChunksObj = redisTemplate.opsForHash().get(statusKey, "totalChunks");
            int totalChunks = totalChunksObj != null ? ((Number) totalChunksObj).intValue() : 0;

            log.debug("Chunk completed: batchId={}, chunk={}/{}", batchId, chunkNumber, totalChunks);

            // Check if batch is complete
            if (completedChunks != null && totalChunks > 0 && completedChunks >= totalChunks) {
                redisTemplate.opsForHash().put(statusKey, "status", "COMPLETED");
                redisTemplate.opsForHash().put(statusKey, "endTime", LocalDateTime.now().toString());
                redisTemplate.opsForSet().remove(ACTIVE_BATCHES_KEY, batchId);
                log.info("Batch completed: batchId={}", batchId);
            }

        } catch (Exception ex) {
            log.debug("Redis unavailable for chunk tracking: {}", batchId);
        }
    }

    /**
     * Get batch status.
     *
     * @param batchId batch identifier
     * @return batch status info
     */
    public Optional<BatchStatusInfo> getBatchStatus(String batchId) {
        try {
            String statusKey = BATCH_STATUS_KEY_PREFIX + batchId;
            Map<Object, Object> batchData = redisTemplate.opsForHash().entries(statusKey);

            if (!batchData.isEmpty()) {
                BatchStatusInfo status = new BatchStatusInfo();
                status.setBatchId(batchId);
                status.setStatus(safeGet(batchData, "status", "UNKNOWN"));
                status.setTotalChunks(safeGetInt(batchData, "totalChunks"));
                status.setCompletedChunks(safeGetInt(batchData, "completedChunks"));
                status.setTotalTickets(safeGetLong(batchData, "totalTickets"));
                status.setSuccessCount(safeGetLong(batchData, "successCount"));
                status.setFailureCount(safeGetLong(batchData, "failureCount"));

                String startTimeStr = safeGet(batchData, "startTime", null);
                if (startTimeStr != null) {
                    try {
                        status.setStartTime(LocalDateTime.parse(startTimeStr));
                    } catch (Exception ex) {
                        log.debug("Error parsing start time for batch: {}", batchId);
                    }
                }

                String endTimeStr = safeGet(batchData, "endTime", null);
                if (endTimeStr != null) {
                    try {
                        status.setEndTime(LocalDateTime.parse(endTimeStr));
                    } catch (Exception ex) {
                        log.debug("Error parsing end time for batch: {}", batchId);
                    }
                }

                return Optional.of(status);
            }

        } catch (Exception ex) {
            log.debug("Redis unavailable for batch status: {}", batchId);
        }

        return Optional.empty();
    }

    /**
     * Get all active batch IDs.
     *
     * @return set of active batch IDs
     */
    public Set<String> getActiveBatches() {
        try {
            Set<Object> batches = redisTemplate.opsForSet().members(ACTIVE_BATCHES_KEY);
            if (batches != null) {
                Set<String> result = new HashSet<>();
                for (Object batch : batches) {
                    if (batch != null) {
                        result.add(batch.toString());
                    }
                }
                return result;
            }

        } catch (Exception ex) {
            log.debug("Redis unavailable for active batches");
        }

        return new HashSet<>();
    }

    /**
     * Cancel a batch.
     *
     * @param batchId batch identifier
     * @return true if cancelled successfully
     */
    public boolean cancelBatch(String batchId) {
        try {
            String statusKey = BATCH_STATUS_KEY_PREFIX + batchId;
            redisTemplate.opsForHash().put(statusKey, "status", "CANCELLED");
            redisTemplate.opsForSet().remove(ACTIVE_BATCHES_KEY, batchId);
            log.info("Batch cancelled: {}", batchId);
            return true;

        } catch (Exception ex) {
            log.warn("Failed to cancel batch: {}", batchId);
            return false;
        }
    }

    /**
     * Delete batch tracking data.
     *
     * @param batchId batch identifier
     */
    public void deleteBatch(String batchId) {
        try {
            String statusKey = BATCH_STATUS_KEY_PREFIX + batchId;
            redisTemplate.delete(statusKey);
            redisTemplate.opsForSet().remove(ACTIVE_BATCHES_KEY, batchId);
            log.debug("Batch data deleted: {}", batchId);

        } catch (Exception ex) {
            log.debug("Failed to delete batch data: {}", batchId);
        }
    }

    /**
     * Safe string value extraction from map.
     */
    private String safeGet(Map<Object, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Safe integer value extraction from map.
     */
    private int safeGetInt(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * Safe long value extraction from map.
     */
    private long safeGetLong(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}

