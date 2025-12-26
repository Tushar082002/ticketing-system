-- ============================================================================
-- Distributed Ticketing Management System - Database Schema
-- MySQL 8.0+
-- ============================================================================

-- Create Database
CREATE DATABASE IF NOT EXISTS ticketing_db;
USE ticketing_db;

-- ============================================================================
-- TICKET TABLE - Core entity for the ticketing system
-- ============================================================================
CREATE TABLE IF NOT EXISTS ticket (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique ticket identifier',
    ticket_number VARCHAR(50) NOT NULL UNIQUE COMMENT 'Human-readable ticket number (e.g., TICKET-2025-001)',
    status VARCHAR(50) NOT NULL COMMENT 'Ticket status: OPEN, IN_PROGRESS, CLOSED, ON_HOLD',
    priority VARCHAR(50) NOT NULL COMMENT 'Priority level: LOW, MEDIUM, HIGH, CRITICAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when ticket was created',
    customer_id BIGINT COMMENT 'ID of the customer who reported the ticket',
    assigned_to INT COMMENT 'User ID of the person assigned to this ticket',

    -- Indexes for query optimization
    INDEX idx_ticket_number (ticket_number),
    INDEX idx_status (status),
    INDEX idx_customer_id (customer_id),
    INDEX idx_assigned_to (assigned_to),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Core ticket entity for the ticketing system';

-- ============================================================================
-- Sample Data (Optional - for testing)
-- ============================================================================
-- INSERT INTO ticket (ticket_number, status, priority, customer_id, assigned_to)
-- VALUES
--     ('TICKET-2025-001', 'OPEN', 'HIGH', 101, 1),
--     ('TICKET-2025-002', 'IN_PROGRESS', 'MEDIUM', 102, 2),
--     ('TICKET-2025-003', 'CLOSED', 'LOW', 103, 1);

