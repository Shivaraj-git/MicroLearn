# LMS: Learning Management System (Microservices)

An online learning platform where **instructors** create courses and quizzes, **students** enroll, pay, learn and earn **certificates**, and **admins** manage users and courses.

It is built as **8 small Spring Boot services** that talk through **RabbitMQ** (messages) and a single **API Gateway** (the front door), each with its **own MySQL database**.

| Concept | Where you can see it |
|---|---|
| Microservices, database-per-service | 8 services, 8 MySQL schemas |
| API Gateway + JWT security | `api-gateway`, `identity-service` |
| **CQRS** | `course-service` (write DB, read DB, projector) |
| **Saga** (orchestration + compensation) | `enrollment-service` coordinating `payment-service` and `progress-service` |
| **Event-driven** reactions | `notification-service`, `certificate-service` |
| **Transactional outbox** + idempotent consumers | every service that publishes messages |

---

## Documentation map

| Read this | When you want to know |
|---|---|
| [docs/01-concepts-glossary.md](docs/01-concepts-glossary.md) | What do words like CQRS, Saga, JWT, outbox mean? (plain English) |
| [docs/02-architecture.md](docs/02-architecture.md) | How is the whole system designed, and why? |
| [docs/03-services.md](docs/03-services.md) | What does each service do, what data does it own, how does it work inside? |
| [docs/04-api-reference.md](docs/04-api-reference.md) | Every endpoint: who can call it, request, response, errors |
| [docs/05-messaging-and-flows.md](docs/05-messaging-and-flows.md) | RabbitMQ queues/messages and the step-by-step business flows |
| [docs/06-running-testing-troubleshooting.md](docs/06-running-testing-troubleshooting.md) | How to run, test, fix problems, and what is still missing |
| [docs/07-setup-on-a-new-computer.md](docs/07-setup-on-a-new-computer.md) | **Step-by-step setup on a brand-new Windows computer** (MySQL, RabbitMQ, Java, Maven, then the project) |
| [docs/08-frontend-guide.md](docs/08-frontend-guide.md) | The React frontend: run, build, pages, code map |

---

## Architecture at a glance

```
                          +------------------+
   Browser / Postman ---> |   API Gateway    |  :8080   (checks the JWT, routes requests)
                          +--------+---------+
        +---------+---------+------+-----+----------+-----------+-----------+
        |         |         |            |          |           |           |
   +----v---+ +---v----+ +--v-------+ +--v------+ +-v--------+ +v--------+ +v----------+
   |Identity| | Course | |Enrollment| |Progress | |Notificat.| |Certific.| | (Payment) |
   | :8081  | | :8082  | |  :8084   | | :8085   | |  :8086   | |  :8087  | |  :8083    |
   +---+----+ +---+----+ +----+-----+ +----+----+ +----+-----+ +----+----+ +-----+-----+
       |          |           |            |           |            |            |
     MySQL      MySQL       MySQL        MySQL       MySQL        MySQL        MySQL
   (identity) (write+read) (enrollment) (progress) (notification)(certificate) (payment)

   Services talk to each other through RabbitMQ (messages), not by calling each other's databases.
   Payment has no route in the gateway on purpose: only other services talk to it.
```

## Services and ports

| Service | Port | Purpose | Database |
|---|---|---|---|
| API Gateway | 8080 | Single entry point: JWT check, routing, CORS | none |
| Identity | 8081 | Sign-up, login, JWT, user administration | `lms_identity` |
| Course | 8082 | Courses, lessons, catalog search (CQRS) | `lms_course_write`, `lms_course_read` |
| Payment | 8083 | Charges and refunds (fake gateway), internal only | `lms_payment` |
| Enrollment | 8084 | Enrollment + **Saga orchestrator** | `lms_enrollment` |
| Progress | 8085 | Course access, lesson progress, quizzes | `lms_progress` |
| Notification | 8086 | Reacts to enrollment events, stores messages | `lms_notification` |
| Certificate | 8087 | Issues and verifies certificates | `lms_certificate` |
| RabbitMQ | 5672 / 15672 (UI) | Message broker | n/a |
| MySQL | 3306 | Database server | n/a |

## Tech stack

Java 17+ (tested on 21), Spring Boot 3.3.5, Spring Web, Spring Data JPA (Hibernate), Spring Security, Spring Cloud Gateway (WebFlux), Spring AMQP (RabbitMQ), Flyway (database migrations), jjwt 0.12.6 (JWT), MySQL 8, RabbitMQ 4, Maven (multi-module).

## Quick start (Windows)

1. Make sure **MySQL** and **RabbitMQ** are installed. Create the databases once (see `infra/mysql/init.sql`).
2. Build everything, from the project root:
   ```
   mvn -q -DskipTests install
   ```
3. Double-click **`scripts\start-all.bat`** (starts RabbitMQ and all 8 services, in the right order).
4. Use **http://localhost:8080** for every request.

Full details, test commands and troubleshooting: [docs/06-running-testing-troubleshooting.md](docs/06-running-testing-troubleshooting.md).

## Default account

| Role | Email | Password |
|---|---|---|
| Admin (created automatically on first start) | `admin@lms.com` | `Admin@1234` |

Students and instructors register themselves (`POST /api/auth/register`). **Change the admin password before any real use.**

## Project layout

```
lms/
  pom.xml                 parent Maven file (lists all modules)
  common/                 shared message classes and constants (no business logic)
  api-gateway/            front door
  identity-service/       users, login, JWT, admin user management
  course-service/         courses + catalog (CQRS)
  payment-service/        payments + refunds (internal)
  enrollment-service/     enrollments + Saga orchestrator
  progress-service/       access, lessons, quizzes
  notification-service/   event-driven notifications
  certificate-service/    event-driven certificates
  scripts/                *.bat files to start/stop everything
  tests/                  sample request files (e.g. quiz-create.json)
  infra/mysql/init.sql    creates the 8 databases and the `lms` MySQL user
  docs/                   all documentation
```
