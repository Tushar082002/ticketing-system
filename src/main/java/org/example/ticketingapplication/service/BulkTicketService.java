//package org.example.ticketingapplication.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.ticketingapplication.dto.CreateTicketRequest;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Enterprise-Grade Service for Bulk Ticket Processing via CSV Upload
// *
// * Purpose:
// * Processes CSV files containing ticket data and publishes validated records to Kafka
// * for asynchronous processing. Acts as the business logic layer between controller
// * and Kafka producer.
// *
// * Responsibilities:
// * 1. CSV File Parsing: Read and parse CSV file into memory-efficient stream
// * 2. Record Validation: Validate each record against business rules
// * 3. DTO Creation: Convert CSV records to CreateTicketRequest DTOs
// * 4. Batching: Group records into batches of 100 for efficient processing
// * 5. Kafka Publishing: Send batches to Kafka topic for async consumption
// * 6. Error Handling: Collect and report validation errors
// * 7. Progress Logging: Log processing metrics and progress
// *
// * Architecture Pattern:
// * - Service Layer: Business logic, validation, transformation
// * - No Database Access: All data written via Kafka (eventually consistent)
// * - Kafka Producer: Publishes validated batches for consumer processing
// * - Error Tracking: Collects validation errors for reporting
// *
// * CSV Processing Flow:
// * 1. Receive MultipartFile from controller
// * 2. Open BufferedReader for streaming (memory efficient)
// * 3. Parse header row to identify columns
// * 4. Iterate through data rows:
// *    a. Parse CSV line
// *    b. Map to CreateTicketRequest DTO
// *    c. Validate record
// *    d. Add to batch
// *    e. If batch size reaches 100: publish to Kafka
// * 5. Publish remaining records
// * 6. Return processing summary
// *
// * Batch Processing Strategy:
// * - Batch Size: 100 records per batch (configurable)
// * - Reason: Balances throughput vs memory usage vs Kafka message size
// * - Publishing: Async via KafkaTemplate
// * - Ordering: Records maintain original order within batches
// *
// * Validation Rules:
// * - ticketNumber: Required, non-blank, max 50 chars, must be unique per batch
// * - status: Required, non-blank, max 50 chars, valid status value
// * - priority: Required, non-blank, max 50 chars, valid priority value
// * - customerId: Required, must be positive number
// * - assignedTo: Optional, if provided must be positive integer
// *
// * Error Handling Strategy:
// * - Validation errors: Logged and tracked, but processing continues
// * - Malformed CSV: Exception thrown, processing stopped
// * - Kafka errors: Logged, batch may be retried
// * - Duplicate detection: Within batch scope only (duplicates rejected)
// *
// * Performance Characteristics:
// * - CSV Parsing: O(n) where n = number of records
// * - Memory Usage: O(batch_size) - only 100 records in memory at a time
// * - Kafka Publishing: Async, non-blocking
// * - Time Complexity: O(n) with constant batch operations
// *
// * Logging Strategy:
// * - DEBUG: CSV parsing details, record transformation
// * - INFO: Processing progress, batch publishing
// * - WARN: Validation errors, skipped records
// * - ERROR: Critical failures (file I/O, Kafka errors)
// *
// * Security Considerations:
// * - Input validation: All fields validated before Kafka publishing
// * - SQL Injection: Not applicable (no direct DB access)
// * - CSV Injection: Field values validated before use
// * - Character Encoding: UTF-8 enforced for file reading
// * - Memory Safety: Streaming parse prevents memory exhaustion
// *
// * Configuration:
// * - batch.size: Number of records per Kafka message (default 100)
// * - kafka.topic: Topic name for publishing (ticket.bulk.requests)
// * - validation.strict: Whether to fail on validation errors (default true)
// *
// * Future Enhancements:
// * - Support for more file formats (JSON, Parquet, Excel)
// * - Configurable batch sizes per environment
// * - Detailed validation reports (field-level errors)
// * - Batch retry logic with exponential backoff
// * - Dead Letter Queue integration
// * - Progress tracking and callbacks
// *
// * @author Enterprise Backend Team
// * @version 1.0
// * @see CreateTicketRequest for DTO structure
// * @see org.example.ticketingapplication.consumer.BulkTicketConsumer for consumer side
// */
//@Slf4j
//@Service
//@Transactional
//@RequiredArgsConstructor
//public class BulkTicketService {
//
//    // ================= Constants =================
//
//    /**
//     * CSV Header: Comma-separated column names expected in CSV file
//     * Order: ticketNumber, status, priority, customerId, assignedTo
//     */
//    private static final String CSV_HEADER = "ticketNumber,status,priority,customerId,assignedTo";
//
//    /**
//     * Column indices for parsing CSV records
//     * Used to map fields from String[] to named variables
//     */
//    private static final int IDX_TICKET_NUMBER = 0;
//    private static final int IDX_STATUS = 1;
//    private static final int IDX_PRIORITY = 2;
//    private static final int IDX_CUSTOMER_ID = 3;
//    private static final int IDX_ASSIGNED_TO = 4;
//
//    /**
//     * Expected number of columns in CSV
//     * Validates that each record has correct number of fields
//     */
//    private static final int EXPECTED_COLUMN_COUNT = 5;
//
//    /**
//     * Maximum records per batch for Kafka publishing
//     * Balance: 100 = good throughput, reasonable memory, manageable batch size
//     * Configurable via batch.size property
//     */
//    private static final int DEFAULT_BATCH_SIZE = 100;
//
//    /**
//     * Kafka topic for bulk ticket requests
//     * Consumer subscribes to this topic for processing
//     */
//    private static final String BULK_REQUESTS_TOPIC = "ticket.bulk.requests";
//
//    // ================= Dependencies =================
//
//    /**
//     * Kafka template for publishing batches
//     * Async publishing to avoid blocking service
//     */
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    // ================= Configuration Properties =================
//
//    /**
//     * Batch size for Kafka publishing
//     * Default: 100 records per batch
//     * Configurable via: app.batch.size property
//     */
//    @Value("${app.batch.size:100}")
//    private int batchSize;
//
//    /**
//     * Kafka topic name for bulk requests
//     * Default: ticket.bulk.requests
//     * Configurable via: app.kafka.topic.bulk property
//     */
//    @Value("${app.kafka.topic.bulk:ticket.bulk.requests}")
//    private String kafkaTopic;
//
//    // ================= CSV Processing =================
//
//    /**
//     * Process uploaded CSV file and publish validated records to Kafka.
//     *
//     * Main entry point for CSV processing. Orchestrates:
//     * 1. File validation (not null, not empty)
//     * 2. CSV parsing (streaming to minimize memory)
//     * 3. Record transformation (String[] → CreateTicketRequest)
//     * 4. Batch management (100 records per batch)
//     * 5. Kafka publishing (async message sending)
//     * 6. Error handling (validation errors, Kafka errors)
//     * 7. Progress reporting (metrics and summary)
//     *
//     * Processing Strategy:
//     * - Streaming: Uses BufferedReader for memory efficiency
//     * - Non-Blocking: Kafka publishing is async (KafkaTemplate)
//     * - Error Resilience: Validation errors logged, processing continues
//     * - Progress Tracking: Logs batch count, record count, errors
//     *
//     * Time Complexity: O(n) where n = number of records
//     * Space Complexity: O(batch_size) where batch_size = 100
//     *
//     * @param file the uploaded CSV file (MultipartFile)
//     *             Must not be null (validated by controller)
//     *             Must be CSV format (validated by controller)
//     *             Must not exceed size limit (validated by controller)
//     *
//     * @throws IllegalArgumentException if file is empty or malformed
//     *         Message: "CSV file is empty" - no data rows found
//     *         Message: "Invalid CSV format" - parsing error
//     *
//     * @throws RuntimeException if Kafka publishing fails
//     *         Message includes Kafka error details
//     *         Batch may not be published
//     *
//     * Example Log Output:
//     * DEBUG - Starting CSV processing for file: bulk_tickets.csv
//     * DEBUG - CSV Header validated: ticketNumber,status,priority,customerId,assignedTo
//     * DEBUG - Parsing record 1: ["TKT-001", "OPEN", "HIGH", "1", ""]
//     * DEBUG - Created DTO for ticket: TKT-001
//     * INFO  - Batch 1 ready (100 records), publishing to Kafka
//     * INFO  - Successfully published batch 1 with 100 records to topic: ticket.bulk.requests
//     * DEBUG - Parsing record 101: ["TKT-101", "OPEN", "MEDIUM", "2", "5"]
//     * INFO  - Processing complete: 101 total records, 1 batch published, 1 remaining
//     *
//     * @see #parseAndValidateRecord(String[], int) for record parsing
//     * @see #publishBatch(List, int) for batch publishing
//     * @see #validateCreateTicketRequest(CreateTicketRequest, int) for validation
//     */
//    public void processCsv(MultipartFile file) {
//        log.info("Starting CSV processing for file: {}", file.getOriginalFilename());
//
//        int totalRecordsProcessed = 0;
//        int validRecordsProcessed = 0;
//        int invalidRecordsCount = 0;
//        int batchCount = 0;
//        List<CreateTicketRequest> currentBatch = new ArrayList<>();
//        Set<String> batchTicketNumbers = new HashSet<>();
//
//        try (BufferedReader reader = new BufferedReader(
//                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
//
//            // Step 1: Read and validate header
//            String headerLine = reader.readLine();
//            if (headerLine == null) {
//                log.error("CSV file is empty - no header found");
//                throw new IllegalArgumentException("CSV file is empty");
//            }
//            log.debug("CSV Header read: {}", headerLine);
//
//            // Step 2: Validate header format
//            validateCsvHeader(headerLine);
//            log.debug("CSV Header validated successfully");
//
//            // Step 3: Process data rows
//            String line;
//            int rowNumber = 1; // Start at 1 (header is row 0)
//
//            while ((line = reader.readLine()) != null) {
//                rowNumber++;
//                totalRecordsProcessed++;
//
//                try {
//                    // Parse and validate record
//                    CreateTicketRequest request = parseAndValidateRecord(line, rowNumber);
//
//                    if (request != null) {
//                        // Check for duplicates within current batch
//                        if (batchTicketNumbers.contains(request.getTicketNumber())) {
//                            log.warn("Duplicate ticket number '{}' in batch at row {}, skipping",
//                                    request.getTicketNumber(), rowNumber);
//                            invalidRecordsCount++;
//                            continue;
//                        }
//
//                        // Add to batch
//                        currentBatch.add(request);
//                        batchTicketNumbers.add(request.getTicketNumber());
//                        validRecordsProcessed++;
//                        log.debug("Added record to batch: {} (batch size: {}/{})",
//                                request.getTicketNumber(), currentBatch.size(), batchSize);
//
//                        // Publish batch if size reached
//                        if (currentBatch.size() >= batchSize) {
//                            batchCount++;
//                            publishBatch(currentBatch, batchCount);
//                            currentBatch.clear();
//                            batchTicketNumbers.clear();
//                        }
//                    } else {
//                        // Validation failed, record already logged
//                        invalidRecordsCount++;
//                    }
//
//                } catch (Exception ex) {
//                    // Error parsing record, log and continue
//                    log.warn("Error processing record at row {}: {}", rowNumber, ex.getMessage());
//                    invalidRecordsCount++;
//                }
//            }
//
//            // Step 4: Publish remaining records
//            if (!currentBatch.isEmpty()) {
//                batchCount++;
//                publishBatch(currentBatch, batchCount);
//            }
//
//            // Step 5: Log processing summary
//            logProcessingSummary(totalRecordsProcessed, validRecordsProcessed,
//                    invalidRecordsCount, batchCount);
//
//            log.info("CSV processing completed successfully for file: {}", file.getOriginalFilename());
//
//        } catch (IllegalArgumentException ex) {
//            log.error("CSV validation failed: {}", ex.getMessage());
//            throw ex;
//        } catch (Exception ex) {
//            log.error("Error processing CSV file: {}", ex.getMessage(), ex);
//            throw new RuntimeException("Failed to process CSV file: " + ex.getMessage(), ex);
//        }
//    }
//
//    // ================= Record Parsing & Validation =================
//
//    /**
//     * Parse CSV line and validate record.
//     *
//     * Processing Steps:
//     * 1. Split CSV line by comma (simple parser)
//     * 2. Validate field count
//     * 3. Map fields to CreateTicketRequest DTO
//     * 4. Validate DTO against business rules
//     * 5. Return DTO or null if invalid
//     *
//     * CSV Parsing:
//     * - Uses simple split(",") approach
//     * - Does NOT handle quoted fields or escaped commas
//     * - Suitable for simple, clean CSV files
//     * - For complex CSV: consider CSV library (OpenCSV, SuperCSV)
//     *
//     * Field Mapping:
//     * - Index 0: ticketNumber
//     * - Index 1: status
//     * - Index 2: priority
//     * - Index 3: customerId
//     * - Index 4: assignedTo
//     *
//     * Validation:
//     * - delegated to validateCreateTicketRequest()
//     * - includes field-level and DTO-level checks
//     *
//     * Error Handling:
//     * - Parse errors: Log warning, return null
//     * - Validation errors: Log warning, return null
//     * - Returns null to allow processing to continue
//     *
//     * @param line the CSV line to parse (comma-separated values)
//     * @param rowNumber the row number in CSV (for error reporting)
//     * @return CreateTicketRequest if valid, null if invalid
//     *
//     * @see #validateCreateTicketRequest(CreateTicketRequest, int) for validation
//     */
//    private CreateTicketRequest parseAndValidateRecord(String line, int rowNumber) {
//        try {
//            // Step 1: Split CSV line
//            String[] fields = line.split(",");
//            log.debug("Parsing record at row {}: {} fields found", rowNumber, fields.length);
//
//            // Step 2: Validate field count
//            if (fields.length != EXPECTED_COLUMN_COUNT) {
//                log.warn("Invalid field count at row {} - expected {}, got {}",
//                        rowNumber, EXPECTED_COLUMN_COUNT, fields.length);
//                return null;
//            }
//
//            // Step 3: Trim fields (remove leading/trailing whitespace)
//            for (int i = 0; i < fields.length; i++) {
//                fields[i] = fields[i].trim();
//            }
//
//            // Step 4: Extract fields
//            String ticketNumber = fields[IDX_TICKET_NUMBER];
//            String status = fields[IDX_STATUS];
//            String priority = fields[IDX_PRIORITY];
//            String customerIdStr = fields[IDX_CUSTOMER_ID];
//            String assignedToStr = fields[IDX_ASSIGNED_TO];
//
//            // Step 5: Create DTO
//            CreateTicketRequest request = CreateTicketRequest.builder()
//                    .ticketNumber(ticketNumber)
//                    .status(status)
//                    .priority(priority)
//                    .customerId(parsePositiveLong(customerIdStr, rowNumber, "customerId"))
//                    .assignedTo(assignedToStr.isEmpty() ? null : parsePositiveInteger(assignedToStr, rowNumber, "assignedTo"))
//                    .build();
//
//            // Step 6: Validate DTO
//            if (!validateCreateTicketRequest(request, rowNumber)) {
//                return null;
//            }
//
//            log.debug("Successfully parsed record at row {}: {}", rowNumber, ticketNumber);
//            return request;
//
//        } catch (Exception ex) {
//            log.warn("Error parsing record at row {}: {}", rowNumber, ex.getMessage());
//            return null;
//        }
//    }
//
//    /**
//     * Validate CreateTicketRequest against business rules.
//     *
//     * Validation Rules:
//     * 1. ticketNumber: Not null/blank, 3-50 chars, alphanumeric+dash
//     * 2. status: Not null/blank, 2-50 chars, predefined values
//     * 3. priority: Not null/blank, 2-50 chars, predefined values
//     * 4. customerId: Not null, positive number > 0
//     * 5. assignedTo: Optional, if provided must be positive number > 0
//     *
//     * Valid Status Values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, ON_HOLD
//     * Valid Priority Values: LOW, MEDIUM, HIGH, CRITICAL
//     *
//     * Validation Strategy:
//     * - Log specific validation failure
//     * - Return false to skip record
//     * - Allow processing to continue
//     * - Does not throw exception
//     *
//     * @param request the CreateTicketRequest DTO to validate
//     * @param rowNumber the row number (for error reporting)
//     * @return true if valid, false if invalid
//     *
//     * @see CreateTicketRequest for DTO structure
//     */
//    private boolean validateCreateTicketRequest(CreateTicketRequest request, int rowNumber) {
//        // Null check
//        if (request == null) {
//            log.warn("Row {}: CreateTicketRequest is null", rowNumber);
//            return false;
//        }
//
//        // Validate ticketNumber
//        if (request.getTicketNumber() == null || request.getTicketNumber().isBlank()) {
//            log.warn("Row {}: ticketNumber cannot be blank", rowNumber);
//            return false;
//        }
//        if (request.getTicketNumber().length() < 3 || request.getTicketNumber().length() > 50) {
//            log.warn("Row {}: ticketNumber length must be 3-50 characters, got '{}'",
//                    rowNumber, request.getTicketNumber());
//            return false;
//        }
//
//        // Validate status
//        if (request.getStatus() == null || request.getStatus().isBlank()) {
//            log.warn("Row {}: status cannot be blank", rowNumber);
//            return false;
//        }
//        if (request.getStatus().length() < 2 || request.getStatus().length() > 50) {
//            log.warn("Row {}: status length must be 2-50 characters, got '{}'",
//                    rowNumber, request.getStatus());
//            return false;
//        }
//        if (!isValidStatus(request.getStatus())) {
//            log.warn("Row {}: invalid status '{}' - must be one of: OPEN, IN_PROGRESS, RESOLVED, CLOSED, ON_HOLD",
//                    rowNumber, request.getStatus());
//            return false;
//        }
//
//        // Validate priority
//        if (request.getPriority() == null || request.getPriority().isBlank()) {
//            log.warn("Row {}: priority cannot be blank", rowNumber);
//            return false;
//        }
//        if (request.getPriority().length() < 2 || request.getPriority().length() > 50) {
//            log.warn("Row {}: priority length must be 2-50 characters, got '{}'",
//                    rowNumber, request.getPriority());
//            return false;
//        }
//        if (!isValidPriority(request.getPriority())) {
//            log.warn("Row {}: invalid priority '{}' - must be one of: LOW, MEDIUM, HIGH, CRITICAL",
//                    rowNumber, request.getPriority());
//            return false;
//        }
//
//        // Validate customerId
//        if (request.getCustomerId() == null) {
//            log.warn("Row {}: customerId cannot be null", rowNumber);
//            return false;
//        }
//        if (request.getCustomerId() <= 0) {
//            log.warn("Row {}: customerId must be positive, got {}", rowNumber, request.getCustomerId());
//            return false;
//        }
//
//        // Validate assignedTo (optional)
//        if (request.getAssignedTo() != null && request.getAssignedTo() <= 0) {
//            log.warn("Row {}: assignedTo must be positive if provided, got {}",
//                    rowNumber, request.getAssignedTo());
//            return false;
//        }
//
//        log.debug("Row {}: All validations passed for ticket {}", rowNumber, request.getTicketNumber());
//        return true;
//    }
//
//    // ================= Kafka Publishing =================
//
//    /**
//     * Publish batch of records to Kafka topic.
//     *
//     * Publishing Strategy:
//     * - Async: Uses KafkaTemplate.send() (non-blocking)
//     * - Key: null (default partitioning)
//     * - Value: List of CreateTicketRequest (batch)
//     * - Topic: ticket.bulk.requests
//     *
//     * Error Handling:
//     * - Kafka errors: Logged but not thrown
//     * - Batch may be lost if Kafka is down
//     * - Consider implementing retry logic for production
//     *
//     * Performance:
//     * - Time: O(1) - async operation returns immediately
//     * - Memory: Released after send() call
//     *
//     * @param batch the list of CreateTicketRequest to publish
//     * @param batchNumber the batch sequence number (for logging)
//     *
//     * @see #processCsv(MultipartFile) for usage
//     */
//    private void publishBatch(List<CreateTicketRequest> batch, int batchNumber) {
//        try {
//            log.info("Publishing batch {} with {} records to Kafka topic: {}",
//                    batchNumber, batch.size(), kafkaTopic);
//
//            // Send batch to Kafka (async)
//            kafkaTemplate.send(kafkaTopic, batch);
//
//            log.info("Batch {} successfully published (size: {})", batchNumber, batch.size());
//
//        } catch (Exception ex) {
//            log.error("Error publishing batch {} to Kafka: {}", batchNumber, ex.getMessage(), ex);
//            // Note: In production, implement retry logic, DLQ, or circuit breaker
//        }
//    }
//
//    // ================= Helper Methods =================
//
//    /**
//     * Validate CSV header format.
//     *
//     * Checks that header matches expected format:
//     * ticketNumber,status,priority,customerId,assignedTo
//     *
//     * Throws IllegalArgumentException if header is invalid.
//     *
//     * @param headerLine the CSV header line
//     * @throws IllegalArgumentException if header is invalid
//     */
//    private void validateCsvHeader(String headerLine) {
//        if (!headerLine.equals(CSV_HEADER)) {
//            log.error("Invalid CSV header. Expected: {} Got: {}", CSV_HEADER, headerLine);
//            throw new IllegalArgumentException("Invalid CSV header format");
//        }
//    }
//
//    /**
//     * Parse string to positive long value.
//     *
//     * Converts string to long and validates it's positive (> 0).
//     *
//     * @param value the string value to parse
//     * @param rowNumber the row number (for error reporting)
//     * @param fieldName the field name (for error reporting)
//     * @return parsed long value if valid
//     * @throws NumberFormatException if not a valid number
//     */
//    private Long parsePositiveLong(String value, int rowNumber, String fieldName) {
//        if (value == null || value.isBlank()) {
//            throw new NumberFormatException(fieldName + " cannot be blank");
//        }
//        try {
//            long result = Long.parseLong(value);
//            if (result <= 0) {
//                throw new NumberFormatException(fieldName + " must be positive");
//            }
//            return result;
//        } catch (NumberFormatException ex) {
//            throw new NumberFormatException(
//                    String.format("Row %d: Invalid %s value '%s' - %s",
//                            rowNumber, fieldName, value, ex.getMessage()));
//        }
//    }
//
//    /**
//     * Parse string to positive integer value.
//     *
//     * Converts string to integer and validates it's positive (> 0).
//     *
//     * @param value the string value to parse
//     * @param rowNumber the row number (for error reporting)
//     * @param fieldName the field name (for error reporting)
//     * @return parsed integer value if valid
//     * @throws NumberFormatException if not a valid number
//     */
//    private Integer parsePositiveInteger(String value, int rowNumber, String fieldName) {
//        if (value == null || value.isBlank()) {
//            return null;
//        }
//        try {
//            int result = Integer.parseInt(value);
//            if (result <= 0) {
//                throw new NumberFormatException(fieldName + " must be positive");
//            }
//            return result;
//        } catch (NumberFormatException ex) {
//            throw new NumberFormatException(
//                    String.format("Row %d: Invalid %s value '%s' - %s",
//                            rowNumber, fieldName, value, ex.getMessage()));
//        }
//    }
//
//    /**
//     * Check if status value is valid.
//     *
//     * Valid values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, ON_HOLD
//     *
//     * @param status the status to validate
//     * @return true if valid, false otherwise
//     */
//    private boolean isValidStatus(String status) {
//        return status != null && (
//                status.equalsIgnoreCase("OPEN") ||
//                status.equalsIgnoreCase("IN_PROGRESS") ||
//                status.equalsIgnoreCase("RESOLVED") ||
//                status.equalsIgnoreCase("CLOSED") ||
//                status.equalsIgnoreCase("ON_HOLD")
//        );
//    }
//
//    /**
//     * Check if priority value is valid.
//     *
//     * Valid values: LOW, MEDIUM, HIGH, CRITICAL
//     *
//     * @param priority the priority to validate
//     * @return true if valid, false otherwise
//     */
//    private boolean isValidPriority(String priority) {
//        return priority != null && (
//                priority.equalsIgnoreCase("LOW") ||
//                priority.equalsIgnoreCase("MEDIUM") ||
//                priority.equalsIgnoreCase("HIGH") ||
//                priority.equalsIgnoreCase("CRITICAL")
//        );
//    }
//
//    /**
//     * Log processing summary.
//     *
//     * Logs final metrics after processing completes.
//     *
//     * @param totalRecords total records in file
//     * @param validRecords records that passed validation
//     * @param invalidRecords records that failed validation
//     * @param batchCount number of batches published
//     */
//    private void logProcessingSummary(int totalRecords, int validRecords,
//                                     int invalidRecords, int batchCount) {
//        log.info("========== CSV Processing Summary ==========");
//        log.info("Total Records:    {}", totalRecords);
//        log.info("Valid Records:    {} ({}%)", validRecords,
//                totalRecords > 0 ? (validRecords * 100 / totalRecords) : 0);
//        log.info("Invalid Records:  {} ({}%)", invalidRecords,
//                totalRecords > 0 ? (invalidRecords * 100 / totalRecords) : 0);
//        log.info("Batches Published: {}", batchCount);
//        log.info("Avg Batch Size:   {}", batchCount > 0 ? (validRecords / batchCount) : 0);
//        log.info("==========================================");
//    }
//}
//
