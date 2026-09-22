# Trichy Estates - frontend

Original HTML/CSS/vanilla-JS UI, now backed by the Spring Boot API in `../backend`.

- Start the backend first (see the root `README.md`), then open this folder with VS Code **Live Server** (port 5501).
- `config.js` holds `API_BASE_URL` (default `http://localhost:8080`). Change it for production.
- `api.js` is the fetch helper; `app.js` is the UI logic.
- Contact numbers shown in the page come from the backend (`CONTACT_PHONE`); the numbers in `index.html` are only fallbacks.
