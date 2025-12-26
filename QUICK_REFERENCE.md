# QUICK REFERENCE - Ticket Entity & Database

## Project Tech Stack
- **Java:** 17
- **Spring Boot:** 4.0.1
- **Database:** MySQL 8.0+
- **ORM:** JPA/Hibernate
- **Cache:** Redis (Lettuce)
- **Build:** Maven

---

## Database Table Schema

### ticket (Main Table)
```sql
CREATE TABLE ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    customer_id BIGINT,
    assigned_to INT
);
```

**Indexes:** ticket_number, status, priority, customer_id, assigned_to, created_at

---

## Entity Class Location
```
src/main/java/org/example/ticketingapplication/entity/Ticket.java
```

### Ticket Entity Fields
| Field | Type | Nullable | Special |
|-------|------|----------|---------|
| id | Long | NO | @Id, @GeneratedValue |
| ticketNumber | String | NO | UNIQUE |
| status | String | NO | Values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, REOPENED, ON_HOLD |
| priority | String | NO | Values: CRITICAL, HIGH, MEDIUM, LOW |
| createdAt | LocalDateTime | NO | @CreationTimestamp, Auto-set |
| customerId | Long | YES | - |
| assignedTo | Integer | YES | INT column type |

---

## Repository Methods

### Basic CRUD (from JpaRepository)
```java
ticketRepository.save(ticket)              // Create/Update
ticketRepository.findById(id)              // Read by ID
ticketRepository.findAll()                 // Read all
ticketRepository.delete(ticket)            // Delete
ticketRepository.deleteById(id)            // Delete by ID
```

### Custom Finders
```java
findByTicketNumber(String)                 // Find by unique ticket number
findByStatus(String)                       // Find by status
findByPriority(String)                     // Find by priority
findByAssignedTo(Integer)                  // Find by assigned team member
findByCustomerId(Long)                     // Find by customer
```

### Custom Queries
```java
findByStatusAndPriority(String, String)    // Complex filter
findByCreatedAtBetween(LocalDateTime, LocalDateTime)  // Date range
countByStatus(String)                      // Count by status
existsByTicketNumber(String)               // Check existence
```

---

## Validation Rules

### Status Values (Only)
```
OPEN, IN_PROGRESS, RESOLVED, CLOSED, REOPENED, ON_HOLD
```

### Priority Values (Only)
```
CRITICAL, HIGH, MEDIUM, LOW
```

### TicketRequestDto Validation
```java
- ticketNumber: Optional, max 50 chars
- status: Required, must be valid
- priority: Required, must be valid
- customerId: Optional, must be positive
- assignedTo: Optional, must be >= 1
```

---

## Configuration Files

### application.properties
```properties
# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db
spring.datasource.username=root
spring.datasource.password=root

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Redis
spring.redis.host=localhost
spring.redis.port=6379
```

### db-schema.sql
Location: `src/main/resources/db-schema.sql`
- Complete DDL statements
- Index creation
- Sample data (commented)

---

## File Structure

```
src/main/java/org/example/ticketingapplication/
├── entity/
│   └── Ticket.java
├── repository/
│   └── TicketRepository.java
├── dto/
│   ├── TicketRequestDto.java
│   └── TicketResponseDto.java
├── exception/
│   ├── TicketingApplicationException.java
│   ├── TicketNotFoundException.java
│   ├── DuplicateTicketException.java
│   └── InvalidTicketDataException.java
├── validation/
│   └── TicketValidator.java
├── config/
│   ├── DatabaseConfiguration.java
│   └── RedisConfiguration.java
└── TicketingApplication.java

src/main/resources/
├── application.properties
└── db-schema.sql
```

---

## Common Operations

### Create a Ticket
```java
TicketRequestDto request = new TicketRequestDto();
request.setTicketNumber("TKT-001");
request.setStatus("OPEN");
request.setPriority("HIGH");
request.setCustomerId(1L);
request.setAssignedTo(10);

// Validate
validator.validateTicketRequest(request);

// Convert to entity
Ticket ticket = Ticket.builder()
    .ticketNumber(request.getTicketNumber())
    .status(request.getStatus())
    .priority(request.getPriority())
    .customerId(request.getCustomerId())
    .assignedTo(request.getAssignedTo())
    .build();

// Save
ticketRepository.save(ticket);
```

### Find Tickets
```java
// By ticket number
Optional<Ticket> ticket = ticketRepository.findByTicketNumber("TKT-001");

// All open tickets
List<Ticket> open = ticketRepository.findByStatus("OPEN");

// Assigned to specific person
List<Ticket> assigned = ticketRepository.findByAssignedTo(10);

// Critical and open
List<Ticket> critical = ticketRepository
    .findByStatusAndPriority("OPEN", "CRITICAL");
```

### Statistics
```java
long openCount = ticketRepository.countByStatus("OPEN");
boolean exists = ticketRepository.existsByTicketNumber("TKT-001");
```

---

## Important Annotations

### Entity Level
```java
@Entity              // Marks as JPA entity
@Table              // Specifies table name and indexes
@Id                 // Primary key
@GeneratedValue     // Auto-generate ID
@Column             // Column mapping and constraints
@CreationTimestamp  // Auto-set on creation
```

### DTO Level
```java
@NotBlank           // Validation: not empty
@Size               // Validation: string length
@Pattern            // Validation: regex
@Positive           // Validation: positive number
@Min                // Validation: minimum value
@JsonFormat         // JSON serialization format
```

### Config Level
```java
@Configuration      // Spring configuration class
@Repository         // Repository bean
@Component          // Spring-managed component
@Bean               // Manual bean definition
```

---

## Dependencies

### Required Starters (in pom.xml)
```xml
spring-boot-starter-data-jpa      <!-- JPA/Hibernate -->
spring-boot-starter-data-redis    <!-- Redis -->
spring-boot-starter-webmvc        <!-- Web support -->
spring-boot-starter-validation    <!-- Validation -->
mysql-connector-j                 <!-- MySQL driver -->
lombok                           <!-- Code generation -->
```

---

## Error Handling

### Custom Exceptions

```java
// When ticket not found
throw new TicketNotFoundException("TKT-001");

// When ticket number already exists
throw new DuplicateTicketException("TKT-001");

// When data is invalid
throw new InvalidTicketDataException("Status must be valid");
```

### Validation Exceptions

```java
// Spring validation errors (BindingResult)
if (bindingResult.hasErrors()) {
    // Handle @NotBlank, @Pattern, etc.
}

// Custom validation errors
catch (IllegalArgumentException e) {
    // Handle from TicketValidator
}
```

---

## Performance Considerations

### Indexes
All commonly queried fields have indexes:
- `ticket_number` (UNIQUE)
- `status` (for filtering)
- `priority` (for filtering)
- `customer_id` (for filtering)
- `assigned_to` (for filtering)
- `created_at` (for sorting/range queries)

### Query Optimization
- Use custom @Query for complex filters
- Use projections for specific fields
- Use pagination for large result sets (STEP 2)
- Redis caching for frequently accessed tickets

---

## Database Setup Commands

```bash
# Login to MySQL
mysql -u root -p

# Create database
CREATE DATABASE ticketing_db;

# Select database
USE ticketing_db;

# Create table (run db-schema.sql)
source src/main/resources/db-schema.sql;

# Verify
SHOW TABLES;
DESCRIBE ticket;
SHOW INDEX FROM ticket;
```

---

## Build & Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Run tests
mvn test

# Package
mvn package
```

---

## Troubleshooting

### MySQL Connection Error
- Check host, port, username, password in application.properties
- Ensure MySQL server is running
- Create ticketing_db database

### Redis Connection Error
- Check Redis is running on localhost:6379
- Or update spring.redis.host and spring.redis.port

### Table Not Found
- Verify ddl-auto is set to "update" or "create"
- Run db-schema.sql manually

### Validation Error
- Check TicketRequestDto validation annotations
- Verify status/priority are from allowed values
- Check custom validation in TicketValidator

---

**Last Updated:** 2024  
**STEP 1 Status:** ✅ COMPLETE  
**Next:** STEP 2 - Service Layer & Controllers

