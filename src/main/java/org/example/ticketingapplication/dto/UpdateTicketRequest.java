package org.example.ticketingapplication.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Data Transfer Object for updating an existing ticket.
 *
 * Used in PUT /api/v1/tickets/{id} endpoint to accept ticket update requests.
 *
 * Design Pattern: Partial Update (PATCH-like behavior via PUT)
 * - All fields are OPTIONAL (null values are ignored)
 * - Only non-null fields are updated
 * - Null fields preserve existing values
 * - Ticket ID and number cannot be changed (immutable)
 *
 * Validation Rules:
 * - status: Optional, if provided must be 2-50 characters
 *   Valid values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, ON_HOLD
 * - priority: Optional, if provided must be 2-50 characters
 *   Valid values: LOW, MEDIUM, HIGH, CRITICAL
 * - assignedTo: Optional, user ID to reassign the ticket
 *
 * Cache Invalidation:
 * - Any successful update invalidates the Redis cache key: "ticket:{id}"
 * - Next read will fetch fresh data from database and update cache
 *
 * Example Usage:
 * {
 *   "status": "IN_PROGRESS",
 *   "priority": null,
 *   "assignedTo": 5
 * }
 * Result: Only status and assignedTo are updated, priority remains unchanged
 *
 * @author Enterprise Backend Team
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class UpdateTicketRequest {

    /**
     * New status for the ticket (Optional).
     * Only updated if provided (non-null).
     * Valid values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, ON_HOLD
     * Null value means status remains unchanged.
     */
    @Size(min = 2, max = 50, message = "Status must be between 2 and 50 characters")
    private String status;

    /**
     * New priority level for the ticket (Optional).
     * Only updated if provided (non-null).
     * Valid values: LOW, MEDIUM, HIGH, CRITICAL
     * Null value means priority remains unchanged.
     */
    @Size(min = 2, max = 50, message = "Priority must be between 2 and 50 characters")
    private String priority;

    /**
     * User ID to reassign the ticket to (Optional).
     * Only updated if provided (non-null).
     * Null value means assignedTo remains unchanged.
     * Can be used to unassign tickets or reassign to different users.
     */
    private Integer assignedTo;
}
