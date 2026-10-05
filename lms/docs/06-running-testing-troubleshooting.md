# 6. Running, Testing, Troubleshooting

## 6.1 Prerequisites

| Tool | Notes |
|---|---|
| Java 17 or newer | Tested with Temurin 21 |
| Maven 3.9+ | `mvn -version` |
| MySQL 8 | Running locally on port 3306 |
| Erlang + RabbitMQ | RabbitMQ 4.x with the management plugin (`rabbitmq-plugins enable rabbitmq_management`) |

Setting everything up on a brand-new computer? Follow [07-setup-on-a-new-computer.md](07-setup-on-a-new-computer.md).

## 6.2 One-time setup

1. **Create the databases and the app user.** Run `infra/mysql/init.sql` in MySQL (it creates `lms_identity`, `lms_course_write`, `lms_course_read`, `lms_enrollment`, `lms_payment`, `lms_progress`, `lms_certificate`, `lms_notification`, and a MySQL user `lms` / `lms`).
2. **Enable the RabbitMQ management plugin** (once), then start RabbitMQ.
3. **Build** from the project root:
   ```
   mvn -q -DskipTests install
   ```
   No output means success. Run Maven **from the root folder** to build, but run `mvn spring-boot:run` only **inside a service's folder** (or just use the scripts).

Tables are created automatically when each service starts (Flyway).

## 6.3 Starting and stopping

**Easiest:** double-click `scripts\start-all.bat`. It starts, in order: RabbitMQ (waits 30 s), Identity, Course, Payment, Notification, Certificate, Progress, Enrollment, Gateway.

| Script | Starts |
|---|---|
| `start-all.bat` | everything |
| `start-services.bat` | the 8 services only (use when RabbitMQ is already running, e.g. as a Windows service) |
| `start-rabbitmq.bat`, `stop-rabbitmq.bat` | RabbitMQ (edit the path inside if yours differs) |
| `start-identity.bat`, `start-course.bat`, `start-payment.bat`, `start-enrollment.bat`, `start-progress.bat`, `start-notification.bat`, `start-certificate.bat`, `start-gateway.bat` | one service each |

Wait for `Started XxxApplication` in each window. Stop a service with `Ctrl+C` in its window.

**After changing code:** stop the changed service(s), run `mvn -q -DskipTests install` at the root, start them again. If you changed the `common` module, restart **every** service.

## 6.4 Ports to remember

| 8080 gateway (use this) | 8081 identity | 8082 course | 8083 payment | 8084 enrollment | 8085 progress | 8086 notification | 8087 certificate | 15672 RabbitMQ UI |
|---|---|---|---|---|---|---|---|---|

## 6.5 Trying it out (Windows Command Prompt)

> In **Command Prompt**, use the `curl` lines below. In **PowerShell**, `curl` is a different command: use `Invoke-RestMethod` instead.
> Replace `TOKEN` placeholders with the `token` value from the matching login. After any change, **wait 3 seconds**: messages need a moment (eventual consistency).

**1. Create accounts and log in**
```
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"name\":\"Teacher\",\"email\":\"teacher@test.com\",\"password\":\"Pass@1234\",\"role\":\"INSTRUCTOR\"}"
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"name\":\"Student\",\"email\":\"student@test.com\",\"password\":\"Pass@1234\",\"role\":\"STUDENT\"}"
```
Each returns a token. Later: `POST /api/auth/login` with `{"email":...,"password":...}`.

**2. Instructor builds a course** (teacher token)
```
curl -X POST http://localhost:8080/api/courses -H "Authorization: Bearer TEACHER_TOKEN" -H "Content-Type: application/json" -d "{\"title\":\"Java Basics\",\"description\":\"Learn Java\",\"price\":499}"
curl -X POST http://localhost:8080/api/courses/1/lessons -H "Authorization: Bearer TEACHER_TOKEN" -H "Content-Type: application/json" -d "{\"title\":\"Intro\",\"content\":\"Welcome\"}"
curl -X POST http://localhost:8080/api/courses/1/publish -H "Authorization: Bearer TEACHER_TOKEN"
```
(`1` is the `courseId` returned by the first call.)

**3. Browse (no token)**
```
curl "http://localhost:8080/api/catalog?q=java"
```

**4. Student enrolls and pays** (student token)
```
curl -X POST http://localhost:8080/api/enrollments -H "Authorization: Bearer STUDENT_TOKEN" -H "Content-Type: application/json" -d "{\"courseId\":1,\"paymentMethod\":\"TEST_SUCCESS\"}"
curl http://localhost:8080/api/enrollments/my -H "Authorization: Bearer STUDENT_TOKEN"
curl http://localhost:8080/api/notifications/my -H "Authorization: Bearer STUDENT_TOKEN"
```
Expected: `PENDING` immediately, `CONFIRMED` after a few seconds, and a welcome notification.

**5. Learn and get a certificate**
```
curl -X POST http://localhost:8080/api/progress/courses/1/lessons/1/complete -H "Authorization: Bearer STUDENT_TOKEN"
curl http://localhost:8080/api/certificates/my -H "Authorization: Bearer STUDENT_TOKEN"
curl http://localhost:8080/api/certificates/verify/CODE
```
(No quiz on this course, so finishing the lessons completes it.)

**6. Failure paths**
- **Declined payment:** enroll with `"paymentMethod":"TEST_FAIL"` -> enrollment ends `REJECTED`.
- **Refund (compensation):** `curl -X POST http://localhost:8085/test/fail-next`, then enroll with `TEST_SUCCESS` -> ends `CANCELLED`, and in MySQL `SELECT status FROM lms_payment.payments ORDER BY id DESC LIMIT 1;` shows `REFUNDED`.

**7. Quiz.** Use `tests/quiz-create.json` as the body of `POST /api/quizzes/courses/{courseId}` (instructor token, `-d @quiz-create.json`), then follow section 4.9 of the API reference.

**8. Admin** (log in as `admin@lms.com` / `Admin@1234`)
```
curl http://localhost:8080/api/admin/users -H "Authorization: Bearer ADMIN_TOKEN"
curl -X POST http://localhost:8080/api/admin/users/ID/disable -H "Authorization: Bearer ADMIN_TOKEN"
curl http://localhost:8080/api/admin/courses -H "Authorization: Bearer ADMIN_TOKEN"
curl http://localhost:8080/api/admin/enrollments?status=CONFIRMED -H "Authorization: Bearer ADMIN_TOKEN"
```

## 6.6 Looking inside

| What | How |
|---|---|
| Queues, message counts, dead letters | http://localhost:15672 -> **Queues** |
| Saga progress | Enrollment window logs each step; table `lms_enrollment.enrollment_sagas` shows each saga's `state` |
| Unsent messages | `SELECT * FROM lms_course_write.outbox_event WHERE sent_at IS NULL;` (same table name in other service databases) |
| Catalog contents | `SELECT * FROM lms_course_read.course_catalog_view;` |
| Payments | `SELECT * FROM lms_payment.payments;` |
| Simulated emails | **Notification** window, lines starting `EMAIL (simulated)` |
| Certificates | **Certificate** window, or `SELECT * FROM lms_certificate.certificates;` |

## 6.7 Troubleshooting

| Symptom | Likely cause and fix |
|---|---|
| `401` on every request | Token expired (60 min) or the JWT secret differs between services. Log in again; make `app.jwt.secret` identical everywhere. |
| `403` | Wrong role, not your resource, no course access, or disabled account. Check which token you used. |
| `Port ... already in use` | The service is already running (or an old `java.exe` is left). Close the old window or end the process in Task Manager. |
| `Access denied for user 'lms'` | The MySQL user was not created. Re-run `infra/mysql/init.sql`. |
| Service fails with a **Flyway** checksum/validation error | A migration that already ran was edited. Never edit applied migrations; add a new `V` file. |
| Service fails with a **Hibernate schema validation** error | An entity and its table disagree. Check column names and types. |
| Compile error `package jakarta.validation does not exist` | The service's `pom.xml` is missing `spring-boot-starter-validation`. |
| Log: `Could not send to RabbitMQ, will retry` | RabbitMQ is not running. Start it; the outbox will send the waiting messages. |
| Enrollment stays `PENDING` | Payment or Progress is not running, or RabbitMQ is down. Start them, check the queues in the RabbitMQ UI, check `.dlq` queues for failed messages. |
| A change is not visible in the catalog | Wait 3 seconds. If still missing, check that the Course service is running and `course.query.projector` has a consumer. |
| A message was "lost" right after first startup | The queue did not exist yet when it was published (consumer service not started before the sender). Start order matters on the very first run. |
| `503 Course service is not reachable` | Enrollment/Progress cannot reach Course on port 8082. Start it. |
| `'mvn' is not recognized` / build fails from the wrong folder | Run `mvn -q -DskipTests install` in the project root. Run `spring-boot:run` only inside a service folder. |
| PowerShell: `Cannot bind parameter 'Headers'` | You used `curl` in PowerShell. Use Command Prompt, or `Invoke-RestMethod`. |

## 6.8 Current limitations (known)

**Security and operations**
- The **JWT secret is hard-coded** in 7 `application.yml` files and the **admin password** (`Admin@1234`) is a default. Move both to environment variables or a secrets store.
- `POST /test/fail-next` on Progress is **unauthenticated** and test-only: remove `TestController` and `FailureSwitch`.
- A disabled user's already-issued token works until it expires (no token revocation).
- CORS allows every origin. There are no refresh tokens, password reset, or rate limiting.
- Database passwords are plain text in the config files.
- Outbox tables grow forever (no cleanup job).

**Features**
- Payment is **fake** and notifications are **log lines** (no real provider).
- One quiz per course, single-correct-answer questions only.
- Quizzes added after completion are not applied retroactively; lessons can be deleted only while the course is a draft.
- While a course is unpublished or suspended, enrolled students cannot complete lessons (Progress reads the catalog).
- Free courses still go through the payment step.
- The catalog search uses `LIKE` (fine for small data; use full-text or Elasticsearch for large catalogs).

**Engineering**
- **No automated tests**: everything was verified by hand. Adding tests for the saga failure paths is the most valuable next step (Testcontainers for MySQL and RabbitMQ).
- No Dockerfiles / deployment setup, no distributed tracing or metrics.
- The outbox code is **copied** into four services; it could become a shared library.
- Rare reordering of `course.changed` messages after a retry could briefly show stale catalog data.

## 6.9 Ideas for next steps

1. Automated tests (saga: happy path, declined payment, refund; outbox with RabbitMQ down).
2. Move secrets to environment variables; remove the test endpoint.
3. Real email (SMTP/SendGrid) in Notification; real payments in Payment.
4. Certificate-ready notification: Certificate publishes `certificate.issued`, Notification listens.
5. A web frontend that calls the gateway.
6. Dockerfiles + `docker-compose` for the whole system; add tracing (Micrometer/Zipkin) with a correlation id on every message.

## 6.10 How to add a new service (checklist)

1. Create a Maven module, add it to the parent `pom.xml`, depend on `common`.
2. Create its database (`lms_<name>`) and grant the `lms` user access (`init.sql`).
3. Copy the `security/` package (same JWT secret) and write a `SecurityConfig` with the roles needed.
4. Add Flyway migrations in `db/migration`, and set `spring.jpa.hibernate.ddl-auto: validate`.
5. To **listen** to an event: add a queue + dead-letter queue + binding in a `RabbitConfig`, and a `@RabbitListener` that is **idempotent**.
6. To **publish**: copy the `outbox/` package and `outbox_event` table; call `OutboxWriter.add(...)` inside your transaction.
7. Add a route in the gateway `application.yml` (and a start script in `scripts/`).
8. Document the endpoints and messages here.
