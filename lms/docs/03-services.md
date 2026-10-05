# 3. Services (what each one does and how)

Each service follows the same layout: `controller` (HTTP) -> `service` (logic) -> `repository` (database), plus `security/`, `config/`, and, for services that publish messages, an `outbox/` package.

Endpoint details are in [04-api-reference.md](04-api-reference.md); queues and message formats are in [05-messaging-and-flows.md](05-messaging-and-flows.md).

---

## 3.1 API Gateway (port 8080)

**Purpose.** The only address clients need. Validates the JWT and forwards requests.

**How it works.**
- Built on **Spring Cloud Gateway** (reactive/WebFlux). Do not add `spring-boot-starter-web` to it.
- `JwtAuthGlobalFilter` runs before routing. It lets public requests through (`OPTIONS`, `/api/auth/**`, `GET /api/catalog*`, `GET /api/certificates/verify/*`) and returns `401` for a missing/invalid/expired token on everything else.
- Routes (in `application.yml`):

| URL path | Goes to |
|---|---|
| `/api/auth/**`, `/api/users/**`, `/api/admin/users/**` | Identity :8081 |
| `/api/courses/**`, `/api/catalog/**`, `/api/admin/courses/**`, `/api/learn/**` | Course :8082 |
| `/api/enrollments/**`, `/api/admin/enrollments/**` | Enrollment :8084 |
| `/api/progress/**`, `/api/quizzes/**` | Progress :8085 |
| `/api/notifications/**` | Notification :8086 |
| `/api/certificates/**` | Certificate :8087 |
| *(none)* | Payment :8083 is intentionally not exposed. `/test/**` is not exposed either. |

- CORS is open to all origins (`*`), convenient for development; restrict it for production.

---

## 3.2 Identity Service (port 8081)

**Purpose.** Who are you, and what may you do? Registration, login, JWT creation, and admin control of users.

**Database `lms_identity`:** `users` (`id`, `name`, `email` unique, `password_hash`, `role`, `created_at`, `enabled`).

**How it works.**
- **Register:** validates input, refuses `ADMIN` and duplicate emails, stores a **BCrypt** hash, returns a JWT.
- **Login:** checks the password, then refuses disabled accounts (`403`), otherwise returns a JWT (claims: `sub`=user id, `email`, `role`).
- **Admin seeding:** `AdminSeeder` creates `admin@lms.com` on startup if missing (credentials in `application.yml` under `app.admin`).
- **Admin user management:** list/search users, disable, enable. A disabled user cannot log in. An admin cannot disable their own account.

**Messages.** None. Identity neither sends nor receives messages.

**Key classes:** `AuthController`/`AuthService`, `JwtService`, `JwtAuthFilter`, `SecurityConfig`, `AdminSeeder`, `AdminUserController`, `UserController`.

**Note.** A token already issued to a user stays valid until it expires (60 min) even after the account is disabled, because other services only check the token.

---

## 3.3 Course Service (port 8082): CQRS

**Purpose.** Courses and lessons (instructors) and the public catalog (everyone).

**Two databases (see `DataSourceConfig`):**
- **Write** `lms_course_write`: `courses` (status `DRAFT` / `PUBLISHED` / `SUSPENDED`), `lessons` (with `position` 1, 2, 3...), `outbox_event`.
- **Read** `lms_course_read`: `course_catalog_view`, one flat row per *published* course (`title`, `description`, `price`, `instructor_email`, `lesson_count`).

**Command side (`command/`).** Instructors create/edit courses and lessons, publish and unpublish. Rules:
- Only the **owner** can change a course (`403` otherwise).
- Publish needs at least **1 lesson** and a non-suspended course.
- Lessons can only be **deleted while the course is not published** (students may have progress tied to lesson numbers). Remaining lessons are renumbered.
- Whenever a **published** course changes (edit, lesson added, publish, unpublish, suspend), the service writes a `course.changed` row to the **outbox** in the same transaction.
- The instructor's own views (`/mine`, `/{id}`) read the **write** database because drafts are not in the catalog.
- **Admin:** suspend any published course (removed from catalog, instructor cannot re-publish) and reinstate it (becomes `DRAFT`).

**Query side (`query/`).**
- `CatalogProjector` listens on queue `course.query.projector`. For a `course.changed` message it **upserts** the catalog row; if `removed=true` it **deletes** it.
- `CatalogQueryController` serves search (`q` matches title, description, instructor email), paging, and single-course lookup, reading **only** the read database.

**Messages.** Publishes `course.changed` (via outbox). Consumes its own `course.changed` (projector).

**Course status lifecycle:**
```
 DRAFT --publish--> PUBLISHED --unpublish--> DRAFT
                        |
                     suspend (admin)
                        v
                    SUSPENDED --reinstate (admin)--> DRAFT
```
`publish` on a `SUSPENDED` course is refused.

---

## 3.4 Payment Service (port 8083): internal only

**Purpose.** Charge and refund. **No public API.** It only reacts to messages from the Enrollment saga.

**Database `lms_payment`:** `payments` (`saga_id` **unique**, `enrollment_id`, `student_id`, `amount`, `payment_method`, `status` = `COMPLETED` / `FAILED` / `REFUNDED`), `outbox_event`.

**How it works.**
- `PaymentCommandHandler` handles `ProcessPaymentCommand`. It is a **fake gateway**: `paymentMethod = TEST_FAIL` is declined, anything else succeeds. If the same saga arrives twice, the first result is reused (no double charge). It replies `PaymentCompleted` or `PaymentFailed`.
- `PaymentRefundHandler` handles `RefundPaymentCommand` (the **compensation**). A `COMPLETED` payment becomes `REFUNDED`, then it replies `PaymentRefunded`.
- Replies are written to the outbox and sent to RabbitMQ.

**To use a real provider (Stripe, Razorpay...):** replace the "fake" decision in `PaymentCommandHandler` and the refund in `PaymentRefundHandler`.

---

## 3.5 Enrollment Service (port 8084): the Saga orchestrator

**Purpose.** Let a student enroll in a course and coordinate payment and access across services.

**Database `lms_enrollment`:** `enrollments` (`student_id`, `course_id`, `course_title`, `price`, `status`), `enrollment_sagas` (`saga_id`, `enrollment_id`, `state`, `version`), `outbox_event`.

**How it works.**
1. `POST /api/enrollments`: `EnrollmentService` asks Course for the **real price/title** (HTTP, outside the DB transaction), rejects if already `PENDING`/`CONFIRMED` for that student+course (`409`), then in **one transaction** saves the enrollment (`PENDING`), a saga row (`PAYMENT_PENDING`) and an outbox row with `ProcessPaymentCommand`. Returns `202 Accepted`.
2. `SagaReplyListener` receives replies (queue `enrollment.saga.replies`) and moves the saga forward (see the state table below).
3. On a final result it publishes `enrollment.confirmed` / `enrollment.rejected` / `enrollment.cancelled` for Notification.

**Saga states:**

| Current state | Reply received | Next state | Action |
|---|---|---|---|
| `PAYMENT_PENDING` | `PaymentCompleted` | `ACCESS_PENDING` | send `GrantAccessCommand` to Progress |
| `PAYMENT_PENDING` | `PaymentFailed` | `FAILED` | enrollment `REJECTED`, publish `enrollment.rejected` |
| `ACCESS_PENDING` | `AccessGranted` | `COMPLETED` | enrollment `CONFIRMED`, publish `enrollment.confirmed` |
| `ACCESS_PENDING` | `AccessGrantFailed` | `COMPENSATING` | send `RefundPaymentCommand` to Payment |
| `COMPENSATING` | `PaymentRefunded` | `FAILED` | enrollment `CANCELLED`, publish `enrollment.cancelled` |

Any reply that does not fit the current state (a duplicate, or out of order) is **ignored** (`expect(...)` check). `@Version` stops two simultaneous updates from clashing.

**Enrollment statuses:** `PENDING`, `CONFIRMED`, `REJECTED`, `CANCELLED`. A student can enroll again after `REJECTED` or `CANCELLED`.

**Admin:** list all enrollments, filter by status, paged.

---

## 3.6 Progress Service (port 8085)

**Purpose.** What a student may open and how far they are: access, lesson completion, quizzes, and deciding when a course is **completed**.

**Database `lms_progress`:**
- `course_access`: one row per student+course (unique), `completed_at` set when finished.
- `lesson_progress`: completed lesson numbers per student+course (unique).
- `quizzes` (one per course, `pass_mark`), `quiz_questions`, `quiz_options` (`is_correct` 0/1), `quiz_attempts` (score and `PASSED`/`FAILED`).
- `outbox_event`.

**How it works.**
- **Grant access (saga step 2):** `GrantAccessHandler` receives `GrantAccessCommand`, creates `course_access` (unless it exists) and replies `AccessGranted`. A test-only switch can force `AccessGrantFailed` to exercise the refund path.
- **Lessons:** a student marks lesson *N* complete. Lessons are numbered 1..`lessonCount`; the lesson count comes from the Course catalog (HTTP). Requires access (`403` otherwise); repeating is harmless.
- **Quizzes:**
  - Instructor creates/replaces **one quiz per course** (pass mark 1-100; each question has 2-6 options and **exactly one** correct). Ownership is verified by asking the Course service with the instructor's own token.
  - Students see questions **without** the correct flags, submit answers, and get a graded result. Attempts are unlimited and saved.
- **Course completion rule** (`ProgressService.completeIfReady`): a course is completed when **all lessons are done AND, if the course has a quiz, the quiz is passed**. It is checked after each lesson completion and after each passed quiz. When it first becomes true, `course.completed` is published (once) via the outbox.

**Messages.** Consumes `progress.grant-access`. Publishes saga replies (`AccessGranted`/`AccessGrantFailed`) and `course.completed`.

**Test-only code (remove before real use):** `TestController` (`POST /test/fail-next`, **no authentication**, port 8085 only) and `FailureSwitch`.

---

## 3.7 Notification Service (port 8086)

**Purpose.** Tell students what happened to their enrollment. Purely **event-driven**.

**Database `lms_notification`:** `notifications` (`student_id`, `enrollment_id`, `type`, `message`, unique on `enrollment_id`+`type`).

**How it works.** `EnrollmentEventListener` consumes `enrollment.confirmed`, `enrollment.rejected`, `enrollment.cancelled` from queue `notification.enrollment-events`, builds a message, saves it, and writes `EMAIL (simulated) to student N: ...` to the log. Students read their messages with `GET /api/notifications/my`.

| Event | Message |
|---|---|
| `CONFIRMED` | Welcome! You are now enrolled in "Title". |
| `REJECTED` | Your payment for "Title" was declined. Please try again. |
| `CANCELLED` | We could not unlock "Title", so your payment was refunded. |

Duplicates are ignored (unique key + existence check). To send real email, replace the log line with a mail provider call.

---

## 3.8 Certificate Service (port 8087)

**Purpose.** Issue a certificate when a course is completed; let anyone verify it.

**Database `lms_certificate`:** `certificates` (`certificate_code` UUID unique, `student_id`, `student_email`, `course_id`, `course_title`, `issued_at`; unique on `student_id`+`course_id`).

**How it works.** `CourseCompletedListener` consumes `course.completed` (queue `certificate.course-completed`) and creates one certificate per student+course (duplicates ignored). `GET /api/certificates/my` lists the student's own certificates; `GET /api/certificates/verify/{code}` is **public** and shows the course and a **masked** email (`s***@test.com`).

---

## 3.9 Common module

Shared by all services, containing **only** message formats and constants:
`Exchanges`, `RoutingKeys`, `ReplyTypes`, `SagaReply`, `ProcessPaymentCommand`, `GrantAccessCommand`, `RefundPaymentCommand`, `EnrollmentEvent`, `CourseCompletedEvent`.

If you change one of these classes, rebuild (`mvn -q -DskipTests install` at the root) and restart every service that uses it.
