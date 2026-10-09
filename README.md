# mood-tracker

ASE Coaching Project. A group measures its mood in anonymous rounds: every member submits one value from -n to +n, and only once everyone has submitted are the lowest, highest and average value shown. Individual values are never revealed.

- `backend/` Spring Boot API (OpenAPI first, H2 database)
- `frontend/` Angular + Angular Material
- `docker-compose.yml` local mail server (Mailpit) for development

Run the backend with `cd backend && ./mvnw spring-boot:run` and the frontend with `cd frontend && npm start` (then open http://localhost:4200).

## Email notifications

The app sends three kinds of email:

| When | Who receives it |
|---|---|
| A member requests a verification code | that member (the code is needed to start a round, submit a mood or add a member) |
| A round is started | all members of the group **except** the one who started it |
| A round is complete (the last member has submitted) | the members who took part in the round and have round-complete notifications switched on (all members, by default) |

The round-complete mail contains the lowest, highest and average value and the number of submissions. It never contains individual values. Both round mails link to the group page.

Mails are sent after the request has been committed and in the background, so a slow or unreachable mail server never makes an API call fail. The exception is the verification code mail: it is sent as part of the request, so if it cannot be delivered the request fails instead of silently losing the code.

### Two modes

The mode is chosen with the property `moodtracker.mail.provider`:

| Mode | What happens | When to use |
|---|---|---|
| `log` (default) | Mails are not sent, they are printed to the application log | everyday development, tests; no mail server needed |
| `smtp` | Mails are sent to an SMTP server | trying the real thing, production |

In `log` mode a verification code shows up in the backend log as `Your verification code is 123456.`, so you can still use the app without any mail server.

### Step by step: try it locally with Mailpit

[Mailpit](https://mailpit.axllent.org) is a local mail server that accepts every message and shows it in a web inbox. Nothing is delivered to real addresses, so you can use any made-up email addresses. It needs Docker.

1. **Start Mailpit** (from the repository root):

   ```bash
   docker compose up -d mailpit
   ```

   SMTP is now listening on port 1025, and the inbox is at http://localhost:8025.

2. **Start the backend with the `smtp` profile:**

   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=smtp
   ```

   The profile switches the provider to `smtp` and points it at `localhost:1025` (no login), which matches Mailpit. No further configuration is needed.

3. **Start the frontend** in a second terminal:

   ```bash
   cd frontend
   npm start
   ```

4. **Create a group** at http://localhost:4200 with at least two members, for example `ann@example.com` and `bob@example.com`.

5. **Start a round.** Enter `ann@example.com` and press **Send code**. Open the Mailpit inbox (http://localhost:8025): the mail "Your Mood Tracker verification code" is there. Enter the 6-digit code and press **Start round**. A code works once.

6. **Check the notification.** The Mailpit inbox now holds "A new mood round has started" for `bob@example.com`. `ann@example.com` does not get one, because she started the round.

7. **Submit moods.** For each member, press **Send code**, read the code from Mailpit, and submit a mood. Every code is single use, so request a new one for each action.

8. **Check the result mail.** When the last member has submitted, each participant receives "Mood round complete" with the lowest, highest and average value.

Stop Mailpit with `docker compose down`.

### Use a real SMTP server

Use the same `smtp` profile and set the connection through environment variables. Nothing secret is stored in the repository.

| Variable | Meaning | Default |
|---|---|---|
| `SMTP_HOST` | SMTP server host | `localhost` |
| `SMTP_PORT` | SMTP server port | `1025` |
| `SMTP_USERNAME` | login user | empty |
| `SMTP_PASSWORD` | login password | empty |
| `SMTP_AUTH` | `true` if the server requires a login | `false` |
| `SMTP_STARTTLS` | `true` to upgrade the connection with STARTTLS | `false` |
| `MAIL_FROM` | sender address of all mails | `noreply@moodtracker.local` |

Example for a provider on the usual submission port 587:

```bash
export SMTP_HOST=smtp.example.com
export SMTP_PORT=587
export SMTP_AUTH=true
export SMTP_STARTTLS=true
export SMTP_USERNAME=my-user
export SMTP_PASSWORD=my-password
export MAIL_FROM=mood-tracker@example.com
export MOODTRACKER_FRONTEND_BASE_URL=https://mood.example.com

cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=smtp
```

`MOODTRACKER_FRONTEND_BASE_URL` (property `moodtracker.frontend-base-url`, default `http://localhost:4200`) is the address of the frontend. The links in the mails are built from it, so set it to the address your members actually use. Many providers only accept a `MAIL_FROM` address they have verified.

### Troubleshooting

- **No mail arrives.** Check that the backend was started with the `smtp` profile; without it mails only appear in the log. Round mails are sent in the background, so look for lines such as `Could not notify …` in the backend log if one is missing.
- **"Unexpected server error" (HTTP 500) when pressing Send code.** The code mail is sent as part of the request, so a refused or unreachable SMTP server shows up there; the backend log has the details. Check `SMTP_HOST`, `SMTP_PORT` and the login settings.
- **Nobody can switch off the round-complete mail yet.** Each member has a setting for it, which is on by default and is respected when sending, but there is not yet an API or screen to change it.

### Tests

The backend tests run without a mail server. A real SMTP conversation is covered by an in-process test server (GreenMail): `SmtpMessageSenderTest` checks the delivery of a single mail, and `NotificationIntegrationTest` runs a complete round through the API and checks who receives which mail. Run them with `cd backend && ./mvnw test`.

## Next steps

In suggested order:

1. **Enforce the round rules in the database.** Add a unique constraint on (round, member), make sure a group can have at most one open round, and add optimistic locking. Today these rules are checked only in the domain code, so two simultaneous requests could produce a duplicate submission or two open rounds.
2. **Keep data across restarts.** Switch H2 from in-memory to file mode (or use another database) and store verification codes durably. Currently all groups and codes are lost when the backend restarts.
3. **Limit wrong-code attempts.** A wrong verification code does not use up the code, so a 6-digit code can be brute-forced within its 10 minutes by someone who knows the group id and a member's email. Count failed attempts and invalidate the code after a few.
4. **Rate-limit code requests.** The endpoint always answers 204 and mails every real member it is asked for, so with a real SMTP server someone could flood a member's inbox. Limit requests per email and per client.
5. **Let members choose round-complete notifications.** Each member has a `notifyOnRoundComplete` flag that is respected when mails are sent, but nothing can change it. Add an endpoint (confirmed with a verification code, like the other member actions) and a toggle on the group page.
6. **Restrict read access to members.** Anyone with a group id can see its members' emails, the open round and the history. Require proof of membership to read them.
7. **Add a creation date to groups** (`createdAt`) and show it where it is useful.
8. **Add more notification channels** next to email by implementing the `MessageSender` port.
9. **Decide when a round begins.** A round starts with an explicit "start round" action, which is also what triggers the "round started" mail. Consider letting it begin with the first submission instead, and move the notification trigger accordingly.
10. **Smaller items:** purge expired verification codes, package the frontend for production (serve the built app and set up CORS), add end-to-end tests, and extend this README beyond the mail setup.
