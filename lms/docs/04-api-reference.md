# 4. API Reference

Base URL for everything: **`http://localhost:8080`** (the API Gateway).

## 4.1 Conventions

**Authentication.** Send the token from login with every protected request:
```
Authorization: Bearer <token>
```
Tokens expire after **60 minutes**; log in again when you get `401`.

**Roles shown in tables:** `Public` (no token), `Any` (any logged-in user), `STUDENT`, `INSTRUCTOR`, `ADMIN`.

**Error format.** Most errors return JSON:
```json
{ "error": "Course not found" }
```
Validation errors give the first problem: `{ "error": "password: password must be at least 8 characters" }`.
A missing/invalid token returns `401` with an empty body; wrong role returns `403`.

| Code | Meaning |
|---|---|
| 200 / 201 | OK / created |
| 202 | Accepted, still processing (enrollment) |
| 400 | Invalid input or not allowed in the current state |
| 401 | Missing, invalid, or expired token (or wrong login) |
| 403 | Logged in but not allowed (wrong role, not your resource, disabled account) |
| 404 | Not found |
| 409 | Conflict (duplicate) |
| 503 | A service this one depends on (Course) is unreachable |

**Paging.** List endpoints that page return:
```json
{ "items": [ ... ], "page": 0, "size": 10, "totalItems": 25, "totalPages": 3 }
```
Query parameters: `page` (starts at 0), `size` (catalog default 10, max 50; admin lists default 20, max 100).

## 4.2 Endpoint summary

| Method | Path | Who | Service |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Identity |
| POST | `/api/auth/login` | Public | Identity |
| GET | `/api/users/me` | Any | Identity |
| PUT | `/api/users/me` | Any | Identity |
| PUT | `/api/users/me/password` | Any | Identity |
| GET | `/api/admin/users` | ADMIN | Identity |
| POST | `/api/admin/users/{id}/disable` | ADMIN | Identity |
| POST | `/api/admin/users/{id}/enable` | ADMIN | Identity |
| GET | `/api/catalog` | Public | Course |
| GET | `/api/catalog/{id}` | Public | Course |
| POST | `/api/courses` | INSTRUCTOR | Course |
| PUT | `/api/courses/{id}` | INSTRUCTOR (owner) | Course |
| GET | `/api/courses/mine` | INSTRUCTOR | Course |
| GET | `/api/courses/{id}` | INSTRUCTOR (owner) | Course |
| POST | `/api/courses/{id}/lessons` | INSTRUCTOR (owner) | Course |
| PUT | `/api/courses/{id}/lessons/{lessonId}` | INSTRUCTOR (owner) | Course |
| DELETE | `/api/courses/{id}/lessons/{lessonId}` | INSTRUCTOR (owner) | Course |
| POST | `/api/courses/{id}/publish` | INSTRUCTOR (owner) | Course |
| POST | `/api/courses/{id}/unpublish` | INSTRUCTOR (owner) | Course |
| GET | `/api/admin/courses` | ADMIN | Course |
| POST | `/api/admin/courses/{id}/suspend` | ADMIN | Course |
| POST | `/api/admin/courses/{id}/reinstate` | ADMIN | Course |
| POST | `/api/enrollments` | STUDENT | Enrollment |
| GET | `/api/enrollments/my` | STUDENT | Enrollment |
| GET | `/api/enrollments/{id}` | STUDENT (owner) | Enrollment |
| GET | `/api/admin/enrollments` | ADMIN | Enrollment |
| GET | `/api/progress/my-courses` | STUDENT | Progress |
| GET | `/api/progress/courses/{courseId}` | STUDENT (with access) | Progress |
| POST | `/api/progress/courses/{courseId}/lessons/{position}/complete` | STUDENT (with access) | Progress |
| POST | `/api/quizzes/courses/{courseId}` | INSTRUCTOR (owner) | Progress |
| GET | `/api/quizzes/courses/{courseId}/manage` | INSTRUCTOR (owner) | Progress |
| GET | `/api/quizzes/courses/{courseId}` | STUDENT (with access) | Progress |
| POST | `/api/quizzes/courses/{courseId}/attempts` | STUDENT (with access) | Progress |
| GET | `/api/quizzes/courses/{courseId}/attempts` | STUDENT (with access) | Progress |
| GET | `/api/notifications/my` | Any | Notification |
| GET | `/api/certificates/my` | Any | Certificate |
| GET | `/api/certificates/verify/{code}` | Public | Certificate |
| GET | `/api/learn/courses/{courseId}` | STUDENT (with access) | Course |

---

## 4.3 Identity

### POST `/api/auth/register`  (Public)
Request:
```json
{ "name": "Shiva", "email": "shiva@test.com", "password": "Pass@1234", "role": "STUDENT" }
```
Rules: `name` and `email` required, valid email, `password` at least 8 characters, `role` is `STUDENT` or `INSTRUCTOR` (`ADMIN` is refused).

Response `201`:
```json
{ "token": "eyJhbGciOi...", "userId": 1, "name": "Shiva", "role": "STUDENT" }
```
Errors: `400` validation, `403` "Cannot self-register as ADMIN", `409` "Email already registered".

### POST `/api/auth/login`  (Public)
Request: `{ "email": "shiva@test.com", "password": "Pass@1234" }`
Response `200`: same shape as register.
Errors: `401` "Wrong email or password", `403` "Account is disabled".

### GET `/api/users/me`  (Any)
Response `200`: `{ "id": 1, "name": "Shiva", "email": "shiva@test.com", "role": "STUDENT" }`

### GET `/api/admin/users`  (ADMIN)
Query: `q` (matches name or email, optional), `page`, `size`.
Response `200`:
```json
{ "items": [ { "id": 3, "name": "Student Two", "email": "student2@test.com", "role": "STUDENT", "enabled": true } ],
  "page": 0, "size": 20, "totalItems": 1, "totalPages": 1 }
```

### POST `/api/admin/users/{id}/disable`  and  `/enable`  (ADMIN)
Response `200`: the updated user (`{id, name, email, role, enabled}`).
Errors: `400` "You cannot disable your own account", `404` "User not found".

---

## 4.4 Course: public catalog (read side)

### GET `/api/catalog`  (Public)
Query: `q` (searches title, description, instructor email), `page`, `size`.
Example: `GET /api/catalog?q=spring&page=0&size=5`
Response `200`:
```json
{ "items": [ { "courseId": 2, "title": "Spring Boot", "description": "Build APIs",
               "price": 799.00, "instructorEmail": "teacher@test.com", "lessonCount": 1,
               "instructorName": "Teacher" } ],
  "page": 0, "size": 5, "totalItems": 1, "totalPages": 1 }
```
Only **published** courses appear. Changes show up 1-3 seconds after they are made.

### GET `/api/catalog/{id}`  (Public)
Response `200`: one item (same fields as above). `404` "Course not found" if not published.

---

## 4.5 Course: instructor (write side)

All need an `INSTRUCTOR` token. Changing someone else's course returns `403` "This is not your course".

Common response of the change operations (`CourseCommandResponse`):
```json
{ "courseId": 3, "status": "DRAFT", "lessonCount": 2 }
```

### POST `/api/courses`
Request: `{ "title": "Python Basics", "description": "Learn Python", "price": 299 }` (`title` required, `price` required and >= 0).
Response `201`: `{ "courseId": 3, "status": "DRAFT", "lessonCount": 0 }`

### PUT `/api/courses/{id}`
Request: same body as create. Response `200`: `CourseCommandResponse`. If the course is published the catalog is refreshed.

### POST `/api/courses/{id}/lessons`
Request: `{ "title": "Intro", "content": "Hello" }` (`title` required). Added at the end. Response `201`: `CourseCommandResponse`.

### PUT `/api/courses/{id}/lessons/{lessonId}`
Request: `{ "title": "Welcome", "content": "Hello everyone" }`. Response `200`. `404` "Lesson not found".

### DELETE `/api/courses/{id}/lessons/{lessonId}`
Only while the course is **not published**; remaining lessons are renumbered. Response `200`.
Error `400` "Unpublish the course before deleting lessons".

### POST `/api/courses/{id}/publish`
Response `200` with `status: "PUBLISHED"`. Errors `400`: "Add at least one lesson before publishing", "Course is already published", "This course is suspended by an admin".

### POST `/api/courses/{id}/unpublish`
Removes it from the catalog; status becomes `DRAFT`. Error `400` "Course is not published".

### GET `/api/courses/mine`
Response `200`: the instructor's courses (drafts included):
```json
[ { "id": 3, "title": "Python Basics", "description": "Learn Python", "price": 299.00,
    "status": "DRAFT", "lessonCount": 2 } ]
```

### GET `/api/courses/{id}`
Response `200`:
```json
{ "id": 3, "title": "Python Basics", "description": "Learn Python", "price": 299.00, "status": "DRAFT",
  "lessons": [ { "id": 7, "title": "Intro", "content": "Hello", "position": 1 } ] }
```
Use the lesson `id` for edit/delete; use `position` for progress.

---

## 4.6 Course: admin

### GET `/api/admin/courses`  (ADMIN)
All courses, any status: `[ { "id", "title", "price", "status", "instructorEmail", "lessonCount" } ]`

### POST `/api/admin/courses/{id}/suspend`  (ADMIN)
Only for `PUBLISHED` courses. Removes it from the catalog; the instructor cannot re-publish. Error `400` "Only published courses can be suspended".

### POST `/api/admin/courses/{id}/reinstate`  (ADMIN)
`SUSPENDED` becomes `DRAFT`; the instructor must publish again. Error `400` "Course is not suspended".

---

## 4.7 Enrollment

### POST `/api/enrollments`  (STUDENT)
Request:
```json
{ "courseId": 2, "paymentMethod": "TEST_SUCCESS" }
```
`paymentMethod`: the fake gateway **declines `TEST_FAIL`**; any other value succeeds.
Response `202`: `{ "enrollmentId": 5, "courseId": 2, "status": "PENDING", "courseTitle": "Spring Boot" }`
The result arrives a few seconds later: check `GET /api/enrollments/{id}`.
Errors: `404` course not found or not published, `409` "Already enrolled, or enrollment in progress", `503` Course service unreachable.

### GET `/api/enrollments/my`  (STUDENT)
`[ { "enrollmentId": 5, "courseId": 2, "status": "CONFIRMED", "courseTitle": "Spring Boot" } ]`, newest first. Status is `PENDING`, `CONFIRMED`, `REJECTED` or `CANCELLED`.

### GET `/api/enrollments/{id}`  (STUDENT, owner)
One enrollment. `403` if it is not yours, `404` if not found.

### GET `/api/admin/enrollments`  (ADMIN)
Query: `status` (`PENDING` / `CONFIRMED` / `REJECTED` / `CANCELLED`), `page`, `size`.
```json
{ "items": [ { "id": 5, "studentId": 2, "courseId": 2, "courseTitle": "Spring Boot",
               "price": 799.00, "status": "CONFIRMED" } ],
  "page": 0, "size": 20, "totalItems": 1, "totalPages": 1 }
```
Error `400` for an unknown status.

---

## 4.8 Progress

Needs a `STUDENT` token **and access** to the course (access exists after a `CONFIRMED` enrollment). Without access: `403` "You do not have access to this course".

### GET `/api/progress/my-courses`
`[ 2, 5 ]` (ids of courses the student can open).

### GET `/api/progress/courses/{courseId}`
```json
{ "courseId": 2, "completedLessons": [1], "totalLessons": 3, "courseCompleted": false }
```

### POST `/api/progress/courses/{courseId}/lessons/{position}/complete`
Marks lesson number `position` (1..totalLessons) as done. Same response shape as above. Repeating is harmless.
Error `400` "Lesson number must be between 1 and N".
`courseCompleted` becomes `true` when all lessons are done **and** the quiz (if any) is passed.

---

## 4.9 Quizzes (Progress service)

### POST `/api/quizzes/courses/{courseId}`  (INSTRUCTOR, owner)
Creates the course's quiz, or **replaces** it if one exists.
```json
{
  "passMark": 60,
  "questions": [
    { "text": "What is Spring Boot?",
      "options": [ { "text": "A Java framework", "correct": true },
                   { "text": "A database",       "correct": false } ] }
  ]
}
```
Rules: `passMark` 1-100; at least one question; each question has **2 to 6 options and exactly one `correct: true`**.
Response `200` (instructor view, includes answers):
```json
{ "quizId": 1, "passMark": 60,
  "questions": [ { "id": 10, "text": "What is Spring Boot?",
                   "options": [ { "id": 21, "text": "A Java framework", "correct": true },
                                { "id": 22, "text": "A database", "correct": false } ] } ] }
```
Errors: `400` rule violations, `403` "This is not your course", `404` course not found, `503` Course service unreachable.

### GET `/api/quizzes/courses/{courseId}/manage`  (INSTRUCTOR, owner)
Same response as above (with correct answers). `404` "This course has no quiz".

### GET `/api/quizzes/courses/{courseId}`  (STUDENT)
The quiz **without** the `correct` field:
```json
{ "quizId": 1, "passMark": 60,
  "questions": [ { "id": 10, "text": "What is Spring Boot?",
                   "options": [ { "id": 21, "text": "A Java framework" }, { "id": 22, "text": "A database" } ] } ] }
```

### POST `/api/quizzes/courses/{courseId}/attempts`  (STUDENT)
Request (use the real question and option ids):
```json
{ "answers": [ { "questionId": 10, "optionId": 21 } ] }
```
Unanswered questions count as wrong. Response `200`:
```json
{ "attemptId": 4, "scorePercent": 100, "passMark": 60, "passed": true,
  "correctAnswers": 1, "totalQuestions": 1 }
```
Passing can complete the course and trigger the certificate (if all lessons are done).

### GET `/api/quizzes/courses/{courseId}/attempts`  (STUDENT)
`[ { "attemptId": 4, "scorePercent": 100, "status": "PASSED", "createdAt": "2026-..." } ]`, newest first.

---

## 4.10 Notifications

### GET `/api/notifications/my`  (Any logged-in)
```json
[ { "id": 1, "type": "CONFIRMED", "message": "Welcome! You are now enrolled in \"Spring Boot\".",
    "createdAt": "2026-10-04T10:15:30Z" } ]
```
`type` is `CONFIRMED`, `REJECTED` or `CANCELLED`. Newest first.

---

## 4.11 Certificates

### GET `/api/certificates/my`  (Any logged-in)
```json
[ { "code": "3f1c9a52-....", "courseId": 2, "courseTitle": "Spring Boot",
    "holder": "shiva@test.com", "issuedAt": "2026-10-04T10:20:00Z" } ]
```

### GET `/api/certificates/verify/{code}`  (Public)
Same shape, but `holder` is masked (`s***@test.com`). `404` `{ "error": "Certificate not found" }` for an unknown code.

---

## 4.12 Not available through the gateway

| Endpoint | Where | Notes |
|---|---|---|
| `POST /test/fail-next` | Progress, **port 8085 directly** | **Test only, no authentication.** Makes the *next* access grant fail, to exercise the refund path. Remove before real use. |
| Payment service | port 8083 | No HTTP API; it works only through RabbitMQ messages. |

---

## 4.13 Lesson content for students (Course service)

### GET `/api/learn/courses/{courseId}`  (STUDENT, with access)
Returns the lesson text. The Course service asks Progress whether the student has access (forwarding the student's token).
```json
{ "courseId": 2, "title": "Spring Boot",
  "lessons": [ { "position": 1, "title": "Setup", "content": "Install tools" } ] }
```
Errors: `403` no access, `404` course not published (or suspended), `503` Progress unreachable.

---

## 4.14 Field limits (Course service)

| Field | Limit | If exceeded |
|---|---|---|
| Course title | 200 characters | `400` with a message, e.g. `title: Title must be 200 characters or fewer` |
| Course description | 1,000 characters | `400` |
| Lesson title | 200 characters | `400` |
| Lesson content | 50,000 characters (about 8,000 words) | `400` |
| Price | 0 to 99,999,999.99 | `400` |

The lesson content column itself (`MEDIUMTEXT`) can hold far more; the API limit keeps single requests a sensible size. It can be raised by changing the `@Size` on `AddLessonRequest` and `UpdateLessonRequest`.

---

## 4.15 Profile (Identity service)

### PUT `/api/users/me`  (Any logged-in)
Change your own name. Request: `{ "name": "Arjun Kumar" }` (required, up to 100 characters).
Response `200`: a **fresh token** (the name is stored in the token) in the same shape as login:
`{ "token": "...", "userId": 2, "name": "Arjun Kumar", "role": "INSTRUCTOR" }`
Identity announces `user.renamed`; the Course service updates the instructor name on that person's courses and in the catalog within a few seconds.

### PUT `/api/users/me/password`  (Any logged-in)
Request: `{ "currentPassword": "old", "newPassword": "new-password" }` (new: 8 to 72 characters).
Response `200`: `{ "message": "Password changed" }`. Error `400` "Current password is incorrect". Other sessions stay signed in until their token expires.
