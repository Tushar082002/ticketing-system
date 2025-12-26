package org.example.ticketingapplication.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.example.distributedticketingmanagementsystem.kafka.dto.BulkUploadResponse;
//import org.example.distributedticketingmanagementsystem.kafka.dto.DltMessage;
//import org.example.distributedticketingmanagementsystem.kafka.exception.*;
//import org.example.distributedticketingmanagementsystem.kafka.service.BulkUploadProcessingService;
//import org.example.distributedticketingmanagementsystem.kafka.service.BulkUploadTrackingService;
import org.example.ticketingapplication.KafkaService.BulkUploadProcessingService;
import org.example.ticketingapplication.KafkaService.BulkUploadTrackingService;
import org.example.ticketingapplication.dto.BulkUploadResponse;
import org.example.ticketingapplication.dto.DltMessage;
import org.example.ticketingapplication.event.BulkProcessingErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Enterprise-Grade REST Controller for Bulk Ticket Upload via CSV
 *
 * Responsibilities:
 * - Accept CSV file upload via HTTP multipart endpoint
 * - Validate file (not null, not empty, CSV format, size limit)
 * - Delegate processing to BulkTicketService
 * - Return 202 Accepted immediately (async processing)
 * - Handle errors gracefully
 *
 * Design Pattern:
 * - Thin Controller: No business logic, only HTTP request/response handling
 * - Service Delegation: All processing in BulkTicketService
 * - Separation of Concerns: No Kafka, parsing, or database logic
 * - Async by Design: Returns immediately, processes in background
 *
 * Endpoint:
 * POST /api/tickets/bulk-upload
 * Content-Type: multipart/form-data
 * Parameter: file (CSV file)
 *
 * Response:
 * HTTP 202 Accepted
 * {
 *   "message": "CSV file is being processed asynchronously"
 * }
 *
 * Error Responses:
 * - 400 Bad Request: Invalid file
 * - 500 Internal Server Error: Server error
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/tickets/bulk")
@RequiredArgsConstructor
public class BulkTicketController {

    private final BulkUploadProcessingService processingService;
    private final BulkUploadTrackingService trackingService;

    // ==================== Upload Endpoint ====================

    /**
     * Upload a CSV file for bulk ticket creation via Kafka.
     *
     * @param file       The CSV file to upload
     * @param uploadedBy User identifier (optional)
     * @return Response with batch ID and status
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> uploadBulkTickets(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", required = false, defaultValue = "system")
            String uploadedBy) {

        log.info("📤 BULK UPLOAD REQUEST - file: {}, size: {} bytes, uploadedBy: {}",
                file.getOriginalFilename(), file.getSize(), uploadedBy);

        try {
            String batchId = processingService.processBulkUpload(file, uploadedBy);

            Map<String, Object> response = Map.of(
                    "batchId", batchId,
                    "message", "Batch accepted for processing",
                    "timestamp", LocalDateTime.now()
            );

            log.info("📤 BULK UPLOAD ACCEPTED - batchId: {}", batchId);

            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(ApiResponseWrapper.success(
                            response,
                            "Bulk upload accepted for processing",
                            HttpStatus.ACCEPTED
                    ));

        } catch (IllegalArgumentException e) {
            log.warn("❌ CSV VALIDATION ERROR - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseWrapper.error(
                            e.getMessage(),
                            BulkProcessingErrorCode.INVALID_FILE_FORMAT.getCode(),
                            HttpStatus.BAD_REQUEST
                    ));

        } catch (MaxUploadSizeExceededException e) {
            log.error("❌ FILE TOO LARGE - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONTENT_TOO_LARGE)
                    .body(ApiResponseWrapper.error(
                            "File size exceeds maximum allowed limit",
                            BulkProcessingErrorCode.BATCH_SIZE_EXCEEDED.getCode(),
                            HttpStatus.CONTENT_TOO_LARGE
                    ));

        } catch (Exception e) {
            log.error("💥 UNEXPECTED ERROR - {}", e.getMessage(), e);
            return handleUnexpectedException(e);
        }
    }

    // ==================== Status Endpoints ====================

    /**
     * Get the status of a bulk upload batch.
     */
    @GetMapping("/status/{batchId}")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getBatchStatus(
            @PathVariable String batchId) {

        log.debug("📊 STATUS REQUEST - batchId: {}", batchId);

        try {
            Optional<BulkUploadTrackingService.BatchStatusInfo> statusOpt =
                    trackingService.getBatchStatus(batchId);

            if (statusOpt.isEmpty()) {
                log.warn("📊 BATCH NOT FOUND - batchId: {}", batchId);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseWrapper.error(
                                "Batch not found: " + batchId,
                                BulkProcessingErrorCode.UNKNOWN_ERROR.getCode(),
                                HttpStatus.NOT_FOUND
                        ));
            }

            BulkUploadTrackingService.BatchStatusInfo status = statusOpt.get();
            Map<String, Object> response = Map.of(
                    "batchId", status.getBatchId(),
                    "status", status.getStatus(),
                    "totalChunks", status.getTotalChunks(),
                    "completedChunks", status.getCompletedChunks(),
                    "successCount", status.getSuccessCount(),
                    "failureCount", status.getFailureCount()
            );

            return ResponseEntity.ok(ApiResponseWrapper.success(
                    response,
                    "Batch status retrieved successfully",
                    HttpStatus.OK
            ));


        } catch (Exception e) {
            log.error("📊 STATUS ERROR - batchId: {}, error: {}", batchId, e.getMessage(), e);
            return handleUnexpectedException(e);
        }
    }

    /**
     * Get failures for a batch.
     */
    @GetMapping("/failures/{batchId}")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getBatchFailures(
            @PathVariable String batchId) {

        log.debug("📊 FAILURES REQUEST - batchId: {}", batchId);

        try {
            Map<String, Object> response = Map.of(
                    "batchId", batchId,
                    "message", "DLQ monitoring available - see /api/tickets/bulk/dlt endpoint",
                    "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(ApiResponseWrapper.success(
                    response,
                    "Failures can be checked via DLT endpoint",
                    HttpStatus.OK
            ));

        } catch (Exception e) {
            log.error("📊 FAILURES ERROR - batchId: {}, error: {}", batchId, e.getMessage(), e);
            return handleUnexpectedException(e);
        }
    }

    /**
     * Get all active batches.
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getActiveBatches() {
        log.debug("📊 ACTIVE BATCHES REQUEST");

        try {
            Set<String> activeBatches = trackingService.getActiveBatches();

            Map<String, Object> response = Map.of(
                    "count", activeBatches.size(),
                    "batches", activeBatches,
                    "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(ApiResponseWrapper.success(
                    response,
                    "Active batches retrieved successfully",
                    HttpStatus.OK
            ));

        } catch (Exception e) {
            log.error("📊 ACTIVE BATCHES ERROR - error: {}", e.getMessage(), e);
            return handleUnexpectedException(e);
        }
    }

    // ==================== Cancel Endpoint ====================

    /**
     * Cancel a bulk upload batch.
     */
    @PostMapping("/cancel/{batchId}")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> cancelBatch(
            @PathVariable String batchId,
            @RequestParam(value = "reason", required = false) String reason) {

        log.info("📤 CANCEL REQUEST - batchId: {}, reason: {}", batchId, reason);

        try {
            boolean cancelled = trackingService.cancelBatch(batchId);

            Map<String, Object> response = Map.of(
                    "batchId", batchId,
                    "cancelled", cancelled,
                    "reason", reason != null ? reason : "",
                    "cancelledAt", LocalDateTime.now(),
                    "message", cancelled ? "Batch marked for cancellation" : "Unable to cancel batch"
            );

            return ResponseEntity.ok(ApiResponseWrapper.success(
                    response,
                    cancelled ? "Batch cancelled" : "Cancel request processed",
                    HttpStatus.OK
            ));

        } catch (Exception e) {
            log.error("📤 CANCEL ERROR - batchId: {}, error: {}", batchId, e.getMessage(), e);
            return handleUnexpectedException(e);
        }
    }

    // ==================== DLT Endpoints ====================

    /**
     * Get Dead Letter Topic messages.
     */
    @GetMapping("/dlt")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getDltMessages(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {

        log.debug("☠️ DLT REQUEST - topic: {}, limit: {}", topic, limit);

        try {
            String dltTopic = topic != null ? topic : "ticket.bulk.requests.DLT";

            Map<String, Object> response = Map.of(
                    "topic", dltTopic,
                    "message", "DLT messages monitoring available via dedicated service",
                    "retrievedAt", LocalDateTime.now()
            );

            return ResponseEntity.ok(ApiResponseWrapper.success(
                    response,
                    "DLQ monitoring endpoint active",
                    HttpStatus.OK
            ));

        } catch (Exception e) {
            log.error("☠️ DLT ERROR - error: {}", e.getMessage(), e);
            return handleUnexpectedException(e);
        }
    }

    /**
     * Reprocess a DLT message.
     */
    @PostMapping("/dlt/reprocess/{messageId}")
    public ResponseEntity<ApiResponseWrapper<ReprocessResponse>> reprocessDltMessage(
            @PathVariable String messageId) {

        log.info("🔄 DLT REPROCESS REQUEST - messageId: {}", messageId);

        // TODO: Implement reprocessing logic
        ReprocessResponse response = ReprocessResponse.builder()
                .messageId(messageId)
                .status("NOT_IMPLEMENTED")
                .message("DLT reprocessing is not yet fully implemented")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiResponseWrapper.success(
                        response,
                        "Feature not implemented",
                        HttpStatus.NOT_IMPLEMENTED
                ));
    }

    // ==================== Exception Handlers ====================


    /**
     * Handles unexpected exceptions.
     */
    private <T> ResponseEntity<ApiResponseWrapper<T>> handleUnexpectedException(Exception e) {
        log.error("💥 UNEXPECTED ERROR - {}", e.getMessage(), e);

        BulkProcessingErrorCode errorCode = BulkProcessingErrorCode.fromException(e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseWrapper.<T>builder()
                        .success(false)
                        .message("An unexpected error occurred")
                        .errorCode(errorCode.getCode())
                        .errorDetails(e.getMessage())
                        .retryable(errorCode.isRetryable())
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build());
    }

    // ==================== Response DTOs ====================

    /**
     * Generic API response wrapper.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponseWrapper<T> {
        private boolean success;
        private String message;
        private String errorCode;
        private String errorDetails;
        private Boolean retryable;
        private List<Map<String, String>> validationErrors;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;

        private int status;
        private T data;

        public static <T> ApiResponseWrapper<T> success(T data, String message, HttpStatus status) {
            return ApiResponseWrapper.<T>builder()
                    .success(true)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .status(status.value())
                    .data(data)
                    .build();
        }

        public static <T> ApiResponseWrapper<T> error(String message, String errorCode, HttpStatus status) {
            return ApiResponseWrapper.<T>builder()
                    .success(false)
                    .message(message)
                    .errorCode(errorCode)
                    .timestamp(LocalDateTime.now())
                    .status(status.value())
                    .build();
        }
    }

    /**
     * Response for batch failures.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Deprecated(forRemoval = true)
    public static class BatchFailuresResponse {
        private String batchId;
        private int totalFailures;
        private int page;
        private int pageSize;
        private List<Map<String, String>> failures;
    }

    /**
     * Response for active batches.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Deprecated(forRemoval = true)
    public static class ActiveBatchesResponse {
        private int count;
        private List<BulkUploadResponse> batches;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;
    }

    /**
     * Response for cancel operation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Deprecated(forRemoval = true)
    public static class CancelResponse {
        private String batchId;
        private boolean cancelled;
        private String reason;
        private String message;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime cancelledAt;
    }

    /**
     * Response for DLT messages.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Deprecated(forRemoval = true)
    public static class DltMessagesResponse {
        private String topic;
        private int totalMessages;
        private int returnedMessages;
        private List<DltMessage> messages;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime retrievedAt;
    }

    /**
     * Response for reprocess operation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReprocessResponse {
        private String messageId;
        private String status;
        private String message;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;
    }
}

//    private final BulkTicketService bulkTicketService;
//
//    /**
//     * Upload CSV file for bulk ticket creation.
//     *
//     * Flow:
//     * 1. Validate file is not null/empty
//     * 2. Log file reception
//     * 3. Delegate to service for async processing
//     * 4. Return 202 Accepted immediately
//     *
//     * @param file the CSV file containing ticket records
//     * @return 202 Accepted with processing message
//     * @throws IllegalArgumentException if file validation fails
//     */
//    @PostMapping("/bulk-upload")
//    public ResponseEntity<String> bulkUpload(@RequestParam("file") MultipartFile file) {
//        log.info("Received bulk upload request with file: {}", file.getOriginalFilename());
//
//        // Validate file
//        if (file == null || file.isEmpty()) {
//            log.warn("Invalid file: null or empty");
//            return ResponseEntity.badRequest().body("File cannot be null or empty");
//        }
//
//        String filename = file.getOriginalFilename();
//        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
//            log.warn("Invalid file: not a CSV file - {}", filename);
//            return ResponseEntity.badRequest().body("File must be a CSV file");
//        }
//
//        try {
//            // Delegate to service for processing
//            log.info("Processing CSV file: {} (size: {} bytes)", filename, file.getSize());
//            bulkTicketService.processCsv(file);
//
//            // Return 202 Accepted immediately (async processing)
//            log.info("CSV file submitted for async processing: {}", filename);
//            return ResponseEntity.accepted()
//                    .body("CSV file is being processed asynchronously");
//
//        } catch (Exception ex) {
//            log.error("Error processing CSV file: {}", filename, ex);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Failed to process CSV file: " + ex.getMessage());
//        }
//    }
//}
//
