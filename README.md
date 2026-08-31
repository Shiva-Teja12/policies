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