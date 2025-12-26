# ⚡ QUICK START GUIDE

## 🚀 Get Running in 5 Minutes

### Step 1: Setup Database (1 min)
```bash
mysql -u root -p
CREATE DATABASE ticketing_db;
```

### Step 2: Configure Application (1 min)
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.password=your_password
```

### Step 3: Build (1 min)
```bash
mvn clean install
```

### Step 4: Run (1 min)
```bash
mvn spring-boot:run
```

### Step 5: Test (1 min)
```bash
# Create ticket
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"ticketNumber":"TKT-001","status":"OPEN","priority":"HIGH"}'

# Get ticket
curl http://localhost:8080/api/tickets/1

# Update ticket
curl -X PUT http://localhost:8080/api/tickets/1 \
  -H "Content-Type: application/json" \
  -d '{"status":"RESOLVED","priority":"HIGH"}'

# Delete ticket
curl -X DELETE http://localhost:8080/api/tickets/1
```

---

## 📋 Complete File List

### Java Classes (13 files)
1. **entity/Ticket.java** - JPA Entity
2. **repository/TicketRepository.java** - Repository
3. **service/TicketService.java** - Service Interface
4. **service/TicketServiceImpl.java** - Service Implementation
5. **controller/TicketController.java** - REST Controller
6. **dto/CreateTicketRequest.java** - Request DTO
7. **dto/UpdateTicketRequest.java** - Update DTO
8. **dto/TicketResponse.java** - Response DTO
9. **exception/TicketNotFoundException.java** - Exception
10. **exception/DuplicateTicketException.java** - Exception
11. **exception/GlobalExceptionHandler.java** - Exception Handler
12. **exception/ErrorResponse.java** - Error DTO
13. **exception/ValidationErrorResponse.java** - Validation Error DTO

### Configuration & Resources (3 files)
14. **application.properties** - Configuration
15. **database-setup.sql** - Database Script
16. **pom.xml** - Maven Dependencies

### Documentation (4 files)
17. **README.md** - Main Documentation
18. **API_TESTING_GUIDE.md** - Testing Examples
19. **IMPLEMENTATION_SUMMARY.md** - What Was Built
20. **QUICK_START_GUIDE.md** - This File

---

## 🎯 API Endpoints

```
POST   /api/tickets           - Create ticket (201)
GET    /api/tickets/{id}      - Get ticket (200)
PUT    /api/tickets/{id}      - Update ticket (200)
DELETE /api/tickets/{id}      - Delete ticket (204)
```

---

## ✅ Status

| Item | Status |
|------|--------|
| Compilation | ✅ No Errors |
| STEP 1 (Entity) | ✅ Complete |
| STEP 2 (Service/Controller) | ✅ Complete |
| Error Handling | ✅ Complete |
| Documentation | ✅ Complete |
| Ready to Run | ✅ Yes |

---

## 🔧 System Requirements

- Java 17+
- MySQL 8.0+
- Maven 3.6+
- 10 minutes for complete setup

---

## 📞 Documentation

- **Setup & Overview**: README.md
- **API Examples**: API_TESTING_GUIDE.md
- **Implementation Details**: IMPLEMENTATION_SUMMARY.md
- **Database**: database-setup.sql

---

## ⚙️ Database

**Auto-created by Hibernate**, or manually run:
```bash
mysql ticketing_db < src/main/resources/database-setup.sql
```

---

## 🎓 Key Technologies

- Spring Boot 4.0.1
- Spring Data JPA
- MySQL 8.0
- Hibernate ORM
- Lombok
- Jakarta Validation

---

**Everything is ready to go! Build and run with:**
```bash
mvn clean install && mvn spring-boot:run
```

Server starts: `http://localhost:8080` ✨

