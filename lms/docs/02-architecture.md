# 2. Architecture

## 2.1 Goals

- Split an LMS into **independent services**, each owning its data.
- Keep multi-service business processes (enrollment + payment + access) **consistent without distributed transactions** (Saga).
- Make browsing the catalog **fast** and independent from editing (CQRS).
- Make the system **resilient**: if RabbitMQ or a service is briefly down, messages are not lost (outbox, durable queues, retries, DLQ).
- Make it easy to **add features** by listening to events (Notification, Certificate).

## 2.2 Big picture

```
 Client (browser / Postman / curl)
        |
        v
 +--------------------------------------------------------------+
 |                      API GATEWAY  :8080                      |
 |  - checks JWT (public: login/register, catalog GET, cert verify)
 |  - routes by URL path   - CORS                                |
 +--+--------+---------+----------+---------+---------+----------+
    |        |         |          |         |         |
 /api/auth  /api/    /api/      /api/     /api/     /api/
 /api/users courses  enrollments progress  notifica- certificates
 /api/admin catalog  /api/admin  quizzes   tions
 /users     /api/admin/enrollments
            courses
    |        |         |          |         |         |
    v        v         v          v         v         v
 Identity  Course   Enrollment  Progress  Notification Certificate
  :8081    :8082     :8084      :8085      :8086       :8087
                         \         ^                 ^        ^
                          \  commands & replies      |events  |events
                           v        |                |        |
                        +-----------+----------------+--------+---+
                        |             RabbitMQ                     |
                        |   lms.commands  lms.events   lms.dlx     |
                        +-----------+------------------------------+
                                    ^
                                 Payment :8083 (messages only, no gateway route)
```

## 2.3 Two kinds of communication

| Kind | Used for | Examples |
|---|---|---|
| **Asynchronous (RabbitMQ messages)** | Anything that changes state across services, or reactions | Payment commands/replies, catalog updates, notifications, certificates |
| **Synchronous (HTTP)** | Quick **reads** that must answer right now | Enrollment asks Course for a course's price; Progress asks Course for a lesson count; Progress asks Course "does this instructor own course X?" |

HTTP calls between services (all go **straight to the Course service**, not through the gateway):

| Caller | Calls | Why |
|---|---|---|
| Enrollment | `GET {course}/api/catalog/{id}` | Get the **real price** and title (never trust a price sent by the client) |
| Progress | `GET {course}/api/catalog/{id}` | Get the number of lessons and the title |
| Progress | `GET {course}/api/courses/{id}` with the instructor's token | Check the instructor owns the course before creating a quiz |
| Course | `GET {progress}/api/progress/my-courses` with the student's token | Check the student has access before returning lesson content |

Because of these, **Enrollment and Progress need the Course service running** for those operations (they return `503` if it is unreachable).

## 2.4 Data ownership

| Service | Database | Tables |
|---|---|---|
| Identity | `lms_identity` | `users` |
| Course (write) | `lms_course_write` | `courses`, `lessons`, `outbox_event` |
| Course (read) | `lms_course_read` | `course_catalog_view` |
| Enrollment | `lms_enrollment` | `enrollments`, `enrollment_sagas`, `outbox_event` |
| Payment | `lms_payment` | `payments`, `outbox_event` |
| Progress | `lms_progress` | `course_access`, `lesson_progress`, `quizzes`, `quiz_questions`, `quiz_options`, `quiz_attempts`, `outbox_event` |
| Notification | `lms_notification` | `notifications` |
| Certificate | `lms_certificate` | `certificates` |

Each also has Flyway's own `flyway_schema_history` table. No service reads another service's database. Ids (like `student_id`, `course_id`) are plain numbers with **no foreign keys across databases**.

## 2.5 Security model

1. **Login** (`POST /api/auth/login`) checks the BCrypt-hashed password and returns a **JWT**.
2. The JWT holds `sub` (user id), `email`, `name`, `role`, and an expiry (60 minutes), signed with a **shared secret**.
3. **Gateway** rejects requests without a valid token (except the public paths below).
4. **Every service validates the token again itself** (defence in depth). A service only trusts what is in the verified token.
5. Each service enforces **role rules** in its `SecurityConfig`.

Public (no token needed): `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/catalog` (+ `/{id}`), `GET /api/certificates/verify/{code}`.

| Role | Can do |
|---|---|
| Anyone | Register, log in, browse/search the catalog, verify a certificate |
| STUDENT | Enroll, see own enrollments, complete lessons, take quizzes, see own notifications and certificates |
| INSTRUCTOR | Create/edit/publish/unpublish own courses, manage lessons, create quizzes for own courses |
| ADMIN | List/search/disable/enable users, suspend/reinstate any course, view all enrollments |
| Any logged-in user | `GET /api/users/me`, `GET /api/notifications/my` |

> The JWT secret must be **identical** in `identity`, `course`, `enrollment`, `progress`, `notification`, `certificate` and `api-gateway` (`app.jwt.secret` in each `application.yml`). If one differs, that service answers `401` to every request.

## 2.6 Design decisions and why

| Decision | Reason |
|---|---|
| **Orchestrated** saga (Enrollment is the conductor) | The flow is one clear sequence. All logic and state is in one place, easy to follow and debug. (Alternative: *choreography*, where services react to each other with no conductor, gets hard to follow as steps grow.) |
| **CQRS only in Course** | The catalog is read far more than it is written, and needs search and paging. Other services do not need the extra complexity. |
| **Outbox in every publisher** | No lost messages when RabbitMQ or the service is down at the wrong moment. |
| **Idempotent consumers** | RabbitMQ can redeliver; duplicates must be harmless. |
| **Payment hidden from the gateway** | Only the saga should trigger charges and refunds. |
| **Price read from Course at enrollment time** | Prevents a client from sending a fake price. |
| **Single `common` module with only message classes** | Services share the *message formats* but no business code or database access. |
| **`int` 0/1 instead of boolean columns** | Avoids a MySQL/Hibernate boolean mapping mismatch with `ddl-auto: validate`. |
| **Flyway + `ddl-auto: validate`** | The database schema is controlled by versioned SQL; Hibernate only checks it matches. |

## 2.7 Reliability features

| Feature | Where | Effect |
|---|---|---|
| Durable exchanges/queues, persistent messages | all RabbitMQ config / relays | Survive a RabbitMQ restart |
| Outbox + relay (every 1 s) | Course, Payment, Enrollment, Progress | No message lost if broker is down; sent when it returns |
| Publisher confirms | relays | Row is marked sent only after RabbitMQ confirms it |
| Listener retry (3 attempts: 1 s, 2 s) | `application.yml` of every consumer | Survives short glitches |
| Dead-letter queues | every consumer queue | Failing messages are parked, not lost, not blocking |
| Idempotent handlers | every consumer | Duplicates are harmless |
| Saga state guards + `@Version` | Enrollment | Duplicate or out-of-order replies are ignored |
