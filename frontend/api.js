/*
 * Small fetch wrapper around the Trichy Estates REST API.
 * - Adds the "Authorization: Bearer <jwt>" header automatically.
 * - Converts every failure into an ApiError with a user-friendly message.
 * - Calls the registered handler when the server answers 401 (expired/invalid session).
 *
 * Token storage: "Remember me" -> localStorage (survives browser restarts);
 * otherwise sessionStorage (cleared when the tab closes). Passwords are never stored.
 * Security trade-off: any script running on the page can read either storage, which is why
 * app.js escapes all API data before inserting it into the page (XSS is the main risk).
 */
(function () {
  "use strict";

  const cfg = window.TRICHY_CONFIG || {};
  const BASE = String(cfg.API_BASE_URL || "http://localhost:8080").replace(/\/+$/, "");
  const TOKEN_KEY = "trichy-estates-token";
  const USER_KEY = "trichy-estates-user";

  let unauthorizedHandler = null;

  class ApiError extends Error {
    constructor(status, message, errors) {
      super(message);
      this.name = "ApiError";
      this.status = status;        // 0 = network failure
      this.errors = errors || null; // {field: message} for 400/409 validation problems
    }
  }

  // ---------------------------------------------------------------- session storage
  function stores() { return [localStorage, sessionStorage]; }

  function getToken() {
    for (const s of stores()) {
      try { const t = s.getItem(TOKEN_KEY); if (t) return t; } catch (e) { /* storage blocked */ }
    }
    return null;
  }

  function getUser() {
    for (const s of stores()) {
      try {
        const raw = s.getItem(USER_KEY);
        if (raw) return JSON.parse(raw);
      } catch (e) { /* corrupt or legacy value */ }
    }
    return null;
  }

  function saveSession(token, user, remember) {
    clearSession();
    const store = remember ? localStorage : sessionStorage;
    store.setItem(TOKEN_KEY, token);
    store.setItem(USER_KEY, JSON.stringify({ id: user.id, name: user.name, email: user.email, role: user.role }));
  }

  function clearSession() {
    for (const s of stores()) {
      try { s.removeItem(TOKEN_KEY); s.removeItem(USER_KEY); } catch (e) { /* ignore */ }
    }
    // Leftovers from the old localStorage-only demo.
    try {
      localStorage.removeItem("trichy-estates-cart");
      localStorage.removeItem("trichy-estates-favorites");
    } catch (e) { /* ignore */ }
  }

  // ---------------------------------------------------------------- core request
  function friendlyMessage(status, data) {
    const serverMessage = data && typeof data.message === "string" ? data.message : "";
    if (status === 400) return serverMessage || "Please check the details you entered.";
    if (status === 401) return serverMessage || "Please sign in again.";
    if (status === 403) return "You don't have permission to do that.";
    if (status === 404) return serverMessage || "We couldn't find what you were looking for.";
    if (status === 409) return serverMessage || "That conflicts with existing data.";
    if (status >= 500) return "Something went wrong on our side. Please try again in a moment.";
    return serverMessage || "Something went wrong. Please try again.";
  }

  async function request(path, options) {
    const { method = "GET", body, query, auth = true } = options || {};
    const headers = { Accept: "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    const token = getToken();
    if (auth && token) headers.Authorization = "Bearer " + token;

    let url = BASE + path;
    if (query) {
      const params = new URLSearchParams();
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") params.set(key, value);
      });
      const qs = params.toString();
      if (qs) url += "?" + qs;
    }

    let response;
    try {
      response = await fetch(url, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined
      });
    } catch (networkError) {
      throw new ApiError(0, "Can't reach the server. Check your connection and try again.");
    }

    if (response.status === 204) return null;

    let data = null;
    const text = await response.text();
    if (text) { try { data = JSON.parse(text); } catch (e) { /* non-JSON body */ } }

    if (!response.ok) {
      if (response.status === 401 && auth && token && unauthorizedHandler) unauthorizedHandler();
      throw new ApiError(response.status, friendlyMessage(response.status, data), data && data.errors);
    }
    return data;
  }

  // ---------------------------------------------------------------- public API
  window.api = {
    BASE,
    ApiError,
    getToken, getUser, saveSession, clearSession,
    onUnauthorized(fn) { unauthorizedHandler = fn; },

    // auth
    register: (payload) => request("/api/auth/register", { method: "POST", body: payload, auth: false }),
    login: (payload) => request("/api/auth/login", { method: "POST", body: payload, auth: false }),
    forgotPassword: (email) => request("/api/auth/forgot-password", { method: "POST", body: { email }, auth: false }),
    resetPassword: (token, newPassword) =>
      request("/api/auth/reset-password", { method: "POST", body: { token, newPassword }, auth: false }),
    me: () => request("/api/users/me"),

    // public data
    getContact: () => request("/api/config/contact", { auth: false }),
    getProperties: (query) => request("/api/properties", { query, auth: false }),

    // favorites
    getFavorites: () => request("/api/favorites"),
    addFavorite: (id) => request("/api/favorites/" + encodeURIComponent(id), { method: "POST" }),
    removeFavorite: (id) => request("/api/favorites/" + encodeURIComponent(id), { method: "DELETE" }),

    // cart
    getCart: () => request("/api/cart"),
    addToCart: (id) => request("/api/cart/" + encodeURIComponent(id), { method: "POST" }),
    removeFromCart: (id) => request("/api/cart/" + encodeURIComponent(id), { method: "DELETE" }),

    // enquiries
    sendInquiries: (propertyIds, message) =>
      request("/api/inquiries/bulk", { method: "POST", body: { propertyIds, message } })
  };
})();
