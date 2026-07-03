# Test Platform

A basic online test platform: admin creates tests and approves students one-by-one
or all at once; students join with just their name and phone number (no account),
take the test full-screen with anti-cheating checks, and get an instant score.

```
testplatform/
├── backend/     Spring Boot + PostgreSQL (Java 17)
└── frontend/    Plain HTML / CSS / JS (no build step)
```

## 1. Backend setup

**Requirements:** Java 17, Maven, PostgreSQL running locally.

1. Create the database:
   ```sql
   CREATE DATABASE testplatform;
   ```
2. Edit `backend/src/main/resources/application.properties` if your Postgres
   username/password/port differ from the defaults (`postgres` / `postgres` / `5432`).
3. Change the admin login while you're in there:
   ```properties
   admin.username=admin
   admin.password=admin123
   ```
4. Run it:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   The API starts on `http://localhost:8080`. Tables are created automatically
   (`spring.jpa.hibernate.ddl-auto=update`).

## 2. Frontend setup

The frontend is plain static files — no npm install needed. It just needs to be
served over `http://` (not opened as a `file://` path) so the browser's
full-screen and fetch APIs behave correctly.

```bash
cd frontend
python3 -m http.server 5500
```

Then open:
- **Admin:** `http://localhost:5500/admin-login.html`
- **Student:** `http://localhost:5500/index.html?test=<TEST_ID>`

If your backend isn't on `localhost:8080`, update `API_BASE` at the top of
`frontend/js/api.js`.

## 3. How it works

### Admin
1. Log in at `admin-login.html` with the credentials from `application.properties`.
2. **Create Test** — set a title, description and duration, then add questions either by:
   - **Pasting JSON** (see format below), or
   - **Uploading a `.txt` file** (see format below).
3. Copy the generated student link (`index.html?test=<id>`) and share it.
4. **Waiting Room** — as students join, approve them one at a time or hit
   **Approve All**. Approving starts their timer.
5. **Results** — see every attempt's status, score and how it ended
   (manual submit, time up, full-screen exit, or too many tab switches).

### Student
1. Open the shared link, enter name + contact number, and request to join.
2. Wait for admin approval (auto-polls every few seconds).
3. Once approved, read the instructions and click **Enter Full-Screen & Start Test**.
4. Answer questions (OMR-style bubble selection), navigate via the question grid,
   and submit — or it auto-submits on time-up, full-screen exit, or repeated tab switching.

## 4. Question formats

### JSON (paste into the "Paste JSON" box)
```json
[
  {
    "text": "What is the size of an int in Java?",
    "optionA": "16 bit",
    "optionB": "32 bit",
    "optionC": "64 bit",
    "optionD": "8 bit",
    "correctOption": "B"
  },
  {
    "text": "Which keyword prevents inheritance in Java?",
    "optionA": "static",
    "optionB": "protected",
    "optionC": "final",
    "optionD": "const",
    "correctOption": "C"
  }
]
```

### Text file upload (`.txt`)
One block per question, separated by a blank line:
```
Q: What is the size of an int in Java?
A) 16 bit
B) 32 bit
C) 64 bit
D) 8 bit
ANSWER: B

Q: Which keyword prevents inheritance in Java?
A) static
B) protected
C) final
D) const
ANSWER: C
```
`ANSWER:` must be `A`, `B`, `C` or `D`.

## 5. Anti-cheating behavior

- **Full-screen enforced:** the test only starts once full-screen is entered.
  Exiting full-screen immediately auto-submits the test.
- **Tab/window switch detection:** switching away shows a warning (2 warnings
  allowed); the 3rd switch auto-submits the test.
- **Time limit:** the timer starts the moment the admin approves the student
  and cannot be paused; it auto-submits at zero.
- Every submission records how it ended (`MANUAL`, `TIME_UP`, `FULLSCREEN_EXIT`,
  `TAB_SWITCH_LIMIT`) — visible to the admin in Results.

## 6. Notes / things to change before real-world use

- Admin auth is a single hardcoded username/password with an in-memory session
  token — fine for one instance, not for production or multiple admins.
  It resets whenever the backend restarts.
- There's no rate limiting or CAPTCHA on the student join endpoint.
- CORS is wide open (`allowedOriginPatterns("*")`) for local development;
  lock this down in `WebConfig.java` before deploying publicly.

## Credits

This platform was developed by **Azhar Khan**.
