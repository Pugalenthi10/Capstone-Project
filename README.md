# Trichy Estates - full-stack real-estate app

```
 Browser (HTML / CSS / vanilla JS, Live Server :5501)
        │  fetch() + "Authorization: Bearer <jwt>"
        ▼
 REST API  (Spring Boot 3, :8080)
        │  Spring Security (JWT, roles) → controller → service → repository
        ▼
 JPA / Hibernate  →  MySQL 8  (schema managed by Flyway)
```

```
trichy-estates/
├── backend/                     Spring Boot API
│   ├── pom.xml
│   ├── .env.example             every setting the backend reads
│   └── src/main/
│       ├── java/com/trichyestates/estatehub/
│       │   ├── controller/  service/  repository/  entity/  dto/
│       │   ├── security/    config/   exception/   util/
│       └── resources/
│           ├── application.properties
│           ├── db/migration/V1__init_schema.sql   tables, FKs, unique constraints, indexes
│           └── seed/properties.json               the original 30 listings
└── frontend/                    the ORIGINAL UI, wired to the API
    ├── index.html  styles.css  app.js   (edited, see "What changed")
    ├── api.js                   fetch helper (new)
    └── config.js                API_BASE_URL (new)
```

## 1. Requirements
| Tool | Version |
|---|---|
| JDK | 17 or newer (21 works) |
| Maven | 3.9+ |
| MySQL | 8.0.16+ (CHECK constraints are enforced from 8.0.16) |
| VS Code + *Live Server* | for the frontend (port 5501, as in `settings.json`) |

## 2. Create the database
```sql
CREATE DATABASE estatehub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'estatehub_user'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
GRANT ALL PRIVILEGES ON estatehub.* TO 'estatehub_user'@'localhost';
```
Tables are created automatically on first start by Flyway (`V1__init_schema.sql`). You never create tables by hand.

## 3. Environment variables
Copy `backend/.env.example` for reference. **Required:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥ 32 chars; the app refuses to start without it).

```bash
# macOS / Linux
export DB_URL='jdbc:mysql://localhost:3306/estatehub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USERNAME=estatehub_user
export DB_PASSWORD='choose-a-strong-password'
export JWT_SECRET="$(openssl rand -base64 48)"
export MAIL_LOG_RESET_LINKS=true          # dev only: prints password-reset links to the console
```
```powershell
# Windows PowerShell
$env:DB_URL="jdbc:mysql://localhost:3306/estatehub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="estatehub_user"; $env:DB_PASSWORD="choose-a-strong-password"
$env:JWT_SECRET="paste-a-random-string-of-48+-characters-here"
$env:MAIL_LOG_RESET_LINKS="true"
```
| Optional | Default | Purpose |
|---|---|---|
| `PORT` | 8080 | server port |
| `JWT_EXPIRATION` | 86400000 | token lifetime in ms (24 h) |
| `FRONTEND_URL` | `http://localhost:5501,http://127.0.0.1:5501` | CORS allow-list (comma separated) |
| `FRONTEND_PUBLIC_URL` | `http://localhost:5501` | base of password-reset links |
| `CONTACT_PHONE` / `CONTACT_WHATSAPP` | `+919876543210` (demo placeholder) | numbers shown in the UI |
| `SEED_ENABLED` | true | seed admin/agent/properties. **Set false in production.** |
| `SEED_ADMIN_PASSWORD`, `SEED_AGENT_PASSWORD` | random | see §7 |
| `SMTP_*` | unset | see §10 |

## 4. Run
```bash
cd backend
mvn spring-boot:run          # http://localhost:8080  -> check http://localhost:8080/api/health
```
Frontend: open `frontend/` in VS Code → right-click `index.html` → **Open with Live Server** → `http://localhost:5501`.
(Do not open `index.html` as a `file://` page: browsers block API calls from it.)

Run the tests: `cd backend && mvn test` (uses in-memory H2, needs no MySQL).

## 5. What changed in the frontend
The design, layout, CSS, animations, cards, drawer, modal and responsive behaviour are untouched. Changes were limited to:

| File | Change |
|---|---|
| `app.js` | Hard-coded property array, demo login, and `localStorage` cart/favorites replaced by API calls. Filtering/sorting now happen on the server. All API text is HTML-escaped before rendering. Loading, empty and error states added. 401 → automatic sign-out. |
| `api.js` (new) | `fetch` wrapper: Bearer header, friendly messages for 400/401/403/404/409/5xx and network failure. |
| `config.js` (new) | `API_BASE_URL`, the only value to change for production. |
| `index.html` | Script tags; a *Create an account* mode (name + mobile) toggled with a link under the sign-in form (registration had no UI before); a reset-password form; `data-contact` attributes so phone/WhatsApp come from the server. |
| `styles.css` | ~40 appended lines for loading/error messages and the mode switch. |

**Behaviour notes:** sign-in is now email + password (the mobile field appears only when creating an account, because the API authenticates by email). "Buy / Contact Seller" records an *enquiry* per shortlisted property (no payment, nothing is marked sold) and then shows the same call/WhatsApp modal.

## 6. Authentication and authorization
1. `POST /api/auth/register` or `/login` returns `{ token, user }`.
2. The frontend stores the token (*Remember me* → `localStorage`, otherwise `sessionStorage`) and sends `Authorization: Bearer <token>` on every call.
3. A filter verifies the JWT signature and expiry, then loads the user from the database, so role changes and deleted accounts take effect immediately. The user id always comes from the token, never from the request body.
4. Passwords are hashed with BCrypt. Login errors are identical for "unknown email" and "wrong password".

| Role | Can |
|---|---|
| USER | browse, favorites, cart, enquiries, own profile |
| AGENT | everything a USER can + create properties; edit/delete **only their own**; see enquiries on their listings and update their status |
| ADMIN | edit/delete any property, see all enquiries, list users, change roles (not their own) |

Self-registration always creates a `USER`. An admin promotes someone with `PATCH /api/admin/users/{id}/role`.

**Token storage trade-off:** JavaScript-readable storage means an XSS bug could steal the token. Mitigations here: escaped rendering, URL validation on `imageUrl`, no inline handlers built from data. `HttpOnly` cookies would remove this risk but need CSRF protection and same-site deployment, a larger change than this project called for.

## 7. Development accounts
With `SEED_ENABLED=true` the first start creates `admin@trichyestates.local` (ADMIN) and `agent@trichyestates.local` (AGENT) and the 30 listings (owned by the agent). No password exists in source code: set `SEED_ADMIN_PASSWORD` / `SEED_AGENT_PASSWORD`, or leave them empty and a random password is printed **once** in the startup log. Seeding is idempotent (users matched by email, properties by `seed_code`). **Disable seeding in production.**

## 8. API reference
Errors are always `{ "success": false, "message": "...", "timestamp": "...", "status": 400, "errors": { "field": "message" } }` (`errors` only for validation/duplicates).

**Auth / health / config** (public)
| Method & path | Body | Result |
|---|---|---|
| `GET /api/health` | | 200 `{status:"success", message:"Trichy Estates backend is running"}` |
| `POST /api/auth/register` | `name, email, mobile, password` | 201 `{token,user}`; 400 validation; 409 duplicate email/mobile |
| `POST /api/auth/login` | `email, password` | 200 `{token,user}`; 401 |
| `POST /api/auth/forgot-password` | `email` | always 200 (no account enumeration) |
| `POST /api/auth/reset-password` | `token, newPassword` | 200; 400 if invalid/expired/used |
| `GET /api/config/contact` | | `{phone, whatsapp}` |

**Properties**
| Method & path | Access | Notes |
|---|---|---|
| `GET /api/properties` and `GET /api/properties/search` | public | Query: `category` (apartment/house/flat/all), `listingType`, `location` (matches locality or city, case-insensitive), `minPrice`, `maxPrice`, `bedrooms`, `bathrooms` (both = *at least*), `sort` (featured, low, high, area, newest), `page` (0-based), `size` (max 100). Returns `{content,page,size,totalElements,totalPages}` |
| `GET /api/properties/{id}` | public | 404 if missing |
| `POST /api/properties` | AGENT, ADMIN | 201; owner = caller. `priceLabel` auto-generated if omitted |
| `PUT /api/properties/{id}` | owner or ADMIN | 403 for other agents |
| `DELETE /api/properties/{id}` | owner or ADMIN | 204 |

**Favorites / Cart** (login required; each user sees only their own)
`GET /api/favorites`, `POST /api/favorites/{propertyId}` (201 new, 200 already there), `DELETE /api/favorites/{propertyId}` (204).
`GET /api/cart`, `POST /api/cart/{propertyId}`, `DELETE /api/cart/{propertyId}`, `DELETE /api/cart` (clear).

**Enquiries**
| Method & path | Access | |
|---|---|---|
| `POST /api/inquiries` | login | `{propertyId, message?}` → 201 |
| `POST /api/inquiries/bulk` | login | `{propertyIds[], message?}` → 201, all-or-nothing (used by the cart) |
| `GET /api/inquiries/my` | login | own enquiries |
| `GET /api/inquiries/agent` | AGENT, ADMIN | enquiries on own listings (with buyer name/email/mobile) |
| `GET /api/inquiries` | ADMIN | all |
| `PATCH /api/inquiries/{id}/status` | owning agent or ADMIN | `{status: NEW|CONTACTED|CLOSED}` |

**Users**: `GET /api/users/me`, `PUT /api/users/me` (`name`, `mobile` only). Admin: `GET /api/admin/users`, `PATCH /api/admin/users/{id}/role`.

## 9. Database
Tables `users`, `properties`, `favorites`, `cart_items`, `inquiries`, `password_reset_tokens` with primary keys, foreign keys (favorites/cart/enquiries cascade on delete; a property whose agent is deleted keeps existing with `agent_id = NULL`), `UNIQUE(email)`, `UNIQUE(mobile)`, `UNIQUE(user_id, property_id)` on favorites and cart, CHECK constraints for enums/positive price and area, and indexes on `location, city, category, listing_type, price`.
Two deliberate deviations from the brief's field list: `properties.seed_code` (stable id that makes seeding idempotent) and `password_reset_tokens.token_hash` (a SHA-256 hash, so a database leak cannot be used to reset passwords).

## 10. Password reset and e-mail
Flow: *Forgot password?* (email typed in the form) → server stores a hashed, single-use token valid 30 min → e-mail contains `FRONTEND_PUBLIC_URL/?reset=<token>` → the page shows the "new password" form. No e-mail provider is bundled. To send real mail, add to `application.properties` (or env): `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`, `spring.mail.properties.mail.smtp.auth=true`, `spring.mail.properties.mail.smtp.starttls.enable=true` (commented template is in the file), and set `MAIL_FROM`. Until then, `MAIL_LOG_RESET_LINKS=true` prints the link to the backend console for local testing.

## 11. Deployment
1. **Database**: provision a cloud MySQL 8 and set `DB_URL/DB_USERNAME/DB_PASSWORD` (use TLS, e.g. `useSSL=true`).
2. **Backend**: `cd backend && mvn -DskipTests package` → `java -jar target/estatehub-1.0.0.jar` (any host that runs Java or Docker: Render, Railway, Fly.io, AWS…). Set `JWT_SECRET` (new random value), `SEED_ENABLED=false`, `FRONTEND_URL=https://your-frontend-domain` (the exact origin, no trailing slash), `FRONTEND_PUBLIC_URL`, `CONTACT_PHONE`, SMTP settings. Serve it over HTTPS.
3. **Frontend**: edit `frontend/config.js` → `API_BASE_URL: "https://your-api-domain"`, then upload the `frontend/` folder to any static host (Netlify, Vercel, GitHub Pages, S3…).
4. Create your first real admin: run once with `SEED_ENABLED=true` and `SEED_ADMIN_PASSWORD` set, log in, change nothing else, then switch seeding off (or promote a registered user directly in SQL: `UPDATE users SET role='ADMIN' WHERE email='you@…'`).

## 12. Security notes
- BCrypt hashes; no plaintext or secrets in source; JWT secret/DB credentials only from the environment.
- CORS: explicit origin allow-list, credentials disabled (never "any origin + credentials").
- Stateless API, so CSRF tokens are not needed (no cookies).
- Every query goes through JPA/bound parameters; LIKE wildcards in search input are escaped.
- Server error pages never include stack traces; unexpected errors are logged and return a generic 500.
- **Not included (recommended before real traffic):** rate limiting / lockout on `/api/auth/*`, HTTPS termination config, e-mail verification at sign-up, refresh tokens, audit logging.

## 13. Troubleshooting
| Symptom | Fix |
|---|---|
| Backend exits: `Could not resolve placeholder 'DB_URL'` (or `JWT_SECRET`) | the environment variable is not set in *that* terminal |
| `JWT_SECRET must be set and at least 32 characters` | use a longer secret |
| `Access denied for user` / `Communications link failure` | wrong DB credentials, MySQL not running, or DB `estatehub` not created |
| Flyway `Unsupported Database` | MySQL older than 8 |
| Browser console: CORS error | the page origin must be in `FRONTEND_URL` (`localhost` and `127.0.0.1` are different origins) |
| UI says "Can't reach the server" | backend not running, or wrong `API_BASE_URL` in `config.js` |
| Login works then instantly signs out | token expired, or `JWT_SECRET` changed since it was issued |
| Port 8080 busy | set `PORT=8081` and update `config.js` |
