package org.example.ticketingapplication.repository;

import org.example.ticketingapplication.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TicketRepository - Data Access Layer for Ticket Entity
 *
 * Provides CRUD operations and custom query methods for the Ticket entity.
 * Uses Spring Data JPA for automatic implementation of repository pattern.
 *
 * Design Pattern:
 * - Spring Data JPA Repository Pattern
 * - Automatic implementation by Spring
 * - No manual SQL queries (safe from SQL injection)
 * - Method naming follows Spring Data conventions
 *
 * Method Naming Convention:
 * - findBy{Field}: Returns Optional (single result) or List (multiple results)
 * - findBy{Field}(Pageable): Returns Page for pagination support
 * - Custom @Query: For complex queries or performance optimization
 *
 * Performance Considerations:
 * - Indexes defined in Ticket entity for query optimization:
 *   • idx_ticket_number: For findByTicketNumber()
 *   • idx_status: For findByStatus()
 *   • idx_customer_id: For findByCustomerId()
 *   • idx_assigned_to: For findByAssignedTo()
 *   • idx_created_at: For sorting and time-range queries
 *
 * Transaction Handling:
 * - All methods are transactional by default
 * - Reads are read-only by default
 * - Writes (save, delete) are read-write by default
 *
 * Caching Strategy:
 * - Cache is managed at service layer (TicketService)
 * - Repository layer remains cache-agnostic
 * - Cache key format: "ticket:{id}"
 *
 * Usage Examples:
 * // Find ticket by ID (inherited from JpaRepository)
 * Optional<Ticket> ticket = ticketRepository.findById(1L);
 *
 * // Find ticket by ticket number (unique)
 * Optional<Ticket> ticket = ticketRepository.findByTicketNumber("TKT-001");
 *
 * // Find all tickets with specific status (pagination)
 * Page<Ticket> tickets = ticketRepository.findByStatus("OPEN", pageable);
 *
 * // Find tickets assigned to user (with pagination)
 * Page<Ticket> tickets = ticketRepository.findByAssignedTo(5, pageable);
 *
 * // Find all tickets for customer
 * List<Ticket> tickets = ticketRepository.findByCustomerId(10L);
 *
 * @author Enterprise Backend Team
 * @version 1.0
 * @see Ticket
 * @see org.example.ticketingapplication.service.TicketService
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Find a ticket by its unique ticket number.
     *
     * Query Method: Spring Data JPA automatically generates the SQL query.
     * Generated SQL: SELECT * FROM ticket WHERE ticket_number = ?
     * Index Used: idx_ticket_number (unique index for O(log n) lookup)
     * Performance: O(log n) - logarithmic time due to unique index
     *
     * @param ticketNumber the unique ticket number (e.g., "TKT-001")
     * @return Optional containing the Ticket if found, empty Optional if not found
     * @throws DataAccessException if database error occurs
     *
     * Usage:
     * Optional<Ticket> ticket = ticketRepository.findByTicketNumber("TKT-001");
     * if (ticket.isPresent()) {
     *     Ticket ticketData = ticket.get();
     *     // Process ticket
     * }
     *
     * Used By:
     * - TicketServiceImpl.createTicket() - Check for duplicates
     * - BulkTicketService - Process CSV records
     */
    Optional<Ticket> findByTicketNumber(String ticketNumber);

    /**
     * Find all tickets with a specific status.
     *
     * Query Method: Spring Data JPA automatically generates the SQL query.
     * Generated SQL: SELECT * FROM ticket WHERE status = ? ORDER BY created_at DESC
     * Index Used: idx_status (for fast filtering)
     * Performance: O(n) where n is number of tickets with that status
     * Pagination: Supported via Pageable parameter for large result sets
     *
     * @param status the ticket status (e.g., "OPEN", "IN_PROGRESS", "RESOLVED")
     * @param pageable pagination information (page number, size, sort)
     * @return Page containing matching tickets
     * @throws DataAccessException if database error occurs
     *
     * Pagination Example:
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
     * Page<Ticket> openTickets = ticketRepository.findByStatus("OPEN", pageable);
     *
     * Usage:
     * - Listing tickets by status with pagination
     * - Dashboard queries for different ticket statuses
     * - SLA-based filtering
     */
    Page<Ticket> findByStatus(String status, Pageable pageable);

    /**
     * Find all tickets assigned to a specific user.
     *
     * Query Method: Spring Data JPA automatically generates the SQL query.
     * Generated SQL: SELECT * FROM ticket WHERE assigned_to = ? ORDER BY created_at DESC
     * Index Used: idx_assigned_to (for fast filtering)
     * Performance: O(n) where n is number of tickets assigned to that user
     * Pagination: Supported via Pageable parameter
     *
     * @param assignedTo the user ID who the ticket is assigned to
     * @param pageable pagination information (page number, size, sort)
     * @return Page containing tickets assigned to the user
     * @throws DataAccessException if database error occurs
     *
     * Usage:
     * - Agent workload dashboard
     * - User-specific ticket lists
     * - Workload distribution queries
     */
    Page<Ticket> findByAssignedTo(Integer assignedTo, Pageable pageable);

    /**
     * Find all tickets created by a specific customer.
     *
     * Query Method: Spring Data JPA automatically generates the SQL query.
     * Generated SQL: SELECT * FROM ticket WHERE customer_id = ? ORDER BY created_at DESC
     * Index Used: idx_customer_id (for fast filtering)
     * Performance: O(n) where n is number of tickets from that customer
     *
     * Note: Returns List instead of Page because customer-specific queries
     * typically need all records for processing/export.
     *
     * @param customerId the customer ID who created the tickets
     * @return List of all tickets created by the customer
     * @throws DataAccessException if database error occurs
     *
     * Usage:
     * - Customer portal - show all their tickets
     * - Customer support history
     * - Bulk export for specific customer
     */
    List<Ticket> findByCustomerId(Long customerId);

    /**
     * Find all unassigned tickets with pagination.
     *
     * Custom Query: Uses @Query for null-safe comparison.
     * Query: SELECT t FROM Ticket t WHERE t.assignedTo IS NULL
     * Performance: O(n) - scans all tickets or uses index on assigned_to
     * Pagination: Supported via Pageable parameter
     *
     * Note: Native SQL null check (IS NULL) requires custom @Query
     * because Spring Data method naming doesn't handle nulls well.
     *
     * @param pageable pagination information
     * @return Page containing unassigned tickets
     * @throws DataAccessException if database error occurs
     *
     * Usage:
     * - Find tickets available for assignment
     * - Load balancing/workload distribution queries
     * - Ticket assignment dashboard
     */
    @Query("SELECT t FROM Ticket t WHERE t.assignedTo IS NULL")
    Page<Ticket> findUnassignedTickets(Pageable pageable);

    /**
     * Find all unassigned tickets without pagination.
     *
     * Custom Query: Finds all tickets with no assigned user.
     * Performance: O(n) - full table scan or index usage
     *
     * Note: Use for bulk operations. For UI pagination, use findUnassignedTickets(Pageable)
     *
     * @return List of all unassigned tickets
     * @throws DataAccessException if database error occurs
     *
     * Usage:
     * - Bulk assignment operations
     * - Automatic load balancing
     * - Ticket distribution algorithms
     */
    @Query("SELECT t FROM Ticket t WHERE t.assignedTo IS NULL")
    List<Ticket> findAllUnassignedTickets();

    /**
     * Count tickets with a specific status.
     *
     * Query Method: Spring Data JPA automatically generates the SQL query.
     * Generated SQL: SELECT COUNT(*) FROM ticket WHERE status = ?
     * Performance: O(1) - typically uses index scan
     *
     * @param status the ticket status to count
     * @return number of tickets with the given status
     * @throws DataAccessException if database error occurs
     *
     * Usage:
     * - Dashboard metrics and statistics
     * - SLA tracking
     * - Service level monitoring
     */
    long countByStatus(String status);

    /**
     * Count unassigned tickets.
     *
     * Custom Query: Counts tickets with null assigned_to value.
     * Performance: O(1) - uses index
     *
     * @return number of unassigned tickets
     * @throws DataAccessException if database error occurs
     *
     * Usage:
     * - Queue depth monitoring
     * - Load balancing metrics
     * - Performance metrics
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo IS NULL")
    long countUnassignedTickets();
}
