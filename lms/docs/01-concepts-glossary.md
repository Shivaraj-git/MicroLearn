# 1. Concepts and Glossary (plain English)

Read this first if any term in the other documents is unfamiliar.

---

## Building blocks

**Monolith vs microservices.** A monolith is one big program that does everything. This project instead has **8 small programs (services)**, each doing one job (login, courses, payments...). They can be started, changed and scaled separately.

**Database per service.** Each service has its **own database** and **never reads another service's tables**. If Enrollment needs a course price, it *asks* the Course service. This keeps services independent.

**REST API.** A way to talk to a service over HTTP: `GET /api/catalog` (read), `POST /api/courses` (create), `PUT` (update), `DELETE` (remove). Data travels as JSON.

**API Gateway.** One front door (port 8080). Clients only know this address. It checks the login token and forwards the request to the right service. Without it, a frontend would need to know every service's port.

**JWT (JSON Web Token).** A signed text string you receive after logging in. You send it with each request (`Authorization: Bearer <token>`). It contains your user id, email and role, and is signed with a secret so it cannot be forged. Here it expires after **60 minutes**.

**Roles.** `STUDENT`, `INSTRUCTOR`, `ADMIN`. They decide what each endpoint allows.

**Flyway migration.** A numbered SQL file (`V1__...sql`, `V2__...sql`) that creates or changes tables. Flyway runs unapplied files automatically at startup and remembers which ran. Never edit a migration that already ran: add a new one.

**JPA / Entity.** A Java class mapped to a database table (e.g. `User` to `users`). Spring Data repositories read and write them.

---

## Messaging (RabbitMQ)

**Message broker (RabbitMQ).** A "post office" for programs. A service drops a message in; RabbitMQ delivers it to whoever should receive it, even if that service is briefly down.

**Exchange.** Where messages are sent first. It decides which queues get a copy. This project has `lms.events`, `lms.commands`, and `lms.dlx`.

**Routing key.** A label on a message, like `payment.process` or `course.completed`. Queues say which labels they want.

**Queue.** A mailbox that holds messages for one consumer, for example `payment.commands`. A **binding** connects an exchange + routing key to a queue.

**Command vs event.**
- A **command** says *"do this"* and has one receiver (e.g. `ProcessPaymentCommand`).
- An **event** says *"this happened"* and anyone interested can listen (e.g. `CourseCompleted`). The sender does not know or care who listens.

**Dead-letter queue (DLQ).** Where a message ends up after it keeps failing (here: after 3 attempts). It keeps bad messages for inspection instead of blocking everything.

**At-least-once delivery.** A message may arrive **more than once** (never zero, if things work). So every consumer must handle duplicates safely (see *idempotency*).

---

## Patterns used

**CQRS (Command Query Responsibility Segregation).** Split **writing** from **reading**.
- *Write side* (`lms_course_write`): instructors create/edit courses in normal, structured tables.
- *Read side* (`lms_course_read`): one flat table, `course_catalog_view`, built for fast browsing and search.
- A message (`course.changed`) keeps the read side up to date. Students only ever read the catalog.
- Trade-off: the catalog updates a moment *after* a change (about 1-3 seconds). This is called **eventual consistency**.

**Saga.** A way to run a business process that spans several services **without one big database transaction**. Each step is a local transaction; services reply with success/failure messages. This project uses **orchestration**: Enrollment is the "conductor" that tells the others what to do next.

**Compensation.** If a later saga step fails, the earlier steps are **undone with a reverse action** (not a rollback). Example: payment succeeded, access grant failed, so a **refund** is requested.

**Transactional outbox.** Problem: if you save to the database and then send a message, a crash between the two loses the message. Solution: save the message **in the same database transaction** as your data, in an `outbox_event` table. A background job (the *relay*, runs every second) sends unsent rows to RabbitMQ and marks them sent. If RabbitMQ is down, rows wait and are sent later.

**Idempotency.** Doing the same thing twice has the same result as doing it once. Needed because of at-least-once delivery. Examples here: a unique constraint (one payment per saga, one certificate per student+course), "insert or update" (upsert) for the catalog, and state checks in the saga.

**Optimistic locking (`@Version`).** The saga row has a version number. If two messages try to change it at the same time, the second one fails and is retried, so state never gets corrupted.

**Event-driven design.** Notification and Certificate react to events. Enrollment and Progress do not know they exist, so you can add new listeners (e.g. reporting) without touching the senders.

**Eventual consistency.** Different services (and the catalog) agree after a short delay, not instantly. Use "wait 3 seconds, then check" when testing.

---

## Quick reference: where each pattern lives

| Pattern | Service / files |
|---|---|
| CQRS | `course-service`: `command/`, `query/`, `CatalogProjector`, `DataSourceConfig` |
| Saga orchestrator | `enrollment-service`: `EnrollmentService`, `saga/SagaReplyListener` |
| Saga participants | `payment-service`: `PaymentCommandHandler`, `PaymentRefundHandler`; `progress-service`: `GrantAccessHandler` |
| Outbox | `outbox/` package in course, payment, enrollment, progress |
| Event listeners | `notification-service`: `EnrollmentEventListener`; `certificate-service`: `CourseCompletedListener` |
| JWT | issued in `identity-service` (`JwtService`), verified in the gateway and in every service |
