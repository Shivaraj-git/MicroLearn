# 5. Messaging and Business Flows

## 5.1 RabbitMQ topology

**Exchanges**

| Exchange | Type | Used for |
|---|---|---|
| `lms.commands` | topic | Saga **commands** ("do this") |
| `lms.events` | topic | **Events** ("this happened") and saga **replies** |
| `lms.dlx` | direct | Dead letters (messages that failed 3 times) |

**Queues, bindings and consumers**

| Queue | Bound to (exchange : routing key) | Consumer | Dead-letter queue |
|---|---|---|---|
| `payment.commands` | `lms.commands : payment.process` | Payment (`PaymentCommandHandler`) | `payment.commands.dlq` |
| `payment.refunds` | `lms.commands : payment.refund` | Payment (`PaymentRefundHandler`) | `payment.refunds.dlq` |
| `progress.commands` | `lms.commands : progress.grant-access` | Progress (`GrantAccessHandler`) | `progress.commands.dlq` |
| `enrollment.saga.replies` | `lms.events : enrollment.saga.reply` | Enrollment (`SagaReplyListener`) | `enrollment.saga.replies.dlq` |
| `notification.enrollment-events` | `lms.events : enrollment.confirmed`, `enrollment.rejected`, `enrollment.cancelled` | Notification | `notification.enrollment-events.dlq` |
| `course.query.projector` | `lms.events : course.changed` | Course (`CatalogProjector`) | `course.query.projector.dlq` |
| `certificate.course-completed` | `lms.events : course.completed` | Certificate | `certificate.course-completed.dlq` |
| `course.user-renamed` | `lms.events : user.renamed` | Course (`UserRenamedListener`) | `course.user-renamed.dlq` |

You can watch all of these live in the RabbitMQ UI: http://localhost:15672 (login `guest` / `guest`), tab **Queues**. A non-zero **Ready** count on a normal queue means a consumer is not running; messages in a `.dlq` queue mean something failed repeatedly.

**Important: start order.** A queue is created when its *consumer* service first starts. If a message is published to an exchange **before** any queue is bound to that routing key, RabbitMQ **drops it**. So start consumers (Payment, Progress, Notification, Certificate) before the services that send to them. `scripts\start-all.bat` already uses a safe order. Once a queue exists (they are durable) it survives restarts.

## 5.2 Message catalog

Messages are JSON. Classes live in the `common` module (except `CourseChangedEvent`, which stays inside the Course service).

| Message | Routing key | Sent by -> received by | Fields |
|---|---|---|---|
| `ProcessPaymentCommand` | `payment.process` | Enrollment -> Payment | `sagaId`, `enrollmentId`, `studentId`, `amount`, `paymentMethod` |
| `RefundPaymentCommand` | `payment.refund` | Enrollment -> Payment | `sagaId`, `enrollmentId` |
| `GrantAccessCommand` | `progress.grant-access` | Enrollment -> Progress | `sagaId`, `enrollmentId`, `studentId`, `courseId` |
| `SagaReply` | `enrollment.saga.reply` | Payment / Progress -> Enrollment | `sagaId`, `type`, `reason` |
| `EnrollmentEvent` | `enrollment.confirmed` / `.rejected` / `.cancelled` | Enrollment -> Notification | `enrollmentId`, `studentId`, `courseId`, `courseTitle`, `status` |
| `CourseCompletedEvent` | `course.completed` | Progress -> Certificate | `studentId`, `studentEmail`, `courseId`, `courseTitle` |
| `UserRenamedEvent` | `user.renamed` | Identity -> Course | `userId`, `email`, `name` |
| `CourseChangedEvent` | `course.changed` | Course -> Course (projector) | `courseId`, `title`, `description`, `price`, `instructorEmail`, `lessonCount`, `removed` |

`SagaReply.type` is one of: `PaymentCompleted`, `PaymentFailed`, `AccessGranted`, `AccessGrantFailed`, `PaymentRefunded`.

## 5.3 How a message travels (outbox relay)

```
 service transaction (one DB commit)
 +-----------------------------------------------+
 | 1. change business data (e.g. enrollment)     |
 | 2. INSERT row into outbox_event               |   both succeed or both roll back
 +-----------------------------------------------+
                       |
        OutboxRelay runs every 1 second
                       |
   3. read unsent rows (oldest first, up to 50)
   4. send to RabbitMQ and wait for the broker's CONFIRM
   5. mark row sent_at = now
        (if RabbitMQ is unreachable: log a warning, keep the row, retry next second)
                       |
                       v
              exchange -> queue -> consumer
                       |
   consumer handler runs in a transaction; if it throws:
   retry after 1 s, then 2 s; after 3 attempts -> dead-letter queue
```

Guarantee: **at-least-once** delivery. Duplicates are possible, so every consumer is idempotent:

| Consumer | How it handles a duplicate |
|---|---|
| Catalog projector | upsert (insert or update) / delete is repeatable |
| Payment (charge) | `saga_id` is unique; existing payment is reused |
| Payment (refund) | refunding an already refunded payment just replies again |
| Progress (grant access) | skips insert if the access row exists |
| Enrollment (saga reply) | state guard ignores replies that do not match the current state; `@Version` prevents concurrent updates |
| Notification | unique `(enrollment_id, type)` plus an existence check |
| Certificate | unique `(student_id, course_id)` plus an existence check |

## 5.4 Flow A: Register and login

```
Client -> Gateway -> Identity: POST /api/auth/register
Identity: validate, BCrypt the password, save user (enabled=1) -> returns JWT
Client -> Gateway: POST /api/auth/login  -> Identity checks password and enabled flag -> JWT
Client keeps the JWT and sends it as "Authorization: Bearer ..." on later calls
Gateway verifies the JWT, forwards to the right service, which verifies it again and checks the role
```

## 5.5 Flow B: Publishing a course (CQRS)

```
Instructor           Course service                         RabbitMQ         Course projector
    | POST publish          |                                    |                    |
    |---------------------->| 1. course.status = PUBLISHED       |                    |
    |                       | 2. outbox row "course.changed"     |  (one transaction) |
    |<----------------------| 200 OK                             |                    |
    |                       | 3. relay (<= 1 s) -- publish ----->|                    |
    |                       |                                    |--- queue --------->|
    |                       |                                    |   4. UPSERT into lms_course_read.course_catalog_view
Student: GET /api/catalog  -> reads ONLY the read database -> sees the course (1-3 s after publish)
```

- **Edit** a published course, **add a lesson**: another `course.changed` (row refreshed).
- **Unpublish** or **suspend**: `course.changed` with `removed=true` (row deleted).
- Drafts never reach the catalog.

## 5.6 Flow C: Enrollment saga

### Happy path

```
Student        Enrollment            Payment            Progress          Notification
  | POST /enrollments                                                            
  |------------->| 1. GET price from Course (HTTP)
  |              | 2. tx: enrollment=PENDING, saga=PAYMENT_PENDING, outbox(ProcessPaymentCommand)
  |<-- 202 ------|
  |              |--- payment.process ---->|
  |              |                         | 3. charge (fake) -> payment COMPLETED
  |              |<-- PaymentCompleted ----|
  |              | 4. saga=ACCESS_PENDING, outbox(GrantAccessCommand)
  |              |------------------ progress.grant-access ------->|
  |              |                                                 | 5. create course_access
  |              |<-------------------- AccessGranted -------------|
  |              | 6. enrollment=CONFIRMED, saga=COMPLETED, publish enrollment.confirmed
  |              |--------------------------------------- enrollment.confirmed --->|
  |                                                                                | 7. save + log "EMAIL (simulated)"
  | GET /enrollments/{id} -> CONFIRMED        GET /notifications/my -> "Welcome! ..."
```

### Failure 1: payment declined (`paymentMethod = TEST_FAIL`)

```
Enrollment --ProcessPaymentCommand--> Payment: status FAILED --PaymentFailed--> Enrollment
Enrollment: enrollment=REJECTED, saga=FAILED, publish enrollment.rejected -> Notification: "payment was declined"
(No money was taken, so nothing to undo.)
```

### Failure 2: access grant fails after payment: **compensation**

```
Enrollment --payment.process--> Payment: COMPLETED --PaymentCompleted--> Enrollment
Enrollment --progress.grant-access--> Progress: FAILS --AccessGrantFailed--> Enrollment
Enrollment: saga=COMPENSATING, send RefundPaymentCommand
Enrollment --payment.refund--> Payment: status REFUNDED --PaymentRefunded--> Enrollment
Enrollment: enrollment=CANCELLED, saga=FAILED, publish enrollment.cancelled
Notification: "We could not unlock ..., so your payment was refunded."
```
This is the core idea of a Saga: **no cross-service rollback**; the earlier step is undone with a reverse action (refund).
To try it yourself: call `POST http://localhost:8085/test/fail-next` (Progress directly), then enroll with `TEST_SUCCESS`.

### Saga state machine

```
                      PaymentFailed
 PAYMENT_PENDING ---------------------------------------> FAILED  (enrollment REJECTED)
      |
      | PaymentCompleted  (send GrantAccessCommand)
      v
 ACCESS_PENDING -- AccessGranted ------------------------> COMPLETED (enrollment CONFIRMED)
      |
      | AccessGrantFailed  (send RefundPaymentCommand)
      v
 COMPENSATING ----- PaymentRefunded ---------------------> FAILED  (enrollment CANCELLED)
```

## 5.7 Flow D: Learning, quiz, and certificate

```
1. Student completes lessons:    POST /api/progress/courses/{id}/lessons/{n}/complete
2. (If the course has a quiz)    GET quiz -> POST attempts (score >= passMark = PASSED)
3. Progress checks "completeIfReady" after every lesson completion AND every passed attempt:
        all lessons done?  AND  (no quiz  OR  quiz passed)?
   If yes and not already completed:
        course_access.completed_at = now
        outbox row "course.completed"   (same transaction)
4. Relay sends course.completed -> queue certificate.course-completed
5. Certificate service: creates a certificate with a random UUID code (once per student+course)
6. Student: GET /api/certificates/my      Anyone: GET /api/certificates/verify/{code}
```

Notes:
- Lessons and quiz can be finished in **either order**; whichever completes the requirement last triggers completion.
- A quiz created **after** a student already completed the course does not re-open it.
- Progress needs the Course service (HTTP) to know the lesson count and title; while a course is unpublished or suspended, lesson completion returns an error.

## 5.8 Flow E: Admin actions

| Action | What happens |
|---|---|
| Disable user | `enabled = 0` in Identity. The user can no longer log in. Their existing token keeps working until it expires. |
| Enable user | `enabled = 1`. |
| Suspend course | Course becomes `SUSPENDED`; `course.changed(removed=true)` removes it from the catalog; the instructor cannot publish it. |
| Reinstate course | Back to `DRAFT`; the instructor publishes it again. |
| List enrollments | Reads Enrollment's own database, filterable by status. |
