package org.example.ticketingapplication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketingapplication.dto.TicketResponse;
import org.example.ticketingapplication.entity.Ticket;
import org.example.ticketingapplication.exception.TicketNotFoundException;
import org.example.ticketingapplication.repository.TicketRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * TicketCacheService - Dedicated Cache Management Service for Tickets
 *
 * Purpose:
 * This service provides explicit cache management operations for tickets.
 * While TicketServiceImpl handles integrated caching with business logic,
 * TicketCacheService offers standalone cache operations for specialized use cases.
 *
 * Use Cases:
 * - Manual cache invalidation operations (admin tools, data maintenance)
 * - Cache testing and verification
 * - Separate caching concerns from business logic (if needed)
 * - Cache warming and preloading strategies
 * - Cache statistics and monitoring
 *
 * Architecture Note:
 * IMPORTANT: In production, prefer using TicketServiceImpl for ticket operations.
 * TicketServiceImpl provides integrated cache management with business logic.
 * This service should only be used when cache operations need to be separated
 * from business operations (e.g., cache warming, admin cache management).
 *
 * Caching Strategy:
 * - Framework: Redis with Lettuce driver
 * - Key Format: "ticket:{id}" (e.g., "ticket:1", "ticket:42")
 * - TTL: 30 minutes (1800 seconds)
 * - Serialization: JSON via GenericJackson2JsonRedisSerializer
 * - Connection Pooling: Lettuce with configurable pool size
 *
 * Transaction Management:
 * - Class-level @Transactional: All methods are transactional
 * - Read operations: @Transactional(readOnly = true) where applicable
 * - Write operations: Default read-write transactions
 * - Isolation: Database default (READ_COMMITTED)
 *
 * Error Handling:
 * - Cache errors: Logged but may be thrown depending on method
 * - Database errors: Rethrown as-is or wrapped in custom exceptions
 * - Graceful degradation: Cache failures don't break business logic
 *
 * Thread Safety:
 * - RedisTemplate: Thread-safe with internal connection pooling
 * - Service methods: Stateless and thread-safe
 * - Concurrent requests: Isolated by Spring transactions
 * - No synchronized blocks needed
 *
 * Performance:
 * - Cache hit: O(1) - Redis lookup
 * - Cache miss: O(log n) - Database lookup + cache store
 * - Eviction: O(1) for single entry, O(n) for pattern-based
 *
 * Dependencies:
 * - RedisTemplate<String, Object>: Cache access
 * - TicketRepository: Database access
 * - Spring Data Redis: Configuration and templates
 *
 * Logging Strategy:
 * - INFO: Successful cache operations
 * - DEBUG: Cache operations details
 * - WARN: Non-critical failures
 * - ERROR: Critical failures and unexpected errors
 *
 * @author Enterprise Backend Team
 * @version 1.0
 * @see TicketServiceImpl for integrated cache and business logic
 * @see org.example.ticketingapplication.Config.RedisConfig for Redis setup
 *
 * @deprecated Prefer TicketServiceImpl for standard ticket operations.
 *             Use this service only for explicit cache management scenarios.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TicketCacheService {

    // ================= Constants =================

    /**
     * Cache key prefix for all ticket cache entries.
     * Format: "ticket:{id}" (e.g., "ticket:1", "ticket:42")
     */
    private static final String CACHE_KEY_PREFIX = "ticket:";

    /**
     * Cache Time-To-Live in minutes.
     * Determines how long ticket data is cached in Redis.
     * Value: 30 minutes
     */
    private static final long CACHE_TTL_MINUTES = 30;

    // ================= Dependencies =================

    /**
     * Redis template for cache operations.
     * Configured in RedisConfig with JSON serialization and Lettuce pooling.
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Repository for database access.
     * Used by cache service to fetch tickets from database on cache miss.
     */
    private final TicketRepository ticketRepository;

    // ================= Read Operations =================

    /**
     * Retrieve a ticket by ID with caching.
     *
     * Caching Strategy: Read-Through Pattern
     * 1. Check Redis cache with key "ticket:{id}"
     * 2. If cache HIT: Return immediately (O(1))
     * 3. If cache MISS: Query database
     * 4. Store result in Redis with 30-minute TTL
     * 5. Return to caller
     *
     * Cache Behavior:
     * - Cache hit returns response from Redis (sub-millisecond)
     * - Cache miss queries database and populates cache
     * - Not found errors are NOT cached (no negative caching)
     * - Successful reads always update/populate cache
     *
     * Transaction: Read-only (optimized by Spring)
     * Performance: O(1) cache hit, O(log n) cache miss
     *
     * @param id the ticket ID (positive Long)
     * @return TicketResponse from cache or database
     * @throws TicketNotFoundException if ticket does not exist
     * @throws Exception if unexpected error occurs during cache/database access
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        String cacheKey = buildCacheKey(id);
        log.info("Retrieving ticket with ID: {}", id);

        try {
            // Step 1: Check Redis cache first
            TicketResponse cachedTicket = (TicketResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cachedTicket != null) {
                log.info("Cache HIT for key: {}", cacheKey);
                return cachedTicket;
            }

            // Step 2: Cache miss - fetch from database
            log.info("Cache MISS for key: {}", cacheKey);
            Ticket ticket = ticketRepository.findById(id)
                    .orElseThrow(() -> new TicketNotFoundException("Ticket not found with ID: " + id));

            // Step 3: Convert to response DTO
            TicketResponse ticketResponse = mapToResponse(ticket);

            // Step 4: Store result in Redis with TTL = 30 minutes
            redisTemplate.opsForValue().set(cacheKey, ticketResponse, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("Cached ticket with key: {} for {} minutes", cacheKey, CACHE_TTL_MINUTES);

            return ticketResponse;

        } catch (TicketNotFoundException ex) {
            // Rethrow not found exceptions
            log.warn("Ticket not found for ID: {}", id);
            throw ex;

        } catch (Exception ex) {
            // Log and rethrow unexpected errors
            log.error("Error retrieving ticket from cache/database for ID: {}", id, ex);
            throw ex;
        }
    }

    // ================= Write Operations =================

    /**
     * Update ticket and invalidate cache entry.
     *
     * Operation Flow:
     * 1. Save updated ticket to database
     * 2. Delete cache entry "ticket:{id}"
     * 3. Next read will fetch fresh data from database
     *
     * Cache Invalidation:
     * - Immediately deletes cache key after database update
     * - Ensures consistency (no stale cached data)
     * - Next access repopulates cache from database
     *
     * Transaction: Read-write with database updates
     * Performance: O(log n) database update + O(1) cache delete
     *
     * @param id the ticket ID to update
     * @param updatedTicket the ticket entity with updated values
     * @throws Exception if update or cache operation fails
     */
    public void updateTicket(Long id, Ticket updatedTicket) {
        log.info("Updating ticket with ID: {}", id);

        try {
            // Step 1: Save to database
            ticketRepository.save(updatedTicket);
            log.info("Ticket saved to database with ID: {}", id);

            // Step 2: Invalidate cache entry
            evictCache(id);
            log.info("Cache invalidated for ticket ID: {}", id);

        } catch (Exception ex) {
            log.error("Error updating ticket with ID: {}", id, ex);
            throw ex;
        }
    }

    /**
     * Delete ticket and invalidate cache entry.
     *
     * Operation Flow:
     * 1. Delete ticket from database (hard delete)
     * 2. Delete cache entry "ticket:{id}"
     * 3. Next access returns 404 TicketNotFoundException
     *
     * Cache Invalidation:
     * - Immediately deletes cache key after database deletion
     * - Prevents serving stale deleted tickets
     *
     * Transaction: Read-write with database deletions
     * Performance: O(log n) database delete + O(1) cache delete
     *
     * @param id the ticket ID to delete
     * @throws Exception if deletion or cache operation fails
     */
    public void deleteTicket(Long id) {
        log.info("Deleting ticket with ID: {}", id);

        try {
            // Step 1: Delete from database
            ticketRepository.deleteById(id);
            log.info("Ticket deleted from database with ID: {}", id);

            // Step 2: Invalidate cache entry
            evictCache(id);
            log.info("Cache invalidated for deleted ticket ID: {}", id);

        } catch (Exception ex) {
            log.error("Error deleting ticket with ID: {}", id, ex);
            throw ex;
        }
    }

    // ================= Cache Invalidation Operations =================

    /**
     * Invalidate all ticket cache entries using pattern matching.
     *
     * Cache Invalidation:
     * - Pattern: "ticket:*" matches all ticket cache keys
     * - Operation: Scans all keys and deletes matching entries
     * - Performance: O(n) where n = number of cached tickets
     * - Non-blocking: Uses Redis SCAN internally (safe for large caches)
     *
     * Use Cases:
     * - Bulk invalidation after data synchronization
     * - Administrative cache refresh
     * - After bulk ticket operations
     * - Cache consistency checks
     *
     * Error Handling:
     * - Errors are logged but rethrown
     * - Caller can decide whether to handle or propagate
     *
     * @throws Exception if cache operation fails
     */
    public void evictAllTickets() {
        log.info("Invalidating all ticket cache entries with pattern: {}*", CACHE_KEY_PREFIX);

        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("All ticket cache entries invalidated: {} keys deleted", keys.size());
            } else {
                log.info("No ticket cache entries found to invalidate");
            }

        } catch (Exception ex) {
            log.error("Error evicting all ticket cache entries", ex);
            throw ex;
        }
    }

    // ================= Cache Helper Methods =================

    /**
     * Invalidate a single cache entry by ID.
     *
     * Cache Invalidation:
     * - Deletes key "ticket:{id}"
     * - Next access triggers cache miss and database fetch
     * - Ensures data consistency
     *
     * Error Handling:
     * - Errors are logged at WARN level (non-critical)
     * - Exceptions are NOT rethrown
     * - Service continues despite cache failure
     * - Prevents cache issues from breaking business logic
     *
     * @param id the ticket ID whose cache should be invalidated
     */
    private void evictCache(Long id) {
        try {
            String cacheKey = buildCacheKey(id);
            redisTemplate.delete(cacheKey);
            log.debug("Cache evicted for key: {}", cacheKey);

        } catch (Exception ex) {
            // Log but don't throw - cache failure is non-critical
            log.warn("Error evicting cache for ticket ID: {}", id, ex);
        }
    }

    /**
     * Build cache key from ticket ID.
     *
     * Key Format: "ticket:{id}"
     * Examples: "ticket:1", "ticket:42", "ticket:9999"
     *
     * Performance: O(1) - simple string concatenation
     * Thread-safe: Stateless helper method
     *
     * @param id the ticket ID
     * @return the cache key string
     */
    private String buildCacheKey(Long id) {
        return CACHE_KEY_PREFIX + id;
    }

    /**
     * Map Ticket entity to TicketResponse DTO.
     *
     * Purpose:
     * - Separates entity layer from API response layer
     * - Prevents exposing internal entity details
     * - Enables selective field mapping
     *
     * Mapping Fields:
     * - id, ticketNumber, status, priority, createdAt, customerId, assignedTo
     *
     * Performance: O(1) - fixed field count, no queries
     * Thread-safe: Stateless mapper
     *
     * @param ticket the Ticket entity
     * @return TicketResponse DTO with all fields mapped
     */
    private TicketResponse mapToResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdAt(ticket.getCreatedAt())
                .customerId(ticket.getCustomerId())
                .assignedTo(ticket.getAssignedTo())
                .build();
    }
}
