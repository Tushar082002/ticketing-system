# API Testing Guide

This guide provides examples for testing all endpoints of the Ticketing Management System API.

## Base URL
```
http://localhost:8080/api/tickets
```

## Prerequisites

1. Application running: `mvn spring-boot:run`
2. MySQL database created and running
3. Tables auto-created by Hibernate (or run `database-setup.sql`)

---

## 1. CREATE TICKET (POST)

### Valid Request
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "ticketNumber": "TKT-2024-001",
    "status": "OPEN",
    "priority": "HIGH",
    "customerId": 1,
    "assignedTo": 10
  }'
```

### Expected Response (201 Created)
```json
{
  "id": 1,
  "ticketNumber": "TKT-2024-001",
  "status": "OPEN",
  "priority": "HIGH",
  "createdAt": "2024-12-24T14:30:00",
  "customerId": 1,
  "assignedTo": 10
}
```

### Error: Duplicate Ticket Number
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "ticketNumber": "TKT-2024-001",
    "status": "OPEN",
    "priority": "HIGH"
  }'
```

**Response (409 Conflict)**
```json
{
  "timestamp": "2024-12-24T14:35:00",
  "status": 409,
  "error": "Duplicate Ticket",
  "message": "Ticket with number 'TKT-2024-001' already exists",
  "path": "/api/tickets"
}
```

### Error: Invalid Status
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "ticketNumber": "TKT-2024-002",
    "status": "INVALID_STATUS",
    "priority": "HIGH"
  }'
```

**Response (400 Bad Request)**
```json
{
  "timestamp": "2024-12-24T14:40:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/tickets",
  "fieldErrors": {
    "status": "Status must be one of: OPEN, IN_PROGRESS, RESOLVED, CLOSED"
  }
}
```

### Error: Missing Required Fields
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "status": "OPEN"
  }'
```

**Response (400 Bad Request)**
```json
{
  "timestamp": "2024-12-24T14:45:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/tickets",
  "fieldErrors": {
    "ticketNumber": "Ticket number is required",
    "priority": "Priority is required"
  }
}
```

---

## 2. GET TICKET BY ID (GET)

### Valid Request
```bash
curl http://localhost:8080/api/tickets/1
```

### Expected Response (200 OK)
```json
{
  "id": 1,
  "ticketNumber": "TKT-2024-001",
  "status": "OPEN",
  "priority": "HIGH",
  "createdAt": "2024-12-24T14:30:00",
  "customerId": 1,
  "assignedTo": 10
}
```

### Error: Ticket Not Found
```bash
curl http://localhost:8080/api/tickets/999
```

**Response (404 Not Found)**
```json
{
  "timestamp": "2024-12-24T14:50:00",
  "status": 404,
  "error": "Ticket Not Found",
  "message": "Ticket with id '999' not found",
  "path": "/api/tickets/999"
}
```

---

## 3. UPDATE TICKET (PUT)

### Valid Request
```bash
curl -X PUT http://localhost:8080/api/tickets/1 \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "priority": "CRITICAL",
    "customerId": 2,
    "assignedTo": 11
  }'
```

### Expected Response (200 OK)
```json
{
  "id": 1,
  "ticketNumber": "TKT-2024-001",
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "createdAt": "2024-12-24T14:30:00",
  "customerId": 2,
  "assignedTo": 11
}
```

**Note:** `createdAt` remains unchanged (immutable field)

### Error: Invalid Priority
```bash
curl -X PUT http://localhost:8080/api/tickets/1 \
  -H "Content-Type: application/json" \
  -d '{
    "status": "RESOLVED",
    "priority": "URGENT"
  }'
```

**Response (400 Bad Request)**
```json
{
  "timestamp": "2024-12-24T14:55:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "path": "/api/tickets/1",
  "fieldErrors": {
    "priority": "Priority must be one of: CRITICAL, HIGH, MEDIUM, LOW"
  }
}
```

### Error: Ticket Not Found
```bash
curl -X PUT http://localhost:8080/api/tickets/999 \
  -H "Content-Type: application/json" \
  -d '{
    "status": "RESOLVED",
    "priority": "HIGH"
  }'
```

**Response (404 Not Found)**
```json
{
  "timestamp": "2024-12-24T15:00:00",
  "status": 404,
  "error": "Ticket Not Found",
  "message": "Ticket with id '999' not found",
  "path": "/api/tickets/999"
}
```

---

## 4. DELETE TICKET (DELETE)

### Valid Request
```bash
curl -X DELETE http://localhost:8080/api/tickets/1
```

### Expected Response (204 No Content)
```
(Empty body, just 204 status code)
```

### Error: Ticket Not Found
```bash
curl -X DELETE http://localhost:8080/api/tickets/999
```

**Response (404 Not Found)**
```json
{
  "timestamp": "2024-12-24T15:05:00",
  "status": 404,
  "error": "Ticket Not Found",
  "message": "Ticket with id '999' not found",
  "path": "/api/tickets/999"
}
```

---

## Test Scenario: Complete Workflow

### Step 1: Create a Ticket
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "ticketNumber": "TKT-2024-TEST-001",
    "status": "OPEN",
    "priority": "HIGH",
    "customerId": 5,
    "assignedTo": 15
  }'
```

Save the returned `id` (e.g., 5)

### Step 2: Retrieve the Ticket
```bash
curl http://localhost:8080/api/tickets/5
```

### Step 3: Update the Ticket Status
```bash
curl -X PUT http://localhost:8080/api/tickets/5 \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "customerId": 5,
    "assignedTo": 15
  }'
```

### Step 4: Update to Resolved
```bash
curl -X PUT http://localhost:8080/api/tickets/5 \
  -H "Content-Type: application/json" \
  -d '{
    "status": "RESOLVED",
    "priority": "HIGH",
    "customerId": 5,
    "assignedTo": 15
  }'
```

### Step 5: Close the Ticket
```bash
curl -X PUT http://localhost:8080/api/tickets/5 \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CLOSED",
    "priority": "HIGH",
    "customerId": 5,
    "assignedTo": 15
  }'
```

### Step 6: Delete the Ticket
```bash
curl -X DELETE http://localhost:8080/api/tickets/5
```

---

## Valid Enum Values

### Status
```
OPEN
IN_PROGRESS
RESOLVED
CLOSED
```

### Priority
```
CRITICAL
HIGH
MEDIUM
LOW
```

---

## HTTP Status Code Reference

| Code | Meaning | When |
|------|---------|------|
| 200 | OK | GET, PUT successful |
| 201 | Created | POST successful |
| 204 | No Content | DELETE successful |
| 400 | Bad Request | Validation failed |
| 404 | Not Found | Ticket doesn't exist |
| 409 | Conflict | Duplicate ticket number |
| 500 | Server Error | Unexpected error |

---

## Testing with Postman

### 1. Import Collection
Create a new Postman collection with these requests:

**Create Ticket**
- Method: POST
- URL: `{{baseUrl}}/api/tickets`
- Body (raw JSON):
```json
{
  "ticketNumber": "TKT-{{$timestamp}}",
  "status": "OPEN",
  "priority": "HIGH",
  "customerId": 1,
  "assignedTo": 10
}
```

**Get Ticket**
- Method: GET
- URL: `{{baseUrl}}/api/tickets/{{ticketId}}`

**Update Ticket**
- Method: PUT
- URL: `{{baseUrl}}/api/tickets/{{ticketId}}`
- Body (raw JSON):
```json
{
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "customerId": 2,
  "assignedTo": 11
}
```

**Delete Ticket**
- Method: DELETE
- URL: `{{baseUrl}}/api/tickets/{{ticketId}}`

### 2. Environment Variables
```json
{
  "baseUrl": "http://localhost:8080/api/tickets",
  "ticketId": ""
}
```

---

## Testing with VSCode REST Client

Create file `test.http`:

```http
### Variables
@baseUrl = http://localhost:8080/api/tickets

### Create Ticket
POST {{baseUrl}}
Content-Type: application/json

{
  "ticketNumber": "TKT-2024-001",
  "status": "OPEN",
  "priority": "HIGH",
  "customerId": 1,
  "assignedTo": 10
}

### Get Ticket
GET {{baseUrl}}/1

### Update Ticket
PUT {{baseUrl}}/1
Content-Type: application/json

{
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "customerId": 2,
  "assignedTo": 11
}

### Delete Ticket
DELETE {{baseUrl}}/1
```

---

## Validation Rules Summary

### CreateTicketRequest
```
- ticketNumber: Required, max 50 chars, unique
- status: Required, pattern: OPEN|IN_PROGRESS|RESOLVED|CLOSED
- priority: Required, pattern: CRITICAL|HIGH|MEDIUM|LOW
- customerId: Optional, must be positive if provided
- assignedTo: Optional, must be positive if provided
```

### UpdateTicketRequest
```
- status: Required, pattern: OPEN|IN_PROGRESS|RESOLVED|CLOSED
- priority: Required, pattern: CRITICAL|HIGH|MEDIUM|LOW
- customerId: Optional, must be positive if provided
- assignedTo: Optional, must be positive if provided
```

---

## Troubleshooting Tests

### Issue: Connection Refused
**Solution:** Ensure application is running with `mvn spring-boot:run`

### Issue: 404 Database Error
**Solution:** Run `database-setup.sql` to create tables

### Issue: 409 Duplicate Ticket
**Solution:** Use unique ticket numbers or delete the existing ticket

### Issue: 400 Validation Error
**Solution:** Check field values against valid enums and constraints

---

## Performance Testing

### Load Test with ApacheBench
```bash
# Create 100 requests
ab -n 100 -c 10 http://localhost:8080/api/tickets/1
```

### Load Test with Apache JMeter
1. Create Thread Group
2. Add HTTP Sampler
3. Configure for desired load

---

**Last Updated:** December 2024  
**Version:** 1.0.0

