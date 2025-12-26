-- ================================================
-- DISTRIBUTED TICKETING MANAGEMENT SYSTEM
-- Database Setup Script - MySQL
-- ================================================

-- Create database
CREATE DATABASE IF NOT EXISTS ticketing_db;
USE ticketing_db;

-- Drop existing table if needed (comment out if you want to preserve data)
-- DROP TABLE IF EXISTS ticket;

-- Create ticket table
CREATE TABLE IF NOT EXISTS ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique ticket identifier',
    ticket_number VARCHAR(50) NOT NULL UNIQUE COMMENT 'Unique external ticket reference',
    status VARCHAR(20) NOT NULL COMMENT 'Status: OPEN, IN_PROGRESS, RESOLVED, CLOSED',
    priority VARCHAR(20) NOT NULL COMMENT 'Priority: CRITICAL, HIGH, MEDIUM, LOW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Ticket creation timestamp',
    customer_id BIGINT COMMENT 'Customer ID',
    assigned_to INT COMMENT 'Assigned team member ID',

    -- Indexes for optimization
    INDEX idx_ticket_number (ticket_number),
    INDEX idx_ticket_status (status),
    INDEX idx_ticket_priority (priority),
    INDEX idx_ticket_customer_id (customer_id),
    INDEX idx_ticket_assigned_to (assigned_to),
    INDEX idx_ticket_created_at (created_at)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- SAMPLE DATA (Optional - for testing)
-- ================================================

-- Insert sample tickets
INSERT INTO ticket (ticket_number, status, priority, customer_id, assigned_to)
VALUES ('TKT-2024-001', 'OPEN', 'HIGH', 1, 10);

INSERT INTO ticket (ticket_number, status, priority, customer_id, assigned_to)
VALUES ('TKT-2024-002', 'IN_PROGRESS', 'CRITICAL', 2, 11);

INSERT INTO ticket (ticket_number, status, priority, customer_id, assigned_to)
VALUES ('TKT-2024-003', 'RESOLVED', 'MEDIUM', 3, 12);

INSERT INTO ticket (ticket_number, status, priority, customer_id, assigned_to)
VALUES ('TKT-2024-004', 'CLOSED', 'LOW', 4, 10);

-- ================================================
-- VERIFICATION QUERIES
-- ================================================

-- Verify table structure
DESCRIBE ticket;

-- Verify data
SELECT * FROM ticket;

-- Show table size and indexes
SHOW CREATE TABLE ticket;
SHOW INDEX FROM ticket;

-- Count records
SELECT COUNT(*) as total_tickets FROM ticket;
SELECT status, COUNT(*) as count FROM ticket GROUP BY status;

