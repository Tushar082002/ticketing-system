package org.example.ticketingapplication.KafkaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketingapplication.Producer.BulkTicketProducer;
import org.example.ticketingapplication.dto.CreateTicketRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Enterprise-Grade Service for Bulk Upload CSV Processing
 *
 * Responsibilities:
 * - Validate CSV files (format, size, content)
 * - Parse CSV records into CreateTicketRequest DTOs
 * - Validate each record (required fields, null checks, constraints)
 * - Split records into batches of 100
 * - Send batches to Kafka producer for async processing
 * - Handle errors gracefully with meaningful messages
 *
 * Architecture:
 * - Simple, focused CSV parsing (no external dependencies)
 * - Validation before Kafka publishing (fail-fast)
 * - Clean error messages for client feedback
 * - Production-ready logging at all levels
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadProcessingService {

    private final BulkTicketProducer kafkaProducer;

    @Value("${app.kafka.bulk.chunk-size:100}")
    private int chunkSize;

    @Value("${app.kafka.bulk.max-file-size-mb:10}")
    private int maxFileSizeMb;

    @Value("${app.kafka.bulk.max-records:10000}")
    private int maxRecords;

    private static final List<String> REQUIRED_COLUMNS = List.of(
            "ticketnumber", "customerid"
    );

    /**
     * Process bulk upload from CSV file.
     *
     * Flow:
     * 1. Validate file (size, format)
     * 2. Parse CSV records
     * 3. Validate each record
     * 4. Split into chunks
     * 5. Send to Kafka producer
     *
     * @param file the CSV file
     * @param uploadedBy user who uploaded
     * @return batch ID for tracking
     * @throws IllegalArgumentException if file validation fails
     * @throws RuntimeException if processing fails
     */
    public String processBulkUpload(MultipartFile file, String uploadedBy) {
        String filename = file.getOriginalFilename();
        log.info("Bulk upload processing started: file={}, uploadedBy={}, size={} bytes",
                filename, uploadedBy, file.getSize());

        try {
            // Validate file
            validateFile(file);

            // Parse CSV
            List<CreateTicketRequest> tickets = parseCSV(file);

            if (tickets.isEmpty()) {
                throw new IllegalArgumentException("No valid ticket records found in CSV");
            }

            if (tickets.size() > maxRecords) {
                throw new IllegalArgumentException(
                        String.format("Batch size %d exceeds maximum %d", tickets.size(), maxRecords));
            }

            log.info("CSV parsed successfully: {} records found", tickets.size());

            // Convert to TicketEvent for Kafka publishing
            List<org.example.ticketingapplication.dto.TicketEvent> events =
                    convertToTicketEvents(tickets);

            // Send to Kafka
            String batchId = kafkaProducer.publishBatch(events);

            log.info("Bulk upload accepted: batchId={}, records={}, uploadedBy={}",
                    batchId, tickets.size(), uploadedBy);

            return batchId;

        } catch (IllegalArgumentException ex) {
            log.warn("Bulk upload validation failed: {}", ex.getMessage());
            throw ex;

        } catch (IOException ex) {
            log.error("Bulk upload IO error: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to read CSV file: " + ex.getMessage(), ex);

        } catch (Exception ex) {
            log.error("Bulk upload processing error: {}", ex.getMessage(), ex);
            throw new RuntimeException("Bulk upload failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Validate uploaded file.
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(MultipartFile file) {
        String filename = file.getOriginalFilename();

        // Check empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        // Check format
        if (filename == null || (!filename.toLowerCase().endsWith(".csv") &&
                !filename.toLowerCase().endsWith(".txt"))) {
            throw new IllegalArgumentException("File must be CSV or TXT format");
        }

        // Check size
        long maxBytes = (long) maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds %d MB limit", maxFileSizeMb));
        }

        log.debug("File validation passed: {}", filename);
    }

    /**
     * Parse CSV file into CreateTicketRequest list.
     *
     * @param file the CSV file
     * @return list of parsed tickets
     * @throws IOException if file reading fails
     * @throws IllegalArgumentException if CSV validation fails
     */
    private List<CreateTicketRequest> parseCSV(MultipartFile file) throws IOException {
        List<CreateTicketRequest> tickets = new ArrayList<>();
        Set<String> seenTicketNumbers = new HashSet<>();
        int lineNumber = 0;
        int validRecords = 0;
        int invalidRecords = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Read header
            String headerLine = reader.readLine();
            lineNumber = 1;

            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV file must have a header row");
            }

            // Parse header
            String[] headers = parseCSVLine(headerLine);
            java.util.Map<String, Integer> columnIndex = new java.util.HashMap<>();

            for (int i = 0; i < headers.length; i++) {
                String column = headers[i].trim().toLowerCase()
                        .replace(" ", "")
                        .replace("_", "");
                columnIndex.put(column, i);
            }

            // Validate required columns exist
            for (String required : REQUIRED_COLUMNS) {
                if (!columnIndex.containsKey(required)) {
                    throw new IllegalArgumentException("Missing required column: " + required);
                }
            }

            // Parse data rows
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                try {
                    String[] values = parseCSVLine(line);
                    CreateTicketRequest ticket = parseRow(values, columnIndex, lineNumber,
                            seenTicketNumbers);

                    if (ticket != null) {
                        tickets.add(ticket);
                        seenTicketNumbers.add(ticket.getTicketNumber());
                        validRecords++;
                    } else {
                        invalidRecords++;
                    }

                } catch (Exception ex) {
                    log.warn("Invalid record at line {}: {}", lineNumber, ex.getMessage());
                    invalidRecords++;
                }
            }

            log.info("CSV parsing complete: total lines={}, valid records={}, invalid records={}",
                    lineNumber, validRecords, invalidRecords);

            if (validRecords == 0) {
                throw new IllegalArgumentException("No valid records found in CSV");
            }

            return tickets;
        }
    }

    /**
     * Parse CSV line handling quotes and commas.
     *
     * @param line the CSV line
     * @return array of values
     */
    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }

    /**
     * Parse and validate a CSV row.
     *
     * @param values the CSV row values
     * @param columnIndex column name to index mapping
     * @param lineNumber line number for error reporting
     * @param seenTicketNumbers set of already seen ticket numbers
     * @return CreateTicketRequest or null if invalid
     */
    private CreateTicketRequest parseRow(String[] values, java.util.Map<String, Integer> columnIndex,
                                         int lineNumber, Set<String> seenTicketNumbers) {

        // Get ticket number (required)
        String ticketNumber = getColumnValue(values, columnIndex, "ticketnumber");
        if (!StringUtils.hasText(ticketNumber)) {
            log.warn("Line {}: Missing ticket number", lineNumber);
            return null;
        }

        // Check for duplicates
        if (seenTicketNumbers.contains(ticketNumber)) {
            log.warn("Line {}: Duplicate ticket number: {}", lineNumber, ticketNumber);
            return null;
        }

        // Get customer ID (required)
        String customerIdStr = getColumnValue(values, columnIndex, "customerid");
        Long customerId;
        try {
            if (!StringUtils.hasText(customerIdStr)) {
                log.warn("Line {}: Missing customer ID", lineNumber);
                return null;
            }
            customerId = Long.parseLong(customerIdStr.trim());
            if (customerId <= 0) {
                log.warn("Line {}: Invalid customer ID: {}", lineNumber, customerIdStr);
                return null;
            }
        } catch (NumberFormatException ex) {
            log.warn("Line {}: Invalid customer ID format: {}", lineNumber, customerIdStr);
            return null;
        }

        // Get optional fields
        String status = getColumnValue(values, columnIndex, "status");
        if (!StringUtils.hasText(status)) {
            status = "OPEN";
        } else {
            status = status.trim().toUpperCase();
            if (!isValidStatus(status)) {
                status = "OPEN";
            }
        }

        String priority = getColumnValue(values, columnIndex, "priority");
        if (!StringUtils.hasText(priority)) {
            priority = "MEDIUM";
        } else {
            priority = priority.trim().toUpperCase();
            if (!isValidPriority(priority)) {
                priority = "MEDIUM";
            }
        }

        Integer assignedTo = null;
        String assignedToStr = getColumnValue(values, columnIndex, "assignedto");
        if (StringUtils.hasText(assignedToStr)) {
            try {
                assignedTo = Integer.parseInt(assignedToStr.trim());
                if (assignedTo <= 0) {
                    assignedTo = null;
                }
            } catch (NumberFormatException ex) {
                // Ignore, it's optional
            }
        }

        return CreateTicketRequest.builder()
                .ticketNumber(ticketNumber.trim())
                .status(status)
                .priority(priority)
                .customerId(customerId)
                .assignedTo(assignedTo)
                .build();
    }

    /**
     * Get column value from row.
     *
     * @param values row values
     * @param columnIndex column mapping
     * @param column column name
     * @return column value or null
     */
    private String getColumnValue(String[] values, java.util.Map<String, Integer> columnIndex,
                                  String column) {
        Integer index = columnIndex.get(column);
        if (index == null || index >= values.length) {
            return null;
        }
        String value = values[index].trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Validate status value.
     *
     * @param status the status to validate
     * @return true if valid
     */
    private boolean isValidStatus(String status) {
        return Set.of("OPEN", "IN_PROGRESS", "PENDING", "RESOLVED", "CLOSED", "CANCELLED")
                .contains(status);
    }

    /**
     * Validate priority value.
     *
     * @param priority the priority to validate
     * @return true if valid
     */
    private boolean isValidPriority(String priority) {
        return Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(priority);
    }

    /**
     * Convert CreateTicketRequest DTOs to TicketEvent DTOs for Kafka publishing.
     *
     * @param requests the create ticket requests
     * @return list of ticket events
     */
    private List<org.example.ticketingapplication.dto.TicketEvent> convertToTicketEvents(
            List<CreateTicketRequest> requests) {
        List<org.example.ticketingapplication.dto.TicketEvent> events = new ArrayList<>();

        for (CreateTicketRequest request : requests) {
            if (request == null) {
                continue;
            }

            org.example.ticketingapplication.dto.TicketEvent event =
                    org.example.ticketingapplication.dto.TicketEvent.builder()
                            .ticketNumber(request.getTicketNumber())
                            .status(request.getStatus())
                            .priority(request.getPriority())
                            .customerId(request.getCustomerId())
                            .assignedTo(request.getAssignedTo())
                            .build();

            events.add(event);
        }

        return events;
    }
}

