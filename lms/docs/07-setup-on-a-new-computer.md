# 7. Setting Up on a Brand-New Windows Computer

This guide takes you from **an empty Windows 10/11 computer** to **the whole LMS running**. Follow the steps in order and do the **"Check"** at the end of each step before moving on.

**Time needed:** about 45-60 minutes (most of it downloads and the first build).
**Internet:** needed for downloads and for the first build (Maven downloads libraries). After that it works offline.

> **Tip for every step:** type or paste commands **one at a time** and press Enter. Pasting several lines at once can cause odd errors.

---

## What you will install

| # | Software | Why it is needed |
|---|---|---|
| 1 | **Java JDK 17 or newer** (21 works) | Runs the services |
| 2 | **Apache Maven 3.9+** | Builds the project |
| 3 | **MySQL 8** | The databases |
| 4 | **Erlang** then **RabbitMQ** | The message broker |
| 5 | The **project folder** | The code |

---

## Step 1: Install Java

1. Download a JDK, for example **Eclipse Temurin 21 (LTS)** from https://adoptium.net (Windows, x64, `.msi` installer).
2. Run the installer. On the feature screen, enable **"Add to PATH"** and **"Set JAVA_HOME variable"**.
3. **Close** any open Command Prompt windows and open a **new** one (the PATH only updates in new windows).

**Check:**
```
java -version
```
You should see a version line such as `openjdk version "21.0.x"` (anything 17 or higher is fine).
If you see `'java' is not recognized`, reopen Command Prompt; if it still fails, re-run the installer with "Add to PATH" enabled.

---

## Step 2: Install Maven

1. Download the **Binary zip archive** from https://maven.apache.org/download.cgi (for example `apache-maven-3.9.x-bin.zip`).
2. Unzip it to a simple folder, for example `C:\Maven\apache-maven-3.9.9` (avoid spaces in the path).
3. Add Maven to the PATH:
   1. Press the Windows key, type **"environment variables"**, open **"Edit the system environment variables"**, click **Environment Variables**.
   2. Under **System variables**, select **Path**, click **Edit**, **New**, and add: `C:\Maven\apache-maven-3.9.9\bin`
   3. Click OK on all windows.
4. Check that `JAVA_HOME` exists in the same list (the Java installer normally sets it). If not, add a **New** system variable `JAVA_HOME` = your JDK folder (for example `C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot`).
5. Open a **new** Command Prompt.

**Check:**
```
mvn -version
```
You should see `Apache Maven 3.9.x` and your Java version below it.

---

## Step 3: Install MySQL

1. Download **MySQL Installer for Windows** (or "MySQL Community Server 8.x") from https://dev.mysql.com/downloads/ .
2. In the installer, choose **Server only** (or "Custom" and select *MySQL Server* and *MySQL Shell/Command Line Client*).
3. During configuration:
   - Keep the port **3306**.
   - Set a **root password** and **write it down**.
   - Select **"Configure MySQL Server as a Windows Service"** and **start at system startup**.
4. Finish the installer.

**Check:** press the Windows key, type **MySQL Command Line Client**, open it, enter the root password. You should see a `mysql>` prompt. Type `exit` to leave.

> If the Command Line Client is not installed, open **Services** (Windows key, type `services`), find **MySQL80** (name may vary) and make sure its status is **Running**.

---

## Step 4: Install Erlang and RabbitMQ

RabbitMQ needs Erlang, so install **Erlang first**.

1. **Erlang:** download the Windows 64-bit installer from https://www.erlang.org/downloads and run it with the defaults.
2. **RabbitMQ:** download the Windows installer (`rabbitmq-server-x.y.z.exe`) from https://www.rabbitmq.com/docs/install-windows . Use a RabbitMQ version that is compatible with your Erlang version (the download page tells you). Run it with the defaults.
3. Enable the **management plugin** (the web page for RabbitMQ):
   1. Press the Windows key, type **RabbitMQ Command Prompt (sbin dir)**, right-click it, **Run as administrator**.
   2. Run this **one command**:
      ```
      rabbitmq-plugins enable rabbitmq_management
      ```
4. Find out how RabbitMQ runs on your computer, because it affects Step 8:
   - Open **http://localhost:15672** in a browser. If a login page appears, RabbitMQ is **already running** (the installer usually sets it up as a Windows service that starts automatically). Log in with **guest / guest**.
   - If the page does not open, try in the admin prompt: `net start RabbitMQ`. If that says the service does not exist, RabbitMQ is **not** installed as a service; you will start it with a script in Step 8.

**Check:** you can log in at http://localhost:15672 with `guest` / `guest` and see the RabbitMQ dashboard (once RabbitMQ is running).

> The `guest` account only works from the same computer. That is fine here because everything runs on one machine.

---

## Step 5: Copy the project

1. Copy the whole **`lms`** folder to the new computer (USB drive, cloud storage, zip, or Git).
   - Put it in a simple path **without spaces**, for example `C:\LMS\lms` or `D:\lms`.
   - You do not need to copy any `target` folders (they are rebuilt).
2. Open the folder. You should see: `pom.xml`, `README.md`, `common`, `api-gateway`, `identity-service`, `course-service`, `payment-service`, `enrollment-service`, `progress-service`, `notification-service`, `certificate-service`, `scripts`, `docs`, `infra`, `tests`.

In the rest of this guide, **`<LMS>`** means that folder, for example `C:\LMS\lms`.

---

## Step 6: Create the databases

The services expect 8 empty databases and a MySQL user `lms` (password `lms`). The file `<LMS>\infra\mysql\init.sql` creates all of them.

**Option A: from the MySQL Command Line Client** (works without PATH setup)
1. Open **MySQL Command Line Client** and log in as root.
2. Run (use forward slashes, and your real path):
   ```
   source C:/LMS/lms/infra/mysql/init.sql
   ```

**Option B: from Command Prompt** (needs `mysql` on the PATH)
```
mysql -u root -p < C:\LMS\lms\infra\mysql\init.sql
```
Enter the root password when asked.

**Check** (in the MySQL Command Line Client):
```
SHOW DATABASES;
```
You should see 8 databases: `lms_identity`, `lms_course_write`, `lms_course_read`, `lms_enrollment`, `lms_payment`, `lms_progress`, `lms_certificate`, `lms_notification`. Also check the user:
```
SELECT user FROM mysql.user WHERE user = 'lms';
```
It should return one row. The tables themselves are created automatically the first time each service starts.

---

## Step 7: Check the configuration (usually nothing to change)

The services are preconfigured for a standard local setup. Only change something if your setup differs:

| If your computer has... | Change this |
|---|---|
| MySQL on a **different port** (not 3306) | In each service's `src\main\resources\application.yml`, edit the `jdbc:mysql://localhost:3306/...` URLs (identity, course (two URLs), payment, enrollment, progress, notification, certificate). Tip: search the project for `3306`. |
| A different MySQL user or password | Same files: `username` and `password` (and the matching user in `init.sql`). |
| RabbitMQ with other credentials or port | Same files: `spring.rabbitmq.*` (default `guest` / `guest`, port 5672). |
| Something else already using ports 8080-8087 | Change `server.port` in that service's `application.yml`, **and** the matching route in `api-gateway\src\main\resources\application.yml`, and the `app.course-service-url` values in Enrollment and Progress if you move Course. |

**Never change only one** of the JWT secrets: `app.jwt.secret` must be **identical** in identity, course, enrollment, progress, notification, certificate and api-gateway.

**Check** which ports are free (nothing should print for 8080-8087 right now):
```
netstat -ano | findstr LISTENING | findstr ":808"
```

---

## Step 8: Prepare the start scripts

The scripts are in `<LMS>\scripts`. They find the services automatically, **except** the RabbitMQ ones, which contain a hard-coded path from the original computer.

**If RabbitMQ is running as a Windows service** (Step 4 showed the login page): you do not need the RabbitMQ scripts. Use **`start-services.bat`** (it starts only the 8 services) instead of `start-all.bat`. Skip the rest of this step.

**If RabbitMQ is not a service** (you must start it yourself): edit two files.
1. Find your RabbitMQ `sbin` folder. It is usually like `C:\Program Files\RabbitMQ Server\rabbitmq_server-4.x.y\sbin`.
2. Right-click `scripts\start-rabbitmq.bat`, **Edit** (Notepad), and change this line to your `sbin` folder (keep the quotes):
   ```
   cd /d "D:\RabbitMQ Server\rabbitmq_server-4.3.4\sbin"
   ```
3. Do the same in `scripts\stop-rabbitmq.bat`.
4. Save both. You will use **`start-all.bat`** (it starts RabbitMQ, waits 30 seconds, then starts the services).

---

## Step 9: Build the project

1. Open a **new** Command Prompt.
2. Go to the project root (the folder that contains the main `pom.xml`):
   ```
   cd /d C:\LMS\lms
   ```
3. Build:
   ```
   mvn -q -DskipTests install
   ```
   The **first build takes several minutes** because Maven downloads libraries. **No output at the end means success.**

**Check:** these folders now exist: `<LMS>\identity-service\target`, `<LMS>\api-gateway\target`, and so on.

> Always run this command in the **project root**. Run `mvn spring-boot:run` only **inside** a single service folder (the scripts do that for you).

If you get errors, see section "If something goes wrong" at the end.

---

## Step 10: Start everything

**Case A: RabbitMQ is a Windows service:** make sure it is running (the page at http://localhost:15672 opens), then double-click:
```
<LMS>\scripts\start-services.bat
```

**Case B: RabbitMQ is started by script:** double-click:
```
<LMS>\scripts\start-all.bat
```

If Windows shows a **firewall prompt** for Java, click **Allow access**.

You will see up to 9 windows: RabbitMQ (Case B only), Identity, Course, Payment, Notification, Certificate, Progress, Enrollment, Gateway. Each takes roughly **20-40 seconds** to start (the first time is slower). A service is ready when its window shows a line like:
```
Started IdentityServiceApplication in 14.2 seconds
```
In the **Identity** window you should also see `Admin account created: admin@lms.com`.

**Do not close these windows.** Closing one stops that service.

---

## Step 11: Verify that everything works

Do these checks in order.

**11.1 All ports are listening.** In a new Command Prompt:
```
netstat -ano | findstr LISTENING | findstr ":808"
```
You should see lines for ports **8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087**.

**11.2 RabbitMQ has all the queues.** Open http://localhost:15672 (guest / guest), tab **Queues**. You should see **16 queues**: 8 working queues and 8 matching `.dlq` queues:

| Working queue | Its dead-letter queue |
|---|---|
| `payment.commands` | `payment.commands.dlq` |
| `payment.refunds` | `payment.refunds.dlq` |
| `progress.commands` | `progress.commands.dlq` |
| `enrollment.saga.replies` | `enrollment.saga.replies.dlq` |
| `notification.enrollment-events` | `notification.enrollment-events.dlq` |
| `course.query.projector` | `course.query.projector.dlq` |
| `certificate.course-completed` | `certificate.course-completed.dlq` |
| `course.user-renamed` | `course.user-renamed.dlq` |

**11.3 Tables were created.** In MySQL Command Line Client:
```
SELECT table_schema, COUNT(*) AS tables_created FROM information_schema.tables WHERE table_schema LIKE 'lms\_%' GROUP BY table_schema;
```
Every `lms_...` database should show at least 2 tables (its own tables plus `flyway_schema_history`).

**11.4 The gateway answers.**
```
curl http://localhost:8080/api/catalog
```
Expected (empty catalog on a fresh system):
```
{"items":[],"page":0,"size":10,"totalItems":0,"totalPages":0}
```

**11.5 Login works.**
```
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@lms.com\",\"password\":\"Admin@1234\"}"
```
Expected: JSON with a `token`, `"role":"ADMIN"`.

**11.6 Protection works.**
```
curl -i http://localhost:8080/api/users/me
```
Expected: `HTTP/1.1 401`.

If all six checks pass, the system is running correctly on the new computer.

---

## Step 12: Try the real flow

Follow section **6.5** of [06-running-testing-troubleshooting.md](06-running-testing-troubleshooting.md). It walks through: create instructor and student, build and publish a course, enroll and pay, see the notification, complete the lesson, receive and verify a certificate, and try the failure paths (declined payment, refund).

> Use **Command Prompt** for the `curl` examples. In PowerShell, `curl` behaves differently.

---

## Step 13: Stopping and starting again

**Stop:** press `Ctrl+C` in each service window (or close it). To stop RabbitMQ when started by script: close its window or run `scripts\stop-rabbitmq.bat`.

**Start again later:** MySQL (and RabbitMQ, if it is a service) start with Windows. Then just run `start-services.bat` (or `start-all.bat`). You do **not** need to rebuild or recreate databases; your data stays in MySQL.

**Reset everything to a clean state** (deletes all data!): in the MySQL Command Line Client run `DROP DATABASE` for each `lms_...` database, run `init.sql` again, and restart the services (the tables are recreated and a new admin is seeded). Optionally delete the 14 queues in the RabbitMQ UI; they are recreated automatically.

---

## Optional: copy the existing data from the old computer

A new computer starts with **empty databases**. To bring over users, courses, enrollments and so on:

**On the old computer** (Command Prompt, `mysqldump` is in MySQL's `bin` folder):
```
mysqldump -u root -p --databases lms_identity lms_course_write lms_course_read lms_enrollment lms_payment lms_progress lms_certificate lms_notification > lms-backup.sql
```
**On the new computer**, after Step 6 (and **before** the first start of the services):
```
mysql -u root -p < lms-backup.sql
```
Then start the services as usual. (Messages that were in flight in RabbitMQ are not copied; that is fine for a demo system.)

---

## If something goes wrong

| Problem | What to do |
|---|---|
| `'java'` / `'mvn'` is not recognized | Close and reopen Command Prompt. Re-check Steps 1-2 (PATH, JAVA_HOME). |
| Build fails: cannot download libraries | Check your internet connection or proxy, then run the build again. |
| Build fails with `package ... does not exist` | You are probably not in the project root, or a file was only partly copied. Re-copy the folder and build from the root. |
| `Access denied for user 'lms'` or `Unknown database` | Step 6 was skipped or failed. Run `init.sql` again. |
| `Communications link failure` / cannot connect to MySQL | MySQL is not running. Open **Services**, start **MySQL80**. Check the port in the config (Step 7). |
| A service says `Port 808x was already in use` | That service (or something else) is already running. Close the old window or end the extra `java.exe` in Task Manager. |
| Log keeps showing `Could not send to RabbitMQ, will retry` | RabbitMQ is not running. Start it (Step 4 / Step 10); the waiting messages are sent automatically afterwards. |
| `401` on everything after login | The JWT secret differs between services, or the token expired (60 minutes). Check Step 7 and log in again. |
| Enrollment stays `PENDING` | Payment or Progress is not running, or RabbitMQ is down. Check the windows, then the queues in the RabbitMQ UI and any `.dlq` queue with messages. |
| A new service does not appear in the checks | Look at its window for the first red `ERROR` or `Caused by:` line. That line explains the problem. |
| RabbitMQ page does not open | Run `rabbitmq-server.bat` from the RabbitMQ `sbin` folder (admin prompt) and wait 30 seconds, or check that Erlang installed correctly. |

More troubleshooting (and the list of known limitations) is in [06-running-testing-troubleshooting.md](06-running-testing-troubleshooting.md).

---

## Quick checklist

- [ ] Java 17+ installed (`java -version`)
- [ ] Maven installed (`mvn -version`)
- [ ] MySQL installed and running; `init.sql` executed; 8 `lms_` databases exist
- [ ] Erlang + RabbitMQ installed; management plugin enabled; http://localhost:15672 opens
- [ ] Project folder copied to a path without spaces
- [ ] RabbitMQ script paths edited (only if not running as a service)
- [ ] `mvn -q -DskipTests install` finished without errors, from the project root
- [ ] `start-services.bat` or `start-all.bat` run; every service window shows `Started ...`
- [ ] Checks 11.1 to 11.6 pass
