package org.example.ticketingapplication.service;

import org.example.ticketingapplication.dto.CreateTicketRequest;
import org.example.ticketingapplication.dto.TicketResponse;
import org.example.ticketingapplication.dto.UpdateTicketRequest;

/**
 * TicketService Interface - Business Logic Contract for Ticket Management
 *
 * Defines the contract for all ticket-related business operations.
 * Separates interface from implementation for loose coupling and testability.
 *
 * Design Patterns:
 * - Service Facade Pattern: Provides high-level business operations
 * - Dependency Inversion Principle: Depend on abstraction, not implementation
 * - Single Responsibility Principle: Only ticket-related business logic
 *
 * Architecture Layer:
 * - Service Layer: Business logic, validation, orchestration
 * - Above: Controllers (thin, request routing only)
 * - Below: Repository Layer (data access only)
 *
 * Caching Strategy:
 * - Implementation: Redis with Lettuce connection pooling
 * - Key format: "ticket:{id}"
 * - TTL: 30 minutes for ticket data
 * - Cache invalidation: Manual on write operations (update/delete)
 * - Cache population: On successful read operations
 *
 * Transaction Management:
 * - All operations are transactional by default (@Transactional)
 * - Read operations: Read-only transactions (@Transactional(readOnly = true))
 * - Write operations: Read-write transactions (default)
 * - Isolation level: Database default (usually READ_COMMITTED)
 * - Rollback: Automatic on RuntimeException
 *
 * Error Handling:
 * - All exceptions are unchecked RuntimeExceptions
 * - Centralized handling via GlobalExceptionHandler
 * - No checked exceptions in service contract
 * - Exceptions include root cause for debugging
 *
 * Performance Considerations:
 * - Read operations: O(1) from cache, O(log n) from database (indexed queries)
 * - Write operations: O(1) + cache invalidation
 * - Bulk operations: Use batch processing for efficiency
 *
 * Validation:
 * - Input validation: DTO-level using @Valid annotation
 * - Business validation: Service layer implementation
 * - Constraint validation: Repository/Database level
 *
 * Usage Examples:
 * // Create new ticket
 * CreateTicketRequest request = new CreateTicketRequest(...);
 * TicketResponse created = ticketService.createTicket(request); // 201 Created
 *
 * // Get ticket (cached)
 * TicketResponse ticket = ticketService.getTicketById(1L); // Reads from cache first
 *
 * // Update ticket (cache invalidated)
 * UpdateTicketRequest update = new UpdateTicketRequest(...);
 * TicketResponse updated = ticketService.updateTicket(1L, update); // Cache cleared
 *
 * // Delete ticket (cache invalidated)
 * ticketService.deleteTicket(1L); // Cache cleared
 *
 * @author Enterprise Backend Team
 * @version 1.0
 * @see TicketServiceImpl for implementation details
 * @see org.example.ticketingapplication.controller.TicketController for REST endpoints
 * @see org.example.ticketingapplication.entity.Ticket for data model
 */
public interface TicketService {

    /**
     * Create a new ticket with the provided information.
     *
     * Operation: INSERT
     * HTTP Method: POST /api/v1/tickets
     * Response Status: 201 Created
     * Transactional: Yes (READ_WRITE)
     * Cached: No (new resource)
     *
     * Business Rules Enforced:
     * - Ticket number must be unique across all tickets
     * - All required fields must be provided and validated
     * - Customer ID must reference a valid customer
     * - Status must be valid ticket status
     * - Priority must be valid priority level
     *
     * Validation Flow:
     * 1. DTO-level validation (@Valid on controller)
     * 2. Business validation (duplicate check)
     * 3. Service-level validation
     * 4. Database constraints (backup)
     *
     * Side Effects:
     * - Inserts new record in database
     * - Does NOT populate cache (new ticket, unlikely to be accessed immediately)
     * - Logs ticket creation event
     *
     * Performance:
     * - Time Complexity: O(log n) for duplicate check + O(1) for insert
     * - Space Complexity: O(1)
     * - Database Operation: Single INSERT with unique constraint validation
     *
     * @param request the ticket creation request DTO containing:
     *        - ticketNumber: Unique identifier (required, 3-50 chars)
     *        - status: Initial status (required, e.g., "OPEN")
     *        - priority: Priority level (required, e.g., "HIGH")
     *        - customerId: Customer ID (required, must exist)
     *        - assignedTo: Optional user assignment (null if unassigned)
     *
     * @return TicketResponse containing:
     *        - id: Auto-generated database ID
     *        - All request fields
     *        - createdAt: Creation timestamp (auto-set)
     *
     * @throws org.example.ticketingapplication.exception.DuplicateTicketNumberException
     *         if ticketNumber already exists in the system
     * @throws org.example.ticketingapplication.exception.TicketCreationException
     *         if database operation fails (constraint violation, data error)
     * @throws jakarta.validation.ConstraintViolationException
     *         if input validation fails (caught by GlobalExceptionHandler)
     *
     * Example:
     * CreateTicketRequest request = CreateTicketRequest.builder()
     *     .ticketNumber("TKT-001")
     *     .status("OPEN")
     *     .priority("HIGH")
     *     .customerId(10L)
     *     .assignedTo(null)
     *     .build();
     * TicketResponse response = ticketService.createTicket(request);
     * // response.getId() = 1, response.getCreatedAt() = now
     */
    TicketResponse createTicket(CreateTicketRequest request);

    /**
     * Retrieve a ticket by its ID with automatic caching.
     *
     * Operation: SELECT
     * HTTP Method: GET /api/v1/tickets/{id}
     * Response Status: 200 OK
     * Transactional: Yes (READ_ONLY)
     * Cached: Yes (30 minutes TTL)
     *
     * Caching Strategy:
     * 1. Check Redis cache with key "ticket:{id}"
     * 2. If cache HIT: Return cached TicketResponse (O(1) operation)
     * 3. If cache MISS: Query database, cache result, return (O(log n))
     * 4. Cache only valid, found tickets
     * 5. Cache miss on 404 errors (no negative caching)
     *
     * Cache Key Format: "ticket:{id}"
     * Cache TTL: 30 minutes (1800 seconds)
     * Cache Invalidation: Triggered by update/delete operations
     *
     * Logging:
     * - INFO level: "Cache HIT" or "Cache MISS"
     * - INFO level: "Stored in cache with TTL 30 minutes"
     * - ERROR level: If retrieval fails
     *
     * Performance Characteristics:
     * - Cache HIT: O(1) - sub-millisecond response from Redis
     * - Cache MISS: O(log n) - database index lookup (via idx_id)
     * - Expected cache hit rate: 60-80% in typical usage
     *
     * @param id the ticket ID (must be positive Long)
     *           - Used to construct cache key: "ticket:{id}"
     *           - Validated by @PathVariable in controller
     *
     * @return TicketResponse containing complete ticket data:
     *        - id: The requested ticket ID
     *        - ticketNumber: Unique ticket identifier
     *        - status: Current ticket status
     *        - priority: Priority level
     *        - createdAt: Ticket creation timestamp
     *        - customerId: Associated customer
     *        - assignedTo: Assigned user (null if unassigned)
     *
     * @throws org.example.ticketingapplication.exception.TicketNotFoundException
     *         if no ticket with given ID exists (404 Not Found)
     * @throws org.example.ticketingapplication.exception.TicketRetrievalException
     *         if database query fails (500 Internal Server Error, may retry)
     *
     * Example:
     * // Cache miss (first call)
     * TicketResponse ticket1 = ticketService.getTicketById(1L);
     * // Cache HIT (subsequent calls within 30 minutes)
     * TicketResponse ticket2 = ticketService.getTicketById(1L);
     * // Both return same data, but ticket2 comes from cache
     */
    TicketResponse getTicketById(Long id);

    /**
     * Update an existing ticket with new information.
     *
     * Operation: UPDATE (Partial)
     * HTTP Method: PUT /api/v1/tickets/{id}
     * Response Status: 200 OK
     * Transactional: Yes (READ_WRITE)
     * Cached: Cache INVALIDATED after update
     *
     * Update Strategy: Partial Updates (PATCH-like via PUT)
     * - Only provided (non-null) fields are updated
     * - Null fields in request preserve existing values
     * - Supports selective field updates without full object replacement
     * - Example: { "status": "IN_PROGRESS" } updates only status
     *
     * Updatable Fields:
     * - status: Ticket status (e.g., OPEN → IN_PROGRESS → RESOLVED)
     * - priority: Priority level can be changed
     * - assignedTo: Ticket assignment (assign/reassign/unassign)
     *
     * Immutable Fields (cannot be changed):
     * - id: Primary key (never changes)
     * - ticketNumber: Business identifier (unique, never changes)
     * - createdAt: Creation timestamp (audit trail, never changes)
     * - customerId: Original customer (audit trail, never changes)
     *
     * Cache Invalidation:
     * - After successful update, cache key "ticket:{id}" is deleted
     * - Next getTicketById() call will query database and repopulate cache
     * - Ensures clients receive fresh data after update
     * - Invalidation happens regardless of which fields changed
     *
     * Side Effects:
     * - Updates database record
     * - Deletes Redis cache entry "ticket:{id}"
     * - Logs update event with changed fields
     * - Updates database timestamps (if configured)
     *
     * Performance:
     * - Time Complexity: O(log n) for database lookup + O(1) for update + O(1) for cache delete
     * - Database Operation: UPDATE with WHERE id = {id}
     * - Index used: Primary key index (idx_id)
     *
     * @param id the ticket ID to update (must be positive Long)
     *           - Must reference existing ticket
     *           - Cannot be changed by update operation
     *
     * @param request the update request containing new values:
     *        - status: New status (optional, null means no change)
     *                  Valid values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, ON_HOLD
     *        - priority: New priority (optional, null means no change)
     *                    Valid values: LOW, MEDIUM, HIGH, CRITICAL
     *        - assignedTo: New assigned user (optional, null means no change)
     *                      Null value in request = unassign ticket
     *
     * @return TicketResponse containing updated ticket data:
     *        - All fields including unchanged ones
     *        - Reflects all database updates
     *        - NOT cached immediately (next read repopulates)
     *
     * @throws org.example.ticketingapplication.exception.TicketNotFoundException
     *         if ticket with given ID does not exist (404 Not Found)
     * @throws org.example.ticketingapplication.exception.TicketUpdateException
     *         if database update fails (422 Unprocessable Entity)
     *
     * Example:
     * // Update only status
     * UpdateTicketRequest request = UpdateTicketRequest.builder()
     *     .status("IN_PROGRESS")
     *     .priority(null)  // No change to priority
     *     .assignedTo(null) // No change to assignment
     *     .build();
     * TicketResponse updated = ticketService.updateTicket(1L, request);
     * // Result: Only status is changed, priority and assignedTo unchanged
     * // Cache key "ticket:1" is deleted for freshness
     */
    TicketResponse updateTicket(Long id, UpdateTicketRequest request);

    /**
     * Delete a ticket by its ID (Hard Delete).
     *
     * Operation: DELETE
     * HTTP Method: DELETE /api/v1/tickets/{id}
     * Response Status: 204 No Content
     * Transactional: Yes (READ_WRITE)
     * Cached: Cache INVALIDATED after delete
     *
     * Delete Strategy: Hard Delete
     * - Permanently removes ticket from database
     * - No soft delete or archive (consider if audit trail needed)
     * - Record is completely deleted, not marked as inactive
     * - Cannot be recovered unless backup exists
     *
     * Preconditions:
     * - Ticket must exist with given ID
     * - No foreign key constraints prevent deletion
     *
     * Cache Invalidation:
     * - After successful delete, cache key "ticket:{id}" is deleted
     * - Key no longer accessible, next read will fail with 404
     * - Prevents stale data being served after deletion
     *
     * Side Effects:
     * - Removes record from database (permanent)
     * - Deletes Redis cache entry "ticket:{id}"
     * - Logs deletion event for audit trail
     * - Frees up ticket number for potential reuse (depends on policy)
     *
     * Performance:
     * - Time Complexity: O(log n) for database lookup + O(1) for delete + O(1) for cache delete
     * - Database Operation: DELETE FROM ticket WHERE id = {id}
     * - Index used: Primary key index (idx_id)
     *
     * @param id the ticket ID to delete (must be positive Long)
     *           - Must reference existing ticket
     *           - Cannot delete non-existent ticket
     *
     * @throws org.example.ticketingapplication.exception.TicketNotFoundException
     *         if no ticket with given ID exists (404 Not Found)
     * @throws org.example.ticketingapplication.exception.TicketDeletionException
     *         if database delete fails (422 Unprocessable Entity)
     *         - Causes: Foreign key constraint violation, database error
     *
     * Example:
     * // Delete ticket with ID 1
     * ticketService.deleteTicket(1L);
     * // Result: Record deleted, cache cleared, 204 response
     * // Subsequent getTicketById(1L) returns 404 TicketNotFoundException
     */
    void deleteTicket(Long id);

    /**
     * Invalidate all ticket cache entries (Bulk Cache Invalidation).
     *
     * Operation: CACHE INVALIDATION
     * Scope: All tickets (pattern-based invalidation)
     * Transactional: No (cache-only operation)
     * Database Impact: None (cache-only)
     *
     * Purpose:
     * - Invalidate entire ticket cache in one operation
     * - Used after bulk operations (bulk update, bulk import)
     * - Ensures consistency after batch processing
     * - Manual cache clearing when needed
     *
     * Cache Invalidation Pattern:
     * - Uses Redis key pattern: "ticket:*"
     * - Deletes all keys matching pattern
     * - O(n) operation where n = number of cached tickets
     * - Safe for large caches (non-blocking)
     *
     * When to Use:
     * - After bulk ticket import/creation
     * - After bulk ticket updates
     * - After data synchronization from external systems
     * - Manual cache refresh in administration tools
     * - Do NOT use for every single operation (use single-key invalidation instead)
     *
     * Performance:
     * - Time Complexity: O(n) where n = number of cached tickets
     * - Network calls: 1 (pattern scan + delete)
     * - Logging: INFO level with pattern and count
     *
     * Error Handling:
     * - Errors during cache invalidation are logged but not thrown
     * - Service continues even if cache operation fails
     * - Prevents cache issues from breaking business logic
     *
     * Example:
     * // After bulk CSV import
     * bulkTicketService.importTicketsFromCsv(file);
     * ticketService.invalidateAllTicketCache(); // Refresh all cached tickets
     * // Next reads will fetch fresh data from database
     */
    void invalidateAllTicketCache();
}
