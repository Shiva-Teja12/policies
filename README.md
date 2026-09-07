# HRMS Policies 2 — PostgreSQL

HRMS Policies is a full-stack policy management module built using Spring Boot, PostgreSQL, Spring Security, JWT, Google OAuth 2.0, and Next.js.

The application supports policy creation, approval workflows, publishing, versioning, employee acknowledgement, compliance tracking, notifications, role-based authorization, and secure authentication.

---

## Technology Stack

### Backend

- Java 17
- Spring Boot 3.5.4
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- JWT Authentication
- Google OAuth 2.0
- Bean Validation
- springdoc-openapi / Swagger
- Maven

### Database

- PostgreSQL

### Frontend

- Next.js
- React
- TypeScript
- Tailwind CSS
- Lucide React

---

## Project Structure

```text
hrmspolicies2/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── target/
│
├── frontend/
│   ├── app/
│   ├── components/
│   ├── lib/
│   ├── package.json
│   └── .env.local
│
├── database/
│   └── schema.sql
│
├── postman/
│
├── README.md
├── .env.example
└── .gitignore
```

---

# Main Features

The Policies module provides the following functionality:

- User authentication
- Google OAuth 2.0 login
- JWT authentication
- Role-based access control
- Policy creation
- Policy editing
- Policy deletion
- Policy approval workflow
- Policy publishing
- Policy versioning
- Policy retirement
- Employee policy acknowledgement
- Policy status tracking
- Compliance dashboard
- Notifications
- Reminders
- Search
- Filtering
- Sorting
- Pagination
- Global exception handling
- Swagger/OpenAPI documentation

---

# Application Roles

The application supports the following roles:

```text
HR_ADMIN
LEGAL_REVIEWER
HR_HEAD
MANAGING_DIRECTOR
MANAGER
EMPLOYEE
```

## HR Admin

HR Admin can perform administrative policy operations such as:

- Create policies
- Edit policies
- Delete policies
- Submit policies for review
- Manage policy categories
- Publish approved policies
- Retire policies
- View approvals
- View compliance information
- Send reminders

## Legal Reviewer

Legal Reviewer participates in the policy approval workflow.

The Legal Reviewer can:

- View policies waiting for legal review
- Approve policies
- Reject policies
- Add review comments
- View notifications

## HR Head

HR Head performs the next approval stage after legal review.

The HR Head can:

- View policies waiting for HR Head review
- Approve policies
- Reject policies
- Add comments
- View notifications

## Managing Director

The Managing Director performs final approval when required.

The Managing Director can:

- View policies requiring final approval
- Approve policies
- Reject policies
- Add comments
- View notifications

## Manager

Managers can access published policies and other functionality allowed by the application.

## Employee

Employees can:

- View applicable published policies
- View policy details
- Acknowledge policies
- View acknowledgement status
- View notifications

---

# Policy Lifecycle

A policy moves through the application's workflow.

Typical lifecycle:

```text
DRAFT
  ↓
LEGAL_REVIEW
  ↓
HR_HEAD_REVIEW
  ↓
MD_REVIEW
  ↓
APPROVED
  ↓
PUBLISHED
```

A policy may also enter:

```text
REJECTED
RETIRED
```

If a policy is rejected, HR Admin can modify it and save it again as a draft before resubmitting it for review.

---

# Policy Approval Workflow

The approval process is:

```text
HR Admin
   ↓
Create Policy
   ↓
DRAFT
   ↓
Submit for Review
   ↓
Legal Reviewer
   ↓
HR Head
   ↓
Managing Director
   ↓
APPROVED
   ↓
HR Admin Publishes
   ↓
PUBLISHED
```

Authorization is enforced by the Spring Boot backend.

Frontend role checks are used for the user interface but are not considered sufficient security on their own.

---

# Main Entities

## User

Important User fields include:

| Field | Type | Description |
| --- | --- | --- |
| id | Long | Primary key |
| name | String | User name |
| email | String | Unique email address |
| password | String | Authentication password |
| role | Role | Application role |
| accountStatus | AccountStatus | ACTIVE, DISABLED, or LOCKED |
| dateOfJoining | LocalDate | Date of joining |
| department | String | Department |
| grade | String | Employee grade |
| managerEmail | String | Manager email |
| createdAt | LocalDateTime | Creation timestamp |
| updatedAt | LocalDateTime | Update timestamp |

Passwords are never returned in normal API responses.

---

## Policy

Important Policy information includes:

- ID
- Name
- Unique policy code
- Category
- Content
- Applicability
- Mandatory flag
- Status
- Created by
- Created timestamp
- Updated timestamp
- Retirement information

Supported applicability values include:

```text
ALL
GRADE_BASED
DEPT_BASED
```

---

## PolicyVersion

Publishing a policy creates a version snapshot.

Important fields include:

- Policy
- Version number
- Content
- Effective date
- Published date
- Deadline
- Change summary
- Published by

Version numbers are automatically incremented.

---

## PolicyAcknowledgement

Employee acknowledgement information includes:

- Policy
- Policy version
- Employee
- Acknowledged timestamp
- IP address
- User agent

---

## PolicyApproval

Approval information includes:

- Policy
- Approval stage
- Approval status
- Comments
- Submitted timestamp
- Actioned timestamp

Approval stages include:

```text
LEGAL_REVIEW
HR_HEAD
MANAGING_DIRECTOR
```

Approval statuses include:

```text
PENDING
APPROVED
REJECTED
```

---

## PolicyCategory

Policy categories are used to organize policies.

Important information includes:

- ID
- Name
- Code
- Description
- Active status
- Created timestamp
- Updated timestamp

---

# API Base URL

Development backend:

```text
http://localhost:8080
```

---

# API Response Format

API responses use a JSON response structure.

## Successful API Response

Example:

```json
{
  "success": true,
  "message": "Request completed successfully",
  "data": {},
  "timestamp": "2026-08-31T17:00:00"
}
```

## Error API Response

Example:

```json
{
  "success": false,
  "message": "Policy not found",
  "status": 404,
  "path": "/api/policies/99",
  "errors": null,
  "timestamp": "2026-08-31T17:00:00"
}
```

The `errors` field can contain field-level validation messages for validation failures.

---

# Authentication APIs

Common authentication endpoints include:

| Method | Endpoint | Authentication | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | Public | Register an employee |
| POST | `/api/auth/login` | Public | Login and receive JWT |
| POST | `/api/auth/forgot-password` | Public | Start password recovery |
| POST | `/api/auth/reset-password` | Public | Reset password |

---

# Policy APIs

Important policy endpoints include:

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/policies` | Create policy |
| GET | `/api/policies` | Get/search policies |
| GET | `/api/policies/{id}` | Get policy by ID |
| PUT | `/api/policies/{id}` | Update policy |
| DELETE | `/api/policies/{id}` | Delete policy |
| PATCH | `/api/policies/{id}/submit-for-review` | Submit policy for approval |
| POST | `/api/policies/{id}/publish` | Publish policy |
| POST | `/api/policies/{id}/acknowledge` | Acknowledge policy |
| GET | `/api/policies/my-status` | Employee acknowledgement status |
| GET | `/api/policies/{id}/versions` | Policy version history |

Some endpoint details may vary depending on the current controller implementation.

---

# Approval APIs

Important approval operations include:

```text
GET  /api/approvals
POST /api/approvals/{approvalId}/approve
POST /api/approvals/{approvalId}/reject
```

Access is controlled according to the authenticated user's role and the current approval stage.

---

# Pagination, Sorting and Filtering

Policy listing supports filtering and pagination where implemented.

Typical parameters include:

| Parameter | Example | Description |
| --- | --- | --- |
| search | leave | Search policy information |
| categoryId | 1 | Filter by category |
| status | PUBLISHED | Filter by policy status |
| applicability | ALL | Filter by applicability |
| mandatory | true | Filter mandatory policies |
| page | 0 | Zero-based page number |
| size | 10 | Number of records per page |

---

# Validation

Request DTOs use Jakarta Bean Validation annotations such as:

```text
@NotBlank
@NotNull
@Size
@Pattern
```

Invalid requests are rejected before normal service processing.

Validation errors are returned using the application's error response structure.

---

# Global Exception Handling

The application uses a global exception handler with:

```text
@RestControllerAdvice
```

Typical HTTP responses include:

| Situation | HTTP Status |
| --- | --- |
| Successful request | 200 |
| Invalid request | 400 |
| Authentication required | 401 |
| Access denied | 403 |
| Resource not found | 404 |
| Duplicate resource | 409 |
| Unexpected server error | 500 |

---

# PostgreSQL Setup

## Create Database

Start PostgreSQL and create the database if it does not already exist.

```sql
CREATE DATABASE hrmspolicies2;
```

Database creation can also be performed using pgAdmin.

---

## PostgreSQL Connection

Example development configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/hrmspolicies2
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRESQL_PASSWORD
```

For development:

```properties
spring.jpa.hibernate.ddl-auto=update
```

For production, schema validation can be used:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Do not commit real database credentials.

---

# Environment Variables

Sensitive configuration should be provided through environment variables.

Typical variables include:

```text
SERVER_PORT
SPRING_PROFILES_ACTIVE

DB_URL
DB_USERNAME
DB_PASSWORD

JWT_SECRET
JWT_EXPIRATION

GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET

CORS_ALLOWED_ORIGINS
```

Example:

```env
DB_URL=jdbc:postgresql://localhost:5432/hrmspolicies2
DB_USERNAME=postgres
DB_PASSWORD=your-postgresql-password

JWT_SECRET=your-jwt-secret
JWT_EXPIRATION=86400000

GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Do not place real production secrets in `.env.example`.

---

# JWT Authentication

The application uses JWT authentication for protected APIs.

After successful login, the backend generates a JWT containing information such as:

- User ID
- Email
- Role
- Issued time
- Expiration time

The frontend sends the JWT using:

```text
Authorization: Bearer <JWT>
```

The backend validates the token before allowing access to protected APIs.

---

# JWT Expiration

JWT expiration is configured using:

```properties
jwt.expiration=${JWT_EXPIRATION:86400000}
```

The default value is:

```text
86400000 milliseconds
```

which equals:

```text
24 hours
```

When a JWT is expired or invalid:

```text
Protected API Request
        ↓
JwtAuthenticationFilter
        ↓
JWT validation fails
        ↓
Authentication rejected
        ↓
401 Unauthorized
```

The frontend handles a `401` response by:

```text
401 Unauthorized
      ↓
clearSession()
      ↓
Remove token and user
      ↓
Redirect to /login
```

---

# Role-Based Authorization

Authentication verifies who the user is.

Authorization determines what the user is allowed to do.

Spring Security protects backend endpoints according to roles.

For example:

```text
POST /api/policies
```

is restricted to:

```text
HR_ADMIN
```

Therefore:

```text
HR_ADMIN JWT
      ↓
POST /api/policies
      ↓
Allowed
```

but:

```text
EMPLOYEE JWT
      ↓
POST /api/policies
      ↓
403 Forbidden
```

Authorization may be configured using:

```text
hasRole(...)
hasAnyRole(...)
@PreAuthorize(...)
```

---

# Google OAuth 2.0 Authentication

The HRMS Policies application supports Google OAuth 2.0 authentication in addition to normal email/password login.

---

## Google OAuth Authentication Flow

```text
User
  ↓
HRMS Login Page
  ↓
Continue with Google
  ↓
Google Authentication
  ↓
Spring Boot OAuth Callback
  ↓
Read Google User Information
  ↓
Find Existing User or Create User
  ↓
Application Role Authorization
  ↓
Generate HRMS JWT
  ↓
Next.js OAuth Success Page
  ↓
Dashboard
  ↓
Protected APIs
```

Google authenticates the identity of the user.

The HRMS application controls application authorization.

---

# Google OAuth Configuration

Create an OAuth 2.0 Web Application in Google Cloud Console.

## Authorized JavaScript Origin

```text
http://localhost:3000
```

## Authorized Redirect URI

```text
http://localhost:8080/login/oauth2/code/google
```

Google redirects the authentication result to the Spring Boot backend.

After successful processing, the backend redirects the browser to:

```text
http://localhost:3000/oauth-success
```

---

# Spring Boot Google OAuth Configuration

Configure Google OAuth using environment variables.

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
```

Never hard-code the real Google client secret in source code.

---

# Google User Information

After successful Google authentication, the backend receives information including:

- Google account email
- Google account name

The email is normalized before database lookup.

---

# Existing Google User

If a user with the same email already exists:

```text
Google Login
     ↓
Email Lookup
     ↓
Existing User Found
     ↓
Existing Application Account Used
```

The user's existing application role is retained.

For example, if an existing account has:

```text
role = HR_HEAD
```

Google login does not automatically change that user to `EMPLOYEE`.

The database remains the source of truth for application authorization.

---

# New Google User

If the Google email does not exist in the application database, a new user is created.

The default application role is:

```text
EMPLOYEE
```

The default account status is:

```text
ACTIVE
```

This prevents users from choosing privileged roles simply by using Google authentication.

---

# Duplicate User Prevention

Google users are looked up using their normalized email address before creating a new database record.

Duplicate email accounts can be checked in PostgreSQL using:

```sql
SELECT email, COUNT(*)
FROM users
GROUP BY email
HAVING COUNT(*) > 1;
```

If the query returns no rows, duplicate email accounts were not found.

---

# Google Authentication vs Application Authorization

Google is responsible for authentication.

Example:

```text
Google confirms:
"This user controls this Google account."
```

The HRMS database is responsible for authorization.

Example:

```text
HRMS database:
email = employee@example.com
role = EMPLOYEE
```

The frontend-selected portal or role must not be trusted as the source of authorization.

Backend security rules determine access.

---

# OAuth JWT Generation

After successful Google authentication, the backend generates the same type of application JWT used by normal login.

The JWT contains:

```text
subject = email
userId
role
issuedAt
expiration
```

The frontend then uses this JWT when calling protected Spring Boot APIs.

---

# OAuth Session Handling

Spring Security temporarily requires an HTTP session during the OAuth authorization process.

The configuration uses:

```text
SessionCreationPolicy.IF_REQUIRED
```

This allows Spring Security to maintain the OAuth authorization request while communicating with Google.

After successful authentication:

```text
Google authentication succeeds
        ↓
Application JWT generated
        ↓
OAuth authentication attributes cleared
        ↓
Spring SecurityContext cleared
        ↓
Temporary HTTP session invalidated
        ↓
Frontend uses application JWT
```

Protected APIs therefore rely on the application's JWT after OAuth login.

---

# OAuth Success Page

After successful authentication, the backend redirects to:

```text
http://localhost:3000/oauth-success
```

The frontend OAuth success page:

1. Reads the authentication information.
2. Stores the application JWT.
3. Stores the user session.
4. Removes authentication information from the visible URL.
5. Redirects to the user's dashboard.

The normal login and Google login use the same frontend session structure.

---

# OAuth Error Handling

If Google authentication fails, the backend redirects the browser to the login page with an OAuth error.

Example:

```text
/login?oauthError=<message>
```

The login page displays the error message.

Possible error scenarios include:

- Google authentication failure
- Authentication cancellation
- Missing email information
- Invalid OAuth configuration
- Disabled application account
- Locked application account

---

# Account Status Validation

OAuth authentication does not bypass application account status.

The application supports account statuses such as:

```text
ACTIVE
DISABLED
LOCKED
```

Only an active account is allowed to complete application authentication.

---

# Logout

Frontend logout removes authentication information from Local Storage.

The session cleanup removes:

```text
token
user
userId
name
email
role
```

The browser is then redirected to:

```text
/login
```

The logout process signs the user out of the HRMS application.

It does not necessarily sign the user out of their Google account.

---

# HTTP Authentication and Authorization Results

## 200 OK

A valid JWT with sufficient permissions allows access.

Example:

```text
Valid HR Admin JWT
        ↓
GET /api/policies
        ↓
200 OK
```

## 401 Unauthorized

A missing, invalid, or expired JWT causes:

```text
GET /api/policies
        ↓
401 Unauthorized
```

## 403 Forbidden

A valid JWT with insufficient permissions causes:

```text
EMPLOYEE JWT
        ↓
POST /api/policies
        ↓
403 Forbidden
```

This distinction is important:

```text
401 = Authentication problem
403 = Authorization problem
```

---

# Authentication Testing

The following scenarios should be verified during authentication testing:

| Test | Expected Result |
| --- | --- |
| Email/password login | Login succeeds |
| Google OAuth login | Login succeeds |
| New Google user | Created as EMPLOYEE |
| Existing Google user | Existing account reused |
| Repeated Google login | No duplicate account |
| Protected API without JWT | 401 Unauthorized |
| Protected API with valid JWT | 200 OK |
| Employee accessing HR Admin API | 403 Forbidden |
| Logout | Local session cleared |
| Logout redirect | Redirect to `/login` |
| Expired JWT | 401 Unauthorized |
| Expired JWT frontend handling | Session cleared and redirect to `/login` |
| Disabled OAuth user | Login rejected |
| Locked OAuth user | Login rejected |
| OAuth failure | Redirect to login with error |

---

# Swagger / OpenAPI

Swagger UI is available when the backend is running.

```text
http://localhost:8080/swagger-ui/index.html
```

The OpenAPI specification is available at:

```text
http://localhost:8080/v3/api-docs
```

For JWT-protected APIs:

1. Login using `/api/auth/login`.
2. Copy the returned JWT.
3. Open Swagger.
4. Click `Authorize`.
5. Enter the JWT.
6. Call the protected API.

---

# Security Testing

## Protected API Without JWT

Example:

```text
GET /api/policies
```

Expected result:

```text
401 Unauthorized
```

## Protected API With Valid JWT

Authenticate as an authorized user and send a valid JWT.

Expected result:

```text
200 OK
```

## Wrong Role

Authenticate as an employee and attempt an HR Admin operation.

Example:

```text
POST /api/policies
```

Expected result:

```text
403 Forbidden
```

## Expired Token

After the JWT expires, access to protected endpoints should return:

```text
401 Unauthorized
```

The frontend then clears the session and redirects to `/login`.

---

# Run Backend

Open a terminal in the backend directory:

```bash
cd backend
```

Run:

```bash
mvn clean spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

---

# Run Frontend

Open another terminal:

```bash
cd frontend
```

Install dependencies if required:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The frontend runs at:

```text
http://localhost:3000
```

---

# Frontend Environment Configuration

The frontend API URL can be configured in:

```text
frontend/.env.local
```

Example:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

# Development URLs

## Frontend

```text
http://localhost:3000
```

## Backend

```text
http://localhost:8080
```

## Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

## OpenAPI

```text
http://localhost:8080/v3/api-docs
```

## Google OAuth Login

```text
http://localhost:8080/oauth2/authorization/google
```

## Google OAuth Callback

```text
http://localhost:8080/login/oauth2/code/google
```

## Frontend OAuth Success

```text
http://localhost:3000/oauth-success
```

---

# Security Guidelines

The following security practices should be followed:

- Never commit Google OAuth client secrets.
- Never commit JWT secrets.
- Never commit PostgreSQL passwords.
- Keep real credentials outside source control.
- Use environment variables for sensitive values.
- Keep `.env` and `.env.local` out of Git.
- Validate JWT expiration.
- Enforce authorization on the backend.
- Do not rely only on frontend role checks.
- Use HTTPS in production.
- Configure production OAuth redirect URLs carefully.
- Rotate credentials immediately if they are exposed.

---

# Files That Must Not Be Committed

Sensitive/generated files should be excluded using `.gitignore`.

Typical examples include:

```text
.env
.env.local
*.env.local

target/
.next/
node_modules/

.idea/
*.iml
```

Do not put real passwords, JWT secrets, or OAuth client secrets in `.env.example`.

---

# Example .env.example

A safe `.env.example` can contain placeholders:

```env
DB_URL=jdbc:postgresql://localhost:5432/hrmspolicies2
DB_USERNAME=postgres
DB_PASSWORD=your-postgresql-password

JWT_SECRET=your-jwt-secret
JWT_EXPIRATION=86400000

GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

CORS_ALLOWED_ORIGINS=http://localhost:3000
```

---

# Logging

The backend uses SLF4J logging.

Typical logging levels include:

```text
INFO  - successful application operations
WARN  - validation or expected business problems
ERROR - unexpected application errors
```

Sensitive information such as:

```text
Passwords
JWT secrets
Google client secrets
```

must never be logged.

---

# Development and Production Profiles

Spring Boot profiles can be used to separate development and production configuration.

Typical files include:

```text
application.properties
application-dev.properties
application-prod.properties
```

Development can use:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Production can use:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Production database credentials should be supplied through environment variables.

---

# Manual Testing Checklist

## Authentication

- [x] Email/password login
- [x] Google OAuth login
- [x] New Google user creation
- [x] Existing Google user reuse
- [x] Duplicate Google user prevention
- [x] JWT generation
- [x] Logout
- [x] JWT expiration handling

## Authorization

- [x] Protected API without JWT returns 401
- [x] Protected API with valid JWT returns 200
- [x] Employee accessing HR Admin endpoint returns 403

## Policies

- [ ] Create policy
- [ ] Edit policy
- [ ] Delete policy
- [ ] Search/filter policies
- [ ] Submit policy for review
- [ ] Legal Reviewer approval
- [ ] HR Head approval
- [ ] Managing Director approval
- [ ] Publish approved policy
- [ ] Policy version creation
- [ ] Employee acknowledgement
- [ ] Policy retirement

Update the unchecked items after each scenario has been verified in the current application.

---

# Git Security

Before pushing the project to GitHub, verify that sensitive files are ignored.

Useful command:

```bash
git status
```

Make sure files containing real credentials are not staged.

Never commit:

```text
Google client secret
JWT secret
PostgreSQL password
.env
.env.local
```

If a secret is accidentally committed or publicly exposed, remove it from the repository and rotate the credential.

---

# Summary

The HRMS Policies module provides:

```text
PostgreSQL Database
        +
Spring Boot REST API
        +
Spring Security
        +
JWT Authentication
        +
Google OAuth 2.0
        +
Role-Based Authorization
        +
Policy Approval Workflow
        +
Policy Publishing and Versioning
        +
Employee Acknowledgement
        +
Next.js Frontend
```

The backend remains responsible for authentication validation and authorization enforcement, while the frontend provides role-specific user interfaces and securely handles the application session.

---

# Logging, Monitoring, Profiles and Environment Configuration

This section documents the backend hardening work completed for the HRMS Policies application.

The implementation includes:

- Structured application logging
- Business-operation logging
- Authentication and security logging
- Global exception logging
- Spring Boot Actuator
- Application health monitoring
- Development, test, and production profiles
- Externalized database configuration
- Externalized JWT configuration
- Externalized Google OAuth configuration
- Externalized mail configuration
- Environment-variable based secrets
- Development environment verification


---

# Spring Boot Actuator

The backend uses Spring Boot Actuator for health checks, monitoring, application information, and metrics.

The following dependency is included in `backend/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```


---

# Actuator Common Configuration

Common Actuator configuration can be placed in:

```text
backend/src/main/resources/application.properties
```

Example:

```properties
management.endpoints.web.base-path=/actuator
management.endpoint.health.show-details=when_authorized
management.health.defaults.enabled=true
management.info.env.enabled=true

info.app.name=HRMS Policies
info.app.description=HRMS Policies Management Service
info.app.version=0.0.1-SNAPSHOT
```

Actuator endpoint exposure is controlled separately by the development, test, and production profiles.


---

# Actuator Health Endpoint

Endpoint:

```text
GET /actuator/health
```

Development URL:

```text
http://localhost:8080/actuator/health
```

A healthy application returns:

```json
{
  "status": "UP"
}
```

During development, detailed health information may include:

```text
db
diskSpace
mail
ping
ssl
```

The components represent:

```text
db
PostgreSQL database connectivity

diskSpace
Available disk space

mail
SMTP mail server connectivity

ping
Basic application health

ssl
SSL certificate health information
```

The development environment has successfully reported the application status as:

```text
UP
```

PostgreSQL database connectivity has also been successfully verified.


---

# Actuator Info Endpoint

Endpoint:

```text
GET /actuator/info
```

URL:

```text
http://localhost:8080/actuator/info
```

Application information is configured using:

```properties
info.app.name=HRMS Policies
info.app.description=HRMS Policies Management Service
info.app.version=0.0.1-SNAPSHOT
```


---

# Actuator Metrics Endpoint

Endpoint:

```text
GET /actuator/metrics
```

URL:

```text
http://localhost:8080/actuator/metrics
```

The metrics endpoint exposes available JVM and application metrics.

Examples may include:

```text
JVM memory
JVM threads
HTTP server requests
Process CPU usage
System CPU usage
Database connection pool statistics
Application startup information
```


---

# Actuator Security

Actuator exposure is configured differently for each environment.

Development and testing may expose additional endpoints for troubleshooting.

Production exposure must remain restricted.


---

# Development Actuator Configuration

File:

```text
backend/src/main/resources/application-dev.properties
```

Configuration:

```properties
management.endpoints.web.exposure.include=*
management.endpoint.env.show-values=never
management.endpoint.health.show-details=always

logging.level.root=INFO
logging.level.com.example.hrmspolicies2=DEBUG
```

All Actuator endpoints can be exposed during local development for debugging and verification.

Environment values are hidden using:

```properties
management.endpoint.env.show-values=never
```


---

# Test Actuator Configuration

File:

```text
backend/src/main/resources/application-test.properties
```

Configuration:

```properties
management.endpoints.web.exposure.include=*
management.endpoint.env.show-values=never
management.endpoint.health.show-details=always

logging.level.root=INFO
logging.level.com.example.hrmspolicies2=INFO
```

The test environment can expose additional Actuator endpoints to support automated and integration testing.


---

# Production Actuator Configuration

File:

```text
backend/src/main/resources/application-prod.properties
```

Configuration:

```properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.env.show-values=never
management.endpoint.health.show-details=never

logging.level.root=INFO
logging.level.com.example.hrmspolicies2=INFO
```

Production must not expose every Actuator endpoint.

Only the required monitoring endpoints should be available:

```text
health
info
metrics
```

Detailed health information is disabled in production to reduce information exposure.


---

# Spring Boot Profiles

The backend supports three environment profiles:

```text
dev
test
prod
```

The profile files are located in:

```text
backend/src/main/resources/
```

Files:

```text
application.properties
application-dev.properties
application-test.properties
application-prod.properties
```


---

# Active Profile

The active Spring profile can be selected using the environment variable:

```text
SPRING_PROFILES_ACTIVE
```

The common configuration can use:

```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

If no value is supplied, the backend uses:

```text
dev
```

by default.


---

# Development Profile

File:

```text
backend/src/main/resources/application-dev.properties
```

The development profile is used for local application development.

Database configuration should use environment variables:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Actuator and logging:

```properties
management.endpoints.web.exposure.include=*
management.endpoint.env.show-values=never
management.endpoint.health.show-details=always

logging.level.root=INFO
logging.level.com.example.hrmspolicies2=DEBUG
```

The development profile has been tested successfully.


---

# Test Profile

File:

```text
backend/src/main/resources/application-test.properties
```

Example:

```properties
spring.datasource.url=${TEST_DB_URL:jdbc:postgresql://localhost:5432/hrmspolicies_test}
spring.datasource.username=${TEST_DB_USERNAME:postgres}
spring.datasource.password=${TEST_DB_PASSWORD:}

management.endpoints.web.exposure.include=*
management.endpoint.env.show-values=never
management.endpoint.health.show-details=always

logging.level.root=INFO
logging.level.com.example.hrmspolicies2=INFO
```

Real test database credentials are intentionally not committed to Git.

When a dedicated test database is used, provide:

```text
TEST_DB_URL
TEST_DB_USERNAME
TEST_DB_PASSWORD
```

through environment variables.

The profile configuration exists even when a dedicated test database is not currently being used.


---

# Production Profile

File:

```text
backend/src/main/resources/application-prod.properties
```

Example:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.env.show-values=never
management.endpoint.health.show-details=never

logging.level.root=INFO
logging.level.com.example.hrmspolicies2=INFO
```

Production database credentials must never be committed.

They must be provided through the deployment environment.


---

# Externalized Configuration

Sensitive application configuration is supplied using environment variables instead of hardcoded values.

Main environment variables:

```text
SPRING_PROFILES_ACTIVE

DB_URL
DB_USERNAME
DB_PASSWORD

JWT_SECRET
JWT_EXPIRATION

GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET

MAIL_USERNAME
MAIL_PASSWORD

CORS_ALLOWED_ORIGINS

MAIL_ENABLED
MAIL_FROM
```

If the frontend URL is also externalized, use:

```text
FRONTEND_URL
```


---

# Database Environment Configuration

Recommended database configuration:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Example environment values:

```env
DB_URL=jdbc:postgresql://localhost:5432/hrmspolicies2
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
```

Do not place real database credentials inside the README or committed property files.


---

# JWT Environment Configuration

JWT secrets must be externally configured.

Example:

```properties
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:86400000}
```

Environment variables:

```env
JWT_SECRET=your_long_secure_jwt_secret
JWT_EXPIRATION=86400000
```

The actual JWT secret must never be committed.


---

# Google OAuth Environment Configuration

Google OAuth credentials use environment variables.

Example:

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
```

Environment variables:

```env
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
```

The Google client secret must never be hardcoded or committed.


---

# Mail Environment Configuration

Mail configuration should also use environment variables.

Example:

```properties
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

Environment variables:

```env
MAIL_USERNAME=your_email_address
MAIL_PASSWORD=your_mail_app_password
```

Optional notification configuration:

```properties
notifications.mail-enabled=${MAIL_ENABLED:false}
notifications.from-email=${MAIL_FROM:noreply@enfec.com}
```

Example:

```env
MAIL_ENABLED=false
MAIL_FROM=noreply@enfec.com
```


---

# CORS Environment Configuration

Local frontend:

```text
http://localhost:3000
```

CORS origins should preferably be externalized.

Example:

```properties
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

Environment variable:

```env
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Production should use the actual deployed frontend URL.


---

# Example Development Environment Variables

Example only:

```env
SPRING_PROFILES_ACTIVE=dev

DB_URL=jdbc:postgresql://localhost:5432/hrmspolicies2
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password

JWT_SECRET=your_long_secure_jwt_secret
JWT_EXPIRATION=86400000

GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

MAIL_USERNAME=your_email_address
MAIL_PASSWORD=your_mail_app_password

CORS_ALLOWED_ORIGINS=http://localhost:3000

MAIL_ENABLED=false
MAIL_FROM=noreply@enfec.com
```

These values are examples only.

Real credentials must never be added to this README.


---

# Secret Management Rules

Never commit:

```text
Database passwords
JWT secrets
Google client secrets
Google OAuth access tokens
Google OAuth refresh tokens
Google authorization codes
Mail passwords
Password reset tokens
User passwords
API secrets
Authorization headers
Bearer tokens
```

Secrets should always be supplied externally through environment variables or an approved secret-management system.

If a secret was previously committed to Git, deleting it from the latest file does not remove it from previous Git history.

Any exposed credential should be rotated.


---

# Structured Application Logging

The backend uses SLF4J for structured application logging.

The project follows a key-value logging format:

```text
event=EVENT_NAME key=value key=value
```

Example:

```java
log.info(
        "event=POLICY_CREATED policyId={} code={} status={} actorUserId={}",
        policy.getId(),
        policy.getCode(),
        policy.getStatus(),
        user.getId()
);
```

This structure makes application logs easier to:

```text
Search
Filter
Analyze
Monitor
Troubleshoot
Forward to centralized logging systems
```


---

# Logging Levels

The backend follows the following logging strategy.


## INFO

Used for important successful business operations.

Examples:

```text
USER_SIGNUP_SUCCESS
LOGIN_SUCCESS
POLICY_CREATED
POLICY_UPDATED
POLICY_SUBMITTED_FOR_REVIEW
POLICY_APPROVED
POLICY_PUBLISHED
POLICY_ACKNOWLEDGED
POLICY_RETIRED
POLICY_REMINDER_SENT
PASSWORD_RESET_SUCCESS
OAUTH2_USER_CREATED
```


## DEBUG

Used for detailed read operations and development diagnostics.

Examples:

```text
POLICY_SEARCH_REQUEST
POLICY_VERSIONS_REQUEST
NOTIFICATION_LIST_REQUEST
COMPLIANCE_DASHBOARD_REQUEST
POLICY_REMINDER_SKIPPED
MY_POLICY_STATUS_REQUEST
```


## WARN

Used for expected business failures, validation failures, and authorization failures.

Examples:

```text
LOGIN_FAILED
LOGIN_REJECTED
POLICY_ACCESS_DENIED
POLICY_PUBLISH_REJECTED
POLICY_RETIRE_REJECTED
POLICY_ACKNOWLEDGEMENT_DENIED
OAUTH2_LOGIN_REJECTED
VALIDATION_FAILED
FORBIDDEN_REQUEST
RESOURCE_NOT_FOUND
```


## ERROR

Used for unexpected technical failures.

Examples:

```text
UNHANDLED_EXCEPTION
PASSWORD_RESET_EMAIL_FAILED
PASSWORD_CHANGED_EMAIL_FAILED
POLICY_DELETE_FAILED
```


---

# Logging Coverage

Structured logging has been added to important business, security, and monitoring-related classes.

Current logging coverage includes:

```text
AuthService

PolicyService

PolicyApprovalService

PolicyPublishingService

PolicyAcknowledgementService

PolicyRetirementService

NewJoinerPolicyAssignmentService

NotificationService

ReminderHistoryService

PolicyReminderService

ComplianceService

PasswordResetEmailService

GlobalExceptionHandler

OAuth2AuthenticationSuccessHandler

OAuth2AuthenticationFailureHandler
```


---

# Policy Logging

Important policy events include:

```text
POLICY_CREATE_REQUEST
POLICY_CREATED

POLICY_UPDATE_REQUEST
POLICY_UPDATED

POLICY_DELETE_REQUEST
POLICY_DELETED

POLICY_SUBMIT_FOR_REVIEW_REQUEST
POLICY_SUBMITTED_FOR_REVIEW

POLICY_APPROVAL_REQUEST
POLICY_APPROVED

POLICY_REJECTION_REQUEST
POLICY_REJECTED

POLICY_PUBLISH_REQUEST
POLICY_VERSION_CREATED
POLICY_PUBLISHED

POLICY_ACKNOWLEDGEMENT_REQUEST
POLICY_ACKNOWLEDGED

POLICY_RETIRE_REQUEST
POLICY_RETIRED
```


---

# Authentication Logging

Authentication events include:

```text
USER_SIGNUP_REQUEST
USER_SIGNUP_SUCCESS
USER_SIGNUP_REJECTED

LOGIN_REQUEST
LOGIN_SUCCESS
LOGIN_FAILED
LOGIN_REJECTED

PASSWORD_RESET_REQUEST
PASSWORD_RESET_EMAIL_SENT
PASSWORD_RESET_TOKEN_VALIDATION_SUCCESS
PASSWORD_RESET_TOKEN_VALIDATION_FAILED
PASSWORD_RESET_SUCCESS
PASSWORD_RESET_FAILED
```


---

# Google OAuth Logging

Google OAuth events include:

```text
OAUTH2_LOGIN_SUCCESS
OAUTH2_LOGIN_FAILED
OAUTH2_LOGIN_REJECTED

OAUTH2_EXISTING_USER_LOGIN
OAUTH2_USER_CREATED

OAUTH2_APPLICATION_SESSION_CREATED

OAUTH2_REDIRECT_SUCCESS
OAUTH2_FAILURE_REDIRECT

OAUTH2_TEMP_SESSION_CLEARED
```

OAuth access tokens and application JWT values are never logged.


---

# Reminder Logging

Reminder processing events include:

```text
POLICY_REMINDER_JOB_STARTED
POLICY_REMINDER_OVERDUE_ASSIGNMENTS_FOUND
POLICY_REMINDER_JOB_COMPLETED

POLICY_REMINDER_DUE
POLICY_REMINDER_SENT
POLICY_REMINDER_DELIVERY_FAILED

POLICY_REMINDER_SKIPPED
POLICY_REMINDER_ASSIGNMENT_ACKNOWLEDGED
POLICY_REMINDER_ESCALATION_FALLBACK
```


---

# Notification Logging

Notification events include:

```text
NOTIFICATION_LIST_REQUEST
NOTIFICATION_LIST_COMPLETED

NOTIFICATION_UNREAD_COUNT_REQUEST
NOTIFICATION_UNREAD_COUNT_COMPLETED

NOTIFICATION_MARK_READ_REQUEST
NOTIFICATION_MARKED_READ
NOTIFICATION_MARK_READ_FAILED

NOTIFICATION_AUTHENTICATION_FAILED
```


---

# Compliance Logging

Compliance events include:

```text
COMPLIANCE_DASHBOARD_REQUEST
COMPLIANCE_DASHBOARD_COMPLETED

COMPLIANCE_EMPLOYEES_LOADED
COMPLIANCE_POLICY_CALCULATED
COMPLIANCE_POLICY_SKIPPED

COMPLIANCE_CSV_EXPORT_REQUEST
COMPLIANCE_CSV_EXPORT_COMPLETED

COMPLIANCE_DASHBOARD_REJECTED
COMPLIANCE_REQUEST_REJECTED
```


---

# Global Exception Logging

The Global Exception Handler logs important HTTP and application failures.

Events include:

```text
RESOURCE_NOT_FOUND

DUPLICATE_RESOURCE

UNAUTHORIZED_REQUEST

FORBIDDEN_REQUEST

BAD_REQUEST

VALIDATION_FAILED

MISSING_REQUEST_PARAMETER

REQUEST_PARAMETER_TYPE_MISMATCH

UNHANDLED_EXCEPTION
```

Unexpected exceptions are logged using:

```text
ERROR
```

Expected application problems use:

```text
WARN
```


---

# Sensitive Data Logging Policy

The application must never log:

```text
User passwords

Confirm passwords

BCrypt input passwords

JWT tokens

JWT secrets

Google OAuth access tokens

Google OAuth refresh tokens

Google authorization codes

Google client secrets

Password reset tokens

Password reset URLs containing tokens

Database passwords

Mail passwords

Authorization headers

Bearer tokens

Authentication cookies containing credentials
```

Notification/email message bodies should also not be logged unless explicitly required and appropriately protected.


---

# Password Reset Security

Password reset tokens are generated and stored by the backend.

They must never appear in logs.

The password reset flow is:

```text
User requests password reset
        ↓
Backend finds user
        ↓
Old reset tokens removed
        ↓
New secure token generated
        ↓
Token stored
        ↓
Reset email sent
        ↓
User opens reset link
        ↓
Backend validates token
        ↓
Password validated
        ↓
Password BCrypt encoded
        ↓
Token marked as used
        ↓
Password changed email sent
```

Passwords and reset tokens are never logged.


---

# Running the Backend Using Development Profile

From the backend directory:

```powershell
cd backend
```

Run:

```powershell
mvn spring-boot:run
```

The default profile is:

```text
dev
```

when configured using:

```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```


---

# Running With Explicit Development Profile

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
mvn spring-boot:run
```

Alternatively:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```


---

# IntelliJ Environment Configuration

In IntelliJ IDEA:

```text
Run
→ Edit Configurations
→ Application
→ Environment Variables
```

Add the required development environment variables.

Example:

```text
SPRING_PROFILES_ACTIVE=dev

DB_URL=jdbc:postgresql://localhost:5432/hrmspolicies2

DB_USERNAME=your_database_username

DB_PASSWORD=your_database_password

JWT_SECRET=your_secure_jwt_secret

GOOGLE_CLIENT_ID=your_google_client_id

GOOGLE_CLIENT_SECRET=your_google_client_secret

MAIL_USERNAME=your_email

MAIL_PASSWORD=your_mail_app_password

CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Do not put real credentials into Git.


---

# Development Verification

After starting the application, verify:

```text
http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

Also verify:

```text
http://localhost:8080/actuator/info
```

and:

```text
http://localhost:8080/actuator/metrics
```


---

# Verify Structured Logs

Perform a normal login.

Expected console events may include:

```text
event=LOGIN_REQUEST
event=LOGIN_SUCCESS
```

Creating a policy may produce:

```text
event=POLICY_CREATE_REQUEST
event=POLICY_CREATED
```

Submitting a policy may produce:

```text
event=POLICY_SUBMIT_FOR_REVIEW_REQUEST
event=POLICY_SUBMITTED_FOR_REVIEW
```

Publishing may produce:

```text
event=POLICY_PUBLISH_REQUEST
event=POLICY_VERSION_CREATED
event=POLICY_PUBLISHED
```

Acknowledging a policy may produce:

```text
event=POLICY_ACKNOWLEDGEMENT_REQUEST
event=POLICY_ACKNOWLEDGED
```


---

# Development Verification Checklist

Verify the following before committing:

```text
[x] Spring Boot backend starts

[x] Development profile loads

[x] PostgreSQL connection works

[x] Spring Boot Actuator is integrated

[x] /actuator/health works

[x] PostgreSQL health is UP

[x] Mail health is available

[ ] /actuator/info verified

[ ] /actuator/metrics verified

[ ] Login verified after logging changes

[ ] Google OAuth verified after logging changes

[ ] Policy creation verified after logging changes

[ ] Policy approval verified after logging changes

[ ] Policy publishing verified after logging changes

[ ] Policy acknowledgement verified after logging changes

[ ] Structured logs visible in backend console

[ ] Passwords do not appear in logs

[ ] JWT values do not appear in logs

[ ] OAuth tokens do not appear in logs

[ ] Password-reset tokens do not appear in logs

[ ] Real credentials are not staged for Git
```

Update the unchecked items after verifying them locally.


---

# Test Environment Status

The test profile is configured.

Real test database credentials are intentionally not committed.

A dedicated test database can later be configured using:

```text
TEST_DB_URL
TEST_DB_USERNAME
TEST_DB_PASSWORD
```

The absence of real test credentials in Git is intentional and is part of the application's secret-management approach.


---

# Production Environment Status

The production profile is configured to use externalized configuration.

Real production credentials are not stored in the repository.

Production deployment must provide:

```text
DB_URL
DB_USERNAME
DB_PASSWORD

JWT_SECRET

GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET

MAIL_USERNAME
MAIL_PASSWORD

CORS_ALLOWED_ORIGINS
```


---

# Production Security Checklist

Before deploying to production:

```text
[ ] Set SPRING_PROFILES_ACTIVE=prod

[ ] Configure production DB_URL

[ ] Configure production DB_USERNAME

[ ] Configure production DB_PASSWORD

[ ] Configure strong production JWT_SECRET

[ ] Configure production GOOGLE_CLIENT_ID

[ ] Configure production GOOGLE_CLIENT_SECRET

[ ] Configure production MAIL_USERNAME

[ ] Configure production MAIL_PASSWORD

[ ] Configure production CORS_ALLOWED_ORIGINS

[ ] Restrict Actuator exposure

[ ] Hide Actuator health details

[ ] Keep environment values hidden

[ ] Use HTTPS

[ ] Do not commit .env files

[ ] Do not commit passwords or tokens

[ ] Rotate credentials if previously exposed
```


---

# Git Security Verification

Before committing:

```powershell
git status
```

Review every staged file.

Make sure no file contains real:

```text
DB password
JWT secret
Google client secret
Mail password
OAuth token
Password reset token
```


---

# Recommended Git Branch

Recommended feature branch:

```text
feature/logging-actuator-profiles
```


---

# Recommended Commit Messages

Example commits:

```text
Add environment profiles and actuator configuration
```

```text
Add structured logging for policy operations
```

```text
Add authentication reminder and exception logging
```

```text
Document backend monitoring and environment setup
```


---

# Current EOD Implementation Status

Current implementation status:

```text
Structured application logging
COMPLETED

Important business operation logging
COMPLETED

Authentication logging
COMPLETED

OAuth login logging
COMPLETED

Policy workflow logging
COMPLETED

Publishing logging
COMPLETED

Acknowledgement logging
COMPLETED

Retirement logging
COMPLETED

Reminder logging
COMPLETED

Notification logging
COMPLETED

Compliance logging
COMPLETED

Global exception logging
COMPLETED

Password reset email logging
COMPLETED

Spring Boot Actuator
INTEGRATED

Actuator health endpoint
VERIFIED

PostgreSQL health
VERIFIED

Development profile
CONFIGURED AND TESTED

Test profile
CONFIGURED

Production profile
CONFIGURED

Database configuration externalization
CONFIGURED

JWT configuration externalization
CONFIGURED

Google OAuth configuration externalization
CONFIGURED

Mail configuration externalization
CONFIGURED

Sensitive values in logs
AVOIDED

Real test credentials committed
NO

Real production credentials committed
NO
```


---

# EOD Deliverables

The backend EOD work includes:

```text
1. Structured application logging

2. Logging of important business operations

3. Logging of authentication and authorization events

4. Logging of unexpected application errors

5. No passwords or tokens logged

6. Spring Boot Actuator integration

7. Application health endpoint

8. Application information endpoint

9. Application metrics endpoint

10. Development profile

11. Test profile

12. Production profile

13. Externalized database credentials

14. Externalized JWT secrets

15. Externalized Google OAuth credentials

16. Externalized mail credentials

17. Development environment successfully running

18. PostgreSQL health verification

19. Backend documentation updated

20. Git verification before commit/push
```


---

# Final Notes

The development environment is currently the primary environment used for functional verification.

The test and production profiles are configured but require their real environment-specific database and authentication credentials when those environments are deployed.

Sensitive configuration must remain outside source control.

The application should continue using:

```text
INFO
```

for important state-changing operations,

```text
DEBUG
```

for detailed read and diagnostic operations,

```text
WARN
```

for expected business, validation, and access failures,

and:

```text
ERROR
```

for unexpected technical failures.

When adding future functionality, follow the same logging and security rules.
