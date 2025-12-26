package org.example.ticketingapplication.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketingapplication.dto.CreateTicketRequest;
import org.example.ticketingapplication.dto.TicketResponse;
import org.example.ticketingapplication.dto.UpdateTicketRequest;
import org.example.ticketingapplication.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Enterprise-Grade REST API Controller for Ticket Management.
 *
 * Implements the following patterns and best practices:
 * - Thin Controller Pattern: Delegates all business logic to service layer
 * - Centralized Exception Handling: All exceptions handled by GlobalExceptionHandler
 * - Constructor-Based Dependency Injection: Using @RequiredArgsConstructor
 * - Request/Response Logging: All operations are logged for monitoring and debugging
 * - Input Validation: All inputs validated using @Valid annotation
 * - RESTful Design: Proper HTTP methods and status codes
 *
 * Base URL: /api/tickets
 * Version: 1.0
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * Create a new ticket.
     *
     * Endpoint: POST /api/tickets
     * Request Body: CreateTicketRequest (validated)
     * Response: 201 Created with TicketResponse
     *
     * @param request the ticket creation request containing ticket details (validated by @Valid)
     * @return ResponseEntity with 201 Created status and the created ticket
     * @throws DuplicateTicketNumberException if ticket number already exists
     * @throws TicketCreationException if ticket creation fails
     */
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        log.info("Creating new ticket with ticket number: {}", request.getTicketNumber());
        TicketResponse response = ticketService.createTicket(request);
        log.info("Ticket created successfully with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve a ticket by ID.
     *
     * Endpoint: GET /api/tickets/{id}
     * Response: 200 OK with TicketResponse (cached via Redis)
     *
     * @param id the ticket ID (must be a valid positive number)
     * @return ResponseEntity with 200 OK status and the ticket
     * @throws TicketNotFoundException if ticket with given ID does not exist
     * @throws TicketRetrievalException if retrieval fails
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        log.info("Retrieving ticket with ID: {}", id);
        TicketResponse response = ticketService.getTicketById(id);
        log.info("Ticket retrieved successfully with ID: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Update an existing ticket.
     *
     * Endpoint: PUT /api/tickets/{id}
     * Request Body: UpdateTicketRequest (validated)
     * Response: 200 OK with updated TicketResponse
     *
     * @param id the ticket ID to update (must be a valid positive number)
     * @param request the update request containing new ticket values (validated by @Valid)
     * @return ResponseEntity with 200 OK status and the updated ticket
     * @throws TicketNotFoundException if ticket with given ID does not exist
     * @throws TicketUpdateException if update operation fails
     */
    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {
        log.info("Updating ticket with ID: {}", id);
        TicketResponse response = ticketService.updateTicket(id, request);
        log.info("Ticket updated successfully with ID: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    /**
     * Delete a ticket by ID.
     *
     * Endpoint: DELETE /api/tickets/{id}
     * Response: 204 No Content (successful deletion)
     *
     * @param id the ticket ID to delete (must be a valid positive number)
     * @return ResponseEntity with 204 No Content status
     * @throws TicketNotFoundException if ticket with given ID does not exist
     * @throws TicketDeletionException if deletion operation fails
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        log.info("Deleting ticket with ID: {}", id);
        ticketService.deleteTicket(id);
        log.info("Ticket deleted successfully with ID: {}", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

