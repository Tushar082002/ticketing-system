# Distributed Ticketing Management System

A production-ready Spring Boot REST API for managing support tickets with complete CRUD operations.

## Technology Stack

- **Java:** 17
- **Spring Boot:** 4.0.1
- **Framework:** Spring MVC + Spring Data JPA
- **Database:** MySQL 8.0+
- **ORM:** Hibernate/JPA
- **Build Tool:** Maven
- **Additional:** Lombok, Validation API

## Project Structure

```
src/main/java/org/example/ticketingapplication/
├── entity/
│   └── Ticket.java                    # JPA Entity
├── repository/
│   └── TicketRepository.java          # Data Access Layer
├── service/
│   ├── TicketService.java             # Service Interface
│   └── TicketServiceImpl.java          # Service Implementation
├── controller/
│   └── TicketController.java          # REST Controller
├── dto/
│   ├── CreateTicketRequest.java       # Create request DTO
│   ├── UpdateTicketRequest.java       # Update request DTO
│   └── TicketResponse.java            # Response DTO
├── exception/
│   ├── TicketNotFoundException.java    # Custom exception
│   ├── DuplicateTicketException.java   # Custom exception
│   ├── GlobalExceptionHandler.java     # Global exception handler
│   ├── ErrorResponse.java             # Error response DTO
│   └── ValidationErrorResponse.java   # Validation error DTO
└── TicketingApplication.java          # Main Spring Boot class
```

## Database Schema

### Ticket Table

```sql
CREATE TABLE ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
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

## Setup Instructions

### 1. Create Database

```bash
mysql -u root -p
CREATE DATABASE ticketing_db;
USE ticketing_db;
```

### 2. Update Configuration

Edit `src/main/resources/application.properties`:

```properties
# Database credentials
spring.datasource.username=root
spring.datasource.password=your_password

# Server port (default: 8080)
server.port=8080
```

### 3. Build & Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## REST API Endpoints

### Base URL
```
http://localhost:8080/api/tickets
```

### 1. Create Ticket
**Endpoint:** `POST /api/tickets`

**Status Code:** `201 Created`

**Request Body:**
```json
{
  "ticketNumber": "TKT-001",
  "status": "OPEN",
  "priority": "HIGH",
  "customerId": 1,
  "assignedTo": 10
}
```

**Response:**
```json
{
  "id": 1,
  "ticketNumber": "TKT-001",
  "status": "OPEN",
  "priority": "HIGH",
  "createdAt": "2024-12-24T10:30:00",
  "customerId": 1,
  "assignedTo": 10
}
```

**Validation Rules:**
- `ticketNumber` - Required, max 50 chars, must be unique
- `status` - Required, one of: OPEN, IN_PROGRESS, RESOLVED, CLOSED
- `priority` - Required, one of: CRITICAL, HIGH, MEDIUM, LOW

---

### 2. Get Ticket by ID
**Endpoint:** `GET /api/tickets/{id}`

**Status Code:** `200 OK`

**Example:** `GET /api/tickets/1`

**Response:**
```json
{
  "id": 1,
  "ticketNumber": "TKT-001",
  "status": "OPEN",
  "priority": "HIGH",
  "createdAt": "2024-12-24T10:30:00",
  "customerId": 1,
  "assignedTo": 10
}
```

**Error Response (404):**
```json
{
  "timestamp": "2024-12-24T10:35:00",
  "status": 404,
  "error": "Ticket Not Found",
  "message": "Ticket with id '999' not found",
  "path": "/api/tickets/999"
}
```

---

### 3. Update Ticket
**Endpoint:** `PUT /api/tickets/{id}`

**Status Code:** `200 OK`

**Example:** `PUT /api/tickets/1`

**Request Body:**
```json
{
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "customerId": 2,
  "assignedTo": 11
}
```

**Response:**
```json
{
  "id": 1,
  "ticketNumber": "TKT-001",
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "createdAt": "2024-12-24T10:30:00",
  "customerId": 2,
  "assignedTo": 11
}
```

**Validation Rules:**
- `status` - Required, one of: OPEN, IN_PROGRESS, RESOLVED, CLOSED
- `priority` - Required, one of: CRITICAL, HIGH, MEDIUM, LOW

---

### 4. Delete Ticket
**Endpoint:** `DELETE /api/tickets/{id}`

**Status Code:** `204 No Content`

**Example:** `DELETE /api/tickets/1`

**Response:** Empty body (204 status only)

**Error Response (404):**
```json
{
  "timestamp": "2024-12-24T10:40:00",
  "status": 404,
  "error": "Ticket Not Found",
  "message": "Ticket with id '999' not found",
  "path": "/api/tickets/999"
}
```

## HTTP Status Codes

| Code | Status | Usage |
|------|--------|-------|
| 200 | OK | Successful GET, PUT requests |
| 201 | Created | Successful POST request |
| 204 | No Content | Successful DELETE request |
| 400 | Bad Request | Validation error |
| 404 | Not Found | Ticket not found |
| 409 | Conflict | Duplicate ticket number |
| 500 | Internal Server Error | Server error |

## Error Handling

### TicketNotFoundException
- **Status Code:** 404
- **Thrown:** When ticket ID not found
- **Message:** `"Ticket with id '...' not found"`

### DuplicateTicketException
- **Status Code:** 409
- **Thrown:** When creating ticket with existing ticket number
- **Message:** `"Ticket with number '...' already exists"`

### Validation Error
- **Status Code:** 400
- **Thrown:** When request validation fails
- **Response:** Contains field-level error details

```json
{
  "timestamp": "2024-12-24T10:45:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/tickets",
  "fieldErrors": {
    "ticketNumber": "Ticket number is required",
    "status": "Status must be one of: OPEN, IN_PROGRESS, RESOLVED, CLOSED"
  }
}
```

## Entity Details

### Ticket Entity

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PRIMARY KEY, AUTO_INCREMENT | Unique identifier |
| ticketNumber | String(50) | UNIQUE, NOT NULL | Unique ticket reference |
| status | String(20) | NOT NULL | Ticket status |
| priority | String(20) | NOT NULL | Priority level |
| createdAt | LocalDateTime | NOT NULL, DEFAULT NOW | Creation timestamp |
| customerId | Long | NULL | Customer ID |
| assignedTo | Integer | NULL | Assigned team member |

### Valid Status Values
```
OPEN, IN_PROGRESS, RESOLVED, CLOSED
```

### Valid Priority Values
```
CRITICAL, HIGH, MEDIUM, LOW
```

## Key Features

✅ **Clean Architecture** - Layered architecture (controller → service → repository)
✅ **Constructor-Based Dependency Injection** - Best practice DI
✅ **Global Exception Handling** - Centralized error management
✅ **Input Validation** - Jakarta Validation annotations
✅ **JPA Entities** - Proper ORM mapping with indexes
✅ **DTOs** - Request/Response separation
✅ **Transaction Management** - @Transactional support
✅ **Lombok** - Reduced boilerplate code
✅ **RESTful API** - Proper HTTP methods and status codes
✅ **Production-Ready** - Enterprise-level code quality

## Configuration File

### application.properties

```properties
# Application Name
spring.application.name=TicketingApplication

# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/ticketing_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.batch_size=20

# Logging Configuration
logging.level.root=INFO
logging.level.org.example.ticketingapplication=DEBUG
```

## Service Layer Details

### TicketService Interface

```java
public interface TicketService {
    TicketResponse createTicket(CreateTicketRequest request);
    TicketResponse getTicketById(Long id);
    TicketResponse updateTicket(Long id, UpdateTicketRequest request);
    void deleteTicket(Long id);
}
```

### Key Implementation Details

1. **createTicket()**
   - Validates ticket number uniqueness
   - Throws `DuplicateTicketException` if duplicate
   - Auto-saves created timestamp

2. **getTicketById()**
   - Read-only transaction
   - Throws `TicketNotFoundException` if not found
   - Returns populated DTO

3. **updateTicket()**
   - Updates only provided fields
   - Throws `TicketNotFoundException` if not found
   - Preserves original creation timestamp

4. **deleteTicket()**
   - Throws `TicketNotFoundException` if not found
   - Completely removes ticket

## Repository Methods

```java
// Finder methods
Optional<Ticket> findByTicketNumber(String ticketNumber);
List<Ticket> findByStatus(String status);

// Inherited from JpaRepository
Optional<Ticket> findById(Long id);
List<Ticket> findAll();
void deleteById(Long id);
boolean existsById(Long id);
```

## Testing with cURL

```bash
# Create a ticket
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "ticketNumber": "TKT-001",
    "status": "OPEN",
    "priority": "HIGH",
    "customerId": 1,
    "assignedTo": 10
  }'

# Get ticket by ID
curl http://localhost:8080/api/tickets/1

# Update ticket
curl -X PUT http://localhost:8080/api/tickets/1 \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "priority": "CRITICAL",
    "customerId": 1,
    "assignedTo": 10
  }'

# Delete ticket
curl -X DELETE http://localhost:8080/api/tickets/1
```

## Troubleshooting

### Database Connection Error
- Ensure MySQL is running on localhost:3306
- Verify database name is `ticketing_db`
- Check username/password in application.properties

### Table Not Found
- Ensure `spring.jpa.hibernate.ddl-auto=update` is set
- Tables will be auto-created on first run
- Or manually run the CREATE TABLE statements

### Port Already in Use
- Change `server.port` in application.properties
- Or kill the process using port 8080

## Future Enhancements

- Pagination and sorting for list operations
- Search/filter capabilities
- Redis caching layer
- API documentation with Swagger/Springdoc
- Unit and integration tests
- Authentication and authorization
- Audit logging
- Batch operations

## Author

**Senior Java Backend Engineer**

## License

This project is provided as-is for educational and commercial use.

---

**Version:** 1.0.0  
**Status:** Production Ready  
**Last Updated:** December 2024

