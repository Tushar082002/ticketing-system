# Distributed Ticketing Management System - STEP 1 Implementation

## Overview
This document describes the implementation of STEP 1: ENTITY + DATABASE for the Distributed Ticketing Management System.

---

## Project Structure

```
src/main/java/org/example/ticketingapplication/
├── entity/
│   └── Ticket.java              # JPA Entity mapped to 'ticket' table
├── repository/
│   └── TicketRepository.java    # Spring Data JPA Repository
├── dto/
│   ├── TicketRequestDto.java    # Request DTO with validation
│   └── TicketResponseDto.java   # Response DTO
├── exception/
│   ├── TicketingApplicationException.java    # Base custom exception
│   ├── TicketNotFoundException.java           # Not found exception
│   ├── DuplicateTicketException.java          # Duplicate ticket exception
│   └── InvalidTicketDataException.java        # Invalid data exception
├── validation/
│   └── TicketValidator.java                  # Business logic validation
├── config/
│   ├── DatabaseConfiguration.java            # JPA/Hibernate config
│   └── RedisConfiguration.java               # Redis/Lettuce config
└── TicketingApplication.java    # Spring Boot main class

src/main/resources/
├── application.properties        # Configuration file
└── db-schema.sql               # Database schema script
```

---

## 1. TICKET ENTITY

### File: `entity/Ticket.java`

#### Entity Mapping Details:

| Field | Type | JPA Annotation | Database Column | Constraints |
|-------|------|---|---|---|
| id | Long | @Id @GeneratedValue(IDENTITY) | id | PRIMARY KEY, AUTO_INCREMENT |
| ticketNumber | String | @Column(unique=true) | ticket_number | UNIQUE, NOT NULL |
| status | String | @Column | status | NOT NULL, VARCHAR(20) |
| priority | String | @Column | priority | NOT NULL, VARCHAR(20) |
| createdAt | LocalDateTime | @CreationTimestamp | created_at | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| customerId | Long | @Column | customer_id | Optional |
| assignedTo | Integer | @Column(columnDefinition="INT") | assigned_to | Optional, INT type |

#### Key Features:

**1. Comprehensive Indexing:**
```java
@Table(indexes = {
    @Index(name = "idx_ticket_number", columnList = "ticket_number", unique = true),
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_priority", columnList = "priority"),
    @Index(name = "idx_ticket_customer_id", columnList = "customer_id"),
    @Index(name = "idx_ticket_assigned_to", columnList = "assigned_to"),
    @Index(name = "idx_ticket_created_at", columnList = "created_at")
})
```

**Benefits:**
- Optimized query performance for common queries
- Support for complex filtered searches
- Fast lookups by ticket number, status, priority
- Efficient analytics queries on timestamps

**2. Lombok Annotations:**
- `@Data`: Auto-generates getters, setters, equals, hashCode, toString
- `@NoArgsConstructor`: No-argument constructor
- `@AllArgsConstructor`: Constructor with all fields
- `@Builder`: Builder pattern support for object creation

**3. Creation Timestamp:**
```java
@CreationTimestamp
@Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
private LocalDateTime createdAt;
```
- Automatically set on entity creation
- Immutable field (updatable=false)
- Used for audit trails and sorting

**4. Status & Priority Enums (as Strings):**
```
Status: OPEN, IN_PROGRESS, RESOLVED, CLOSED, REOPENED, ON_HOLD
Priority: CRITICAL, HIGH, MEDIUM, LOW
```

---

## 2. TICKET REPOSITORY

### File: `repository/TicketRepository.java`

Spring Data JPA repository with custom query methods:

#### Standard Finder Methods:
```java
Optional<Ticket> findByTicketNumber(String ticketNumber)
List<Ticket> findByStatus(String status)
List<Ticket> findByPriority(String priority)
List<Ticket> findByAssignedTo(Integer assignedTo)
List<Ticket> findByCustomerId(Long customerId)
```

#### Custom Query Methods:
```java
@Query("SELECT t FROM Ticket t WHERE t.status = :status AND t.priority = :priority")
List<Ticket> findByStatusAndPriority(String status, String priority)

@Query("SELECT t FROM Ticket t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
List<Ticket> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate)
```

#### Utility Methods:
```java
long countByStatus(String status)          // For analytics
boolean existsByTicketNumber(String number) // For validation
```

---

## 3. DATA TRANSFER OBJECTS (DTOs)

### 3.1 TicketRequestDto

Used for **creating and updating** tickets with validation:

```java
- ticketNumber: String (max 50 chars) [Optional]
- status: String [Required] - Pattern: OPEN|IN_PROGRESS|RESOLVED|CLOSED|REOPENED|ON_HOLD
- priority: String [Required] - Pattern: CRITICAL|HIGH|MEDIUM|LOW
- customerId: Long [Optional] - Must be positive
- assignedTo: Integer [Optional] - Must be >= 1
```

**Validation Annotations Used:**
- `@NotBlank`: Ensures field is not empty
- `@Size`: Validates string length
- `@Pattern`: Regex validation for status/priority
- `@Positive`: Ensures positive numbers
- `@Min`: Validates minimum value

### 3.2 TicketResponseDto

Used for **API responses** with JSON formatting:

```java
- id: Long
- ticketNumber: String
- status: String
- priority: String
- createdAt: LocalDateTime (@JsonFormat: ISO 8601)
- customerId: Long
- assignedTo: Integer
```

**Benefits:**
- Clean separation from entity
- Independent API contracts
- Easy to extend without affecting entities
- JSON serialization control

---

## 4. EXCEPTION HANDLING

### Exception Hierarchy:
```
TicketingApplicationException (Base)
├── TicketNotFoundException
├── DuplicateTicketException
└── InvalidTicketDataException
```

### Exception Usage Examples:

```java
// Not Found
throw new TicketNotFoundException("TKT-12345");

// Duplicate
throw new DuplicateTicketException("TKT-67890");

// Invalid Data
throw new InvalidTicketDataException("Status must be valid");
```

---

## 5. VALIDATION LAYER

### File: `validation/TicketValidator.java`

Provides **business logic validation** beyond annotations:

```java
validateTicketRequest(TicketRequestDto)  // Comprehensive validation
validateStatus(String)                   // Status enum validation
validatePriority(String)                 // Priority enum validation
validateAssignedTo(Integer)              // Assigned to validation
```

**Separation of Concerns:**
- Spring validation annotations: Input format/constraints
- TicketValidator: Business rules and domain logic

---

## 6. CONFIGURATION

### 6.1 DatabaseConfiguration

```java
@EnableJpaRepositories(basePackages = "org.example.ticketingapplication.repository")
@EnableTransactionManagement
```

- Enables Spring Data JPA repositories
- Enables @Transactional annotation support
- Auto-creates proxy beans for repository interfaces

### 6.2 RedisConfiguration

**Lettuce Client Setup:**
```java
- Socket timeout: 5 seconds
- Connection timeout: 5 seconds
- Command timeout: 10 seconds
- Keep-alive enabled
- Connection pool: Max 20, Min 5, Idle 10
```

**Redis Templates:**
1. **StringRedisTemplate** - String key/value operations
2. **JsonRedisTemplate** - Object serialization with Jackson

---

## 7. DATABASE CONFIGURATION

### File: `application.properties`

```properties
# MySQL Connection
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=root

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Redis (Lettuce)
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.lettuce.pool.max-active=20
```

### Database Schema (`db-schema.sql`)

```sql
CREATE TABLE ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    customer_id BIGINT,
    assigned_to INT,
    
    INDEX idx_ticket_number (ticket_number),
    INDEX idx_ticket_status (status),
    INDEX idx_ticket_priority (priority),
    INDEX idx_ticket_customer_id (customer_id),
    INDEX idx_ticket_assigned_to (assigned_to),
    INDEX idx_ticket_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 8. DEPENDENCIES ADDED

```xml
<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Already included in pom.xml -->
- spring-boot-starter-data-jpa (Hibernate)
- spring-boot-starter-data-redis (Lettuce)
- spring-boot-starter-webmvc
- mysql-connector-j
- lombok
```

---

## 9. CLEAN ARCHITECTURE LAYERS

```
┌─────────────────────────────────────────┐
│        API CONTROLLER LAYER             │ (Next: STEP 2)
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│        SERVICE LAYER                    │ (Next: STEP 2)
│   (Business Logic, Validation)          │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│        REPOSITORY LAYER                 │ ✅ COMPLETED
│   (Data Access, JPA)                    │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│        ENTITY LAYER                     │ ✅ COMPLETED
│   (Persistence Mapping)                 │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│        DATABASE LAYER                   │ ✅ COMPLETED
│   (MySQL with Indexes)                  │
└─────────────────────────────────────────┘
```

**Cross-Cutting Concerns:**
- Exception Handling ✅ COMPLETED
- Validation (Annotations + Business) ✅ COMPLETED
- Configuration (JPA, Redis, Database) ✅ COMPLETED
- DTOs (Request/Response) ✅ COMPLETED

---

## 10. USAGE EXAMPLES

### Creating a Ticket Entity:

```java
Ticket ticket = Ticket.builder()
    .ticketNumber("TKT-2024-001")
    .status("OPEN")
    .priority("HIGH")
    .customerId(1L)
    .assignedTo(10)
    .build();

// createdAt is automatically set by @CreationTimestamp
```

### Repository Operations:

```java
// Find by ticket number
Optional<Ticket> ticket = ticketRepository.findByTicketNumber("TKT-2024-001");

// Find by status
List<Ticket> openTickets = ticketRepository.findByStatus("OPEN");

// Find by status and priority
List<Ticket> critical = ticketRepository.findByStatusAndPriority("OPEN", "CRITICAL");

// Count tickets
long openCount = ticketRepository.countByStatus("OPEN");
```

### Validation Usage:

```java
try {
    validator.validateTicketRequest(requestDto);
    // Process ticket
} catch (IllegalArgumentException e) {
    // Handle validation error
}
```

---

## 11. TESTING SETUP

Database setup for testing:

```sql
-- Execute db-schema.sql in MySQL
mysql -u root -p < src/main/resources/db-schema.sql

-- Or manually:
CREATE DATABASE ticketing_db;
```

Connect to MySQL:
```bash
mysql -u root -p
```

---

## 12. NEXT STEPS (STEP 2)

Following clean architecture, the next step will include:

1. **Service Layer** - Business logic and orchestration
   - TicketService with CRUD operations
   - Redis caching integration
   - Transaction management

2. **Controller Layer** - REST API endpoints
   - TicketController with @RestController
   - Request/Response mapping
   - Exception handler advice

3. **Global Exception Handler**
   - @ControllerAdvice for centralized error handling
   - HTTP status code mapping

---

## 13. ENTERPRISE-LEVEL PRACTICES IMPLEMENTED

✅ **Separation of Concerns** - Clean layered architecture
✅ **Dependency Injection** - Spring component scanning
✅ **Configuration Management** - Externalized properties
✅ **Data Validation** - Annotations + Custom validation
✅ **Exception Handling** - Hierarchy and custom exceptions
✅ **Database Indexing** - Optimized query performance
✅ **Entity Design** - Proper JPA annotations and constraints
✅ **DTOs** - Request/Response separation
✅ **Lombok** - Reduced boilerplate code
✅ **Transaction Management** - @EnableTransactionManagement
✅ **Documentation** - Javadoc and comments

---

## 14. CONFIGURATION CHECKLIST

Before running the application:

- [ ] Create MySQL database: `CREATE DATABASE ticketing_db;`
- [ ] Update `application.properties` with your MySQL credentials
- [ ] Start Redis server on localhost:6379 (or configure in properties)
- [ ] Run: `mvn clean install`
- [ ] Run: `mvn spring-boot:run`

---

**Author:** Senior Backend Engineer  
**Date:** 2024  
**Version:** 1.0.0 STEP 1 - Entity & Database

