"use strict";

/* ===================================================================
   Trichy Estates - frontend
   Data now comes from the Spring Boot API (see api.js / config.js).
   The hard-coded property list and the localStorage cart/favorites/login
   of the original demo have been replaced by real, per-user server state.
   =================================================================== */

let activeCategory = "all";
let currentSort = "featured";

let properties = [];        // properties currently shown (from GET /api/properties)
let cart = [];              // the signed-in user's cart, as full property objects
let favoriteIds = new Set(); // ids of the signed-in user's favorites
let currentUser = null;
let authMode = "login";     // "login" | "register"
let resetToken = null;      // set when the page is opened from a password-reset e-mail link
let loadRequestId = 0;      // guards against out-of-order responses when filters change quickly
let contact = { phone: "+919876543210", whatsapp: "+919876543210" };
const pending = new Set();  // "fav:12" / "cart:12" keys while a request is in flight

const $ = (id) => document.getElementById(id);

const authScreen = $("authScreen");
const app = $("app");
const loginForm = $("loginForm");
const resetForm = $("resetForm");
const propertyGrid = $("propertyGrid");
const cartButton = $("cartButton");
const cartDrawer = $("cartDrawer");
const drawerBackdrop = $("drawerBackdrop");
const closeCart = $("closeCart");
const cartItems = $("cartItems");
const cartCount = $("cartCount");
const cartSummaryCount = $("cartSummaryCount");
const buyButton = $("buyButton");
const buyModal = $("buyModal");
const modalBackdrop = $("modalBackdrop");
const closeBuyModal = $("closeBuyModal");
const toast = $("toast");
const resultCount = $("resultCount");
const sortSelect = $("sortSelect");
const userChip = $("userChip");
const mobileMenu = $("mobileMenu");
const menuButton = $("menuButton");

$("year").textContent = new Date().getFullYear();

/* ------------------------------------------------------------------
   Helpers
------------------------------------------------------------------- */

// API data is inserted with innerHTML, so every value is escaped first (prevents stored XSS).
function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, (ch) => (
    { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[ch]
  ));
}

// Map the API shape onto the shape the existing card/cart templates were written for.
function normalizeProperty(p) {
  return {
    id: p.id,
    category: String(p.category || "").toLowerCase(),
    title: p.title,
    location: p.city ? `${p.location}, ${p.city}` : p.location,
    price: p.price,
    priceLabel: p.priceLabel,
    beds: p.bedrooms,
    baths: p.bathrooms,
    area: p.area,
    description: p.description,
    image: p.imageUrl || ""
  };
}

let toastTimer;
function showToast(message) {
  clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add("show");
  toastTimer = setTimeout(() => toast.classList.remove("show"), 2600);
}

function setBusy(button, busy) {
  button.disabled = busy;
  button.dataset.busy = busy ? "1" : "";
}

/* ------------------------------------------------------------------
   Contact details (configurable on the server: CONTACT_PHONE)
------------------------------------------------------------------- */

function formatPhone(phone) {
  const m = /^\+91(\d{5})(\d{5})$/.exec(phone);
  return m ? `+91 ${m[1]} ${m[2]}` : phone;
}

function applyContact() {
  const digits = (value) => String(value).replace(/\D/g, "");
  document.querySelectorAll('[data-contact="call"]').forEach((el) => {
    el.href = `tel:${contact.phone}`;
    if (!el.hasAttribute("data-keep-text")) {
      el.textContent = (el.dataset.prefix || "") + formatPhone(contact.phone);
    }
  });
  document.querySelectorAll('[data-contact="whatsapp"]').forEach((el) => {
    el.href = `https://wa.me/${digits(contact.whatsapp)}`;
  });
}

async function loadContact() {
  try {
    const data = await api.getContact();
    if (data && data.phone) {
      contact = { phone: data.phone, whatsapp: data.whatsapp || data.phone };
      applyContact();
    }
  } catch (e) { /* keep the fallback numbers already in the HTML */ }
}

/* ------------------------------------------------------------------
   Authentication (sign in / create account / forgot + reset password)
------------------------------------------------------------------- */

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const mobilePattern = /^[6-9]\d{9}$/;

function clearAuthErrors() {
  ["nameError", "emailError", "mobileError", "passwordError"].forEach((id) => { $(id).textContent = ""; });
}

function setAuthMode(mode) {
  authMode = mode;
  const registering = mode === "register";
  clearAuthErrors();

  $("nameField").classList.toggle("hidden", !registering);
  $("mobileField").classList.toggle("hidden", !registering);
  $("loginMeta").classList.toggle("hidden", registering);

  $("authEyebrow").textContent = registering ? "CREATE YOUR ACCOUNT" : "WELCOME BACK";
  $("authTitle").textContent = registering ? "Join Trichy Estates" : "Sign in to continue";
  $("authSubtitle").textContent = registering
    ? "Create an account to save favorites and shortlist homes."
    : "Enter your details to explore premium properties in Trichy.";
  $("authSubmitLabel").textContent = registering ? "Create account & explore" : "Sign in & explore homes";
  $("authSwitchText").textContent = registering ? "Already have an account?" : "New to Trichy Estates?";
  $("authSwitchButton").textContent = registering ? "Sign in" : "Create an account";
  $("password").autocomplete = registering ? "new-password" : "current-password";
}

function showAuthForm() {
  resetForm.classList.add("hidden");
  loginForm.classList.remove("hidden");
  $("authSwitch").classList.remove("hidden");
  setAuthMode("login");
}

function showResetForm() {
  loginForm.classList.add("hidden");
  $("authSwitch").classList.add("hidden");
  resetForm.classList.remove("hidden");
  $("authEyebrow").textContent = "RESET PASSWORD";
  $("authTitle").textContent = "Choose a new password";
  $("authSubtitle").textContent = "Pick a password you haven't used here before.";
}

function validateAuthForm() {
  clearAuthErrors();
  let valid = true;

  if (authMode === "register" && $("fullName").value.trim().length < 2) {
    $("nameError").textContent = "Enter your full name.";
    valid = false;
  }
  if (!emailPattern.test($("email").value.trim())) {
    $("emailError").textContent = "Enter a valid email address.";
    valid = false;
  }
  if (authMode === "register" && !mobilePattern.test($("mobile").value.trim())) {
    $("mobileError").textContent = "Enter a valid 10-digit Indian mobile number.";
    valid = false;
  }
  if ($("password").value.length < 6) {
    $("passwordError").textContent = "Password must be at least 6 characters.";
    valid = false;
  }
  return valid;
}

// Show server-side validation errors next to the right field when we can.
function showAuthError(err) {
  const fieldToElement = { name: "nameError", email: "emailError", mobile: "mobileError", password: "passwordError" };
  let placed = false;
  if (err.errors) {
    Object.entries(err.errors).forEach(([field, message]) => {
      if (fieldToElement[field]) { $(fieldToElement[field]).textContent = message; placed = true; }
    });
  }
  if (!placed) {
    if (err.status === 401) {
      $("passwordError").textContent = "Incorrect email or password.";
    } else {
      showToast(err.message);
    }
  }
}

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (!validateAuthForm()) return;

  const submit = $("authSubmit");
  const email = $("email").value.trim();
  const password = $("password").value;
  setBusy(submit, true);

  try {
    const data = authMode === "register"
      ? await api.register({
          name: $("fullName").value.trim(),
          email,
          mobile: $("mobile").value.trim(),
          password
        })
      : await api.login({ email, password });

    api.saveSession(data.token, data.user, $("rememberMe").checked);
    loginForm.reset();
    await showApp(data.user);
  } catch (err) {
    showAuthError(err);
  } finally {
    setBusy(submit, false);
  }
});

$("authSwitchButton").addEventListener("click", () => {
  setAuthMode(authMode === "login" ? "register" : "login");
});

$("togglePassword").addEventListener("click", (event) => {
  const password = $("password");
  const isPassword = password.type === "password";
  password.type = isPassword ? "text" : "password";
  event.currentTarget.textContent = isPassword ? "Hide" : "Show";
});

$("forgotPassword").addEventListener("click", async () => {
  clearAuthErrors();
  const email = $("email").value.trim();
  if (!emailPattern.test(email)) {
    $("emailError").textContent = "Enter your email above, then click 'Forgot password?'.";
    $("email").focus();
    return;
  }
  try {
    const result = await api.forgotPassword(email);
    showToast(result.message);
  } catch (err) {
    showToast(err.message);
  }
});

resetForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const password = $("newPassword").value;
  $("newPasswordError").textContent = "";
  $("confirmPasswordError").textContent = "";

  if (password.length < 6) {
    $("newPasswordError").textContent = "Password must be at least 6 characters.";
    return;
  }
  if (password !== $("confirmPassword").value) {
    $("confirmPasswordError").textContent = "Passwords do not match.";
    return;
  }

  const submit = $("resetSubmit");
  setBusy(submit, true);
  try {
    const result = await api.resetPassword(resetToken, password);
    resetToken = null;
    history.replaceState(null, "", window.location.pathname);
    resetForm.reset();
    showAuthForm();
    showToast(result.message);
  } catch (err) {
    $("newPasswordError").textContent = err.message;
  } finally {
    setBusy(submit, false);
  }
});

$("cancelReset").addEventListener("click", () => {
  resetToken = null;
  history.replaceState(null, "", window.location.pathname);
  resetForm.reset();
  showAuthForm();
});

function initialsFor(user) {
  const source = (user && user.name) || (user && user.email ? user.email.split("@")[0] : "");
  return source
    .split(/[\s._-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join("") || "TE";
}

async function showApp(user) {
  currentUser = user;
  authScreen.classList.add("hidden");
  app.classList.remove("hidden");
  userChip.textContent = initialsFor(user);
  userChip.title = user.email ? `Signed in as ${user.email}` : "Signed in";
  window.scrollTo({ top: 0 });

  renderCart();
  await Promise.all([loadProperties(), loadUserState()]);
}

// Clears everything user-specific, then shows the login screen.
function logout(message) {
  api.clearSession();
  currentUser = null;
  cart = [];
  favoriteIds = new Set();
  properties = [];
  propertyGrid.innerHTML = "";
  app.classList.add("hidden");
  authScreen.classList.remove("hidden");
  loginForm.reset();
  showAuthForm();
  mobileMenu.classList.remove("open");
  closeCartDrawer();
  closeModal();
  window.scrollTo({ top: 0 });
  if (message) showToast(message);
}

// The server said our token is invalid or expired.
api.onUnauthorized(() => {
  if (currentUser) logout("Your session has expired. Please sign in again.");
});

$("logoutButton").addEventListener("click", () => logout());
$("logoutButtonMobile").addEventListener("click", () => logout());

/* ------------------------------------------------------------------
   Navigation / filters
------------------------------------------------------------------- */

menuButton.addEventListener("click", () => mobileMenu.classList.toggle("open"));

mobileMenu.querySelectorAll("a").forEach((link) => {
  link.addEventListener("click", () => mobileMenu.classList.remove("open"));
});

document.querySelectorAll(".category-tab").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".category-tab").forEach((tab) => tab.classList.remove("active"));
    button.classList.add("active");
    activeCategory = button.dataset.category;
    $("searchType").textContent =
      activeCategory === "all"
        ? "All Properties"
        : activeCategory.charAt(0).toUpperCase() + activeCategory.slice(1) + "s";
    loadProperties();
  });
});

sortSelect.addEventListener("change", () => {
  currentSort = sortSelect.value;
  loadProperties();
});

$("exploreButton").addEventListener("click", () => {
  $("properties").scrollIntoView({ behavior: "smooth" });
});

/* ------------------------------------------------------------------
   Properties (server-side filtering and sorting)
------------------------------------------------------------------- */

async function loadProperties() {
  const requestId = ++loadRequestId;
  propertyGrid.innerHTML = '<div class="grid-message">Loading properties…</div>';

  try {
    const page = await api.getProperties({ category: activeCategory, sort: currentSort, size: 100 });
    if (requestId !== loadRequestId) return;   // a newer request superseded this one
    properties = page.content.map(normalizeProperty);
    resultCount.textContent = page.totalElements;
    renderProperties();
  } catch (err) {
    if (requestId !== loadRequestId) return;
    properties = [];
    resultCount.textContent = "0";
    propertyGrid.innerHTML = `
      <div class="grid-message">
        ${esc(err.message)}<br />
        <button type="button" class="text-btn" id="retryProperties">Try again</button>
      </div>`;
    $("retryProperties").addEventListener("click", loadProperties);
  }
}

async function loadUserState() {
  try {
    const [favorites, cartData] = await Promise.all([api.getFavorites(), api.getCart()]);
    favoriteIds = new Set(favorites.map((p) => p.id));
    cart = cartData.map(normalizeProperty);
    renderProperties();
    renderCart();
  } catch (err) {
    if (err.status !== 401) showToast(err.message);
  }
}

const isInCart = (id) => cart.some((item) => item.id === id);

function renderProperties() {
  if (!properties.length) {
    propertyGrid.innerHTML = '<div class="grid-message">No properties match this selection yet.</div>';
    return;
  }

  propertyGrid.innerHTML = properties.map((property) => {
    const inCart = isInCart(property.id);
    const favorite = favoriteIds.has(property.id);

    return `
      <article class="property-card">
        <div class="property-image">
          <img
            src="${esc(property.image)}"
            alt="${esc(property.title)} in ${esc(property.location)}"
            data-title="${esc(property.title)}"
            loading="lazy"
          />
          <span class="image-badge">${esc(property.category)}</span>
          <button
            class="favorite-btn ${favorite ? "active" : ""}"
            type="button"
            data-favorite="${property.id}"
            aria-label="Favorite ${esc(property.title)}"
          >${favorite ? "♥" : "♡"}</button>
        </div>

        <div class="property-body">
          <div class="property-location">${esc(property.location)}</div>
          <h3>${esc(property.title)}</h3>
          <p class="property-desc">${esc(property.description)}</p>

          <div class="property-meta">
            <span><b>${esc(property.beds)}</b>Beds</span>
            <span><b>${esc(property.baths)}</b>Baths</span>
            <span><b>${esc(property.area)}</b>sq.ft</span>
          </div>

          <div class="property-footer">
            <div class="price">
              <small>Starting price</small>
              <strong>${esc(property.priceLabel)}</strong>
            </div>
            <button
              class="add-cart ${inCart ? "in-cart" : ""}"
              type="button"
              data-cart="${property.id}"
            >
              ${inCart ? "Added ✓" : "Add to Cart"}
            </button>
          </div>
        </div>
      </article>
    `;
  }).join("");

  // Fallback placeholder if a remote image cannot load (attached in JS, not inline, so titles can't break out).
  propertyGrid.querySelectorAll(".property-image img").forEach((img) => {
    img.addEventListener("error", () => {
      img.onerror = null;
      img.src = "https://placehold.co/900x650/e8e7e1/133b36?text=" + encodeURIComponent(img.dataset.title || "Property");
    }, { once: true });
  });

  propertyGrid.querySelectorAll("[data-cart]").forEach((button) => {
    button.addEventListener("click", () => toggleCart(Number(button.dataset.cart)));
  });
  propertyGrid.querySelectorAll("[data-favorite]").forEach((button) => {
    button.addEventListener("click", () => toggleFavorite(Number(button.dataset.favorite)));
  });
}

/* ------------------------------------------------------------------
   Favorites and cart (saved per user in the database)
------------------------------------------------------------------- */

async function toggleFavorite(id) {
  const key = `fav:${id}`;
  if (pending.has(key)) return;
  pending.add(key);
  try {
    if (favoriteIds.has(id)) {
      await api.removeFavorite(id);
      favoriteIds.delete(id);
    } else {
      await api.addFavorite(id);
      favoriteIds.add(id);
    }
    renderProperties();
  } catch (err) {
    showToast(err.message);
  } finally {
    pending.delete(key);
  }
}

async function toggleCart(id) {
  const key = `cart:${id}`;
  if (pending.has(key)) return;
  pending.add(key);
  try {
    if (isInCart(id)) {
      const removed = cart.find((item) => item.id === id);
      await api.removeFromCart(id);
      cart = cart.filter((item) => item.id !== id);
      showToast(`${removed ? removed.title : "Property"} removed from cart`);
    } else {
      const property = properties.find((item) => item.id === id);
      await api.addToCart(id);
      if (property) cart.push(property);
      showToast(`${property ? property.title : "Property"} added to cart`);
    }
    renderProperties();
    renderCart();
  } catch (err) {
    showToast(err.message);
  } finally {
    pending.delete(key);
  }
}

function renderCart() {
  cartCount.textContent = cart.length;
  cartSummaryCount.textContent = cart.length;
  buyButton.disabled = cart.length === 0;

  if (cart.length === 0) {
    cartItems.innerHTML = `
      <div class="empty-cart">
        <div>
          <div class="empty-icon">⌂</div>
          <h3>Your cart is empty.</h3>
          <p>Add the properties you like. They will appear here so you can review them before contacting the seller.</p>
        </div>
      </div>
    `;
    return;
  }

  cartItems.innerHTML = cart.map((property) => `
      <div class="cart-item">
        <img src="${esc(property.image)}" alt="${esc(property.title)}" data-fallback />
        <div>
          <h4>${esc(property.title)}</h4>
          <p>${esc(property.location)}</p>
          <strong>${esc(property.priceLabel)}</strong>
        </div>
        <button class="remove-item" type="button" data-remove="${property.id}" aria-label="Remove">×</button>
      </div>
    `).join("");

  cartItems.querySelectorAll("img[data-fallback]").forEach((img) => {
    img.addEventListener("error", () => {
      img.onerror = null;
      img.src = "https://placehold.co/300x220/e8e7e1/133b36?text=Property";
    }, { once: true });
  });

  cartItems.querySelectorAll("[data-remove]").forEach((button) => {
    button.addEventListener("click", () => toggleCart(Number(button.dataset.remove)));
  });
}

/* ------------------------------------------------------------------
   Cart drawer
------------------------------------------------------------------- */

function openCartDrawer() {
  cartDrawer.classList.add("open");
  drawerBackdrop.classList.add("show");
  cartDrawer.setAttribute("aria-hidden", "false");
  document.body.style.overflow = "hidden";
}

function closeCartDrawer() {
  cartDrawer.classList.remove("open");
  drawerBackdrop.classList.remove("show");
  cartDrawer.setAttribute("aria-hidden", "true");
  document.body.style.overflow = "";
}

cartButton.addEventListener("click", openCartDrawer);
closeCart.addEventListener("click", closeCartDrawer);
drawerBackdrop.addEventListener("click", closeCartDrawer);

/* ------------------------------------------------------------------
   Buy / Contact Seller  ->  stores a purchase ENQUIRY (no payment, nothing is "sold")
------------------------------------------------------------------- */

buyButton.addEventListener("click", async () => {
  if (!cart.length) return;

  const snapshot = [...cart];
  const original = buyButton.textContent;
  setBusy(buyButton, true);
  buyButton.textContent = "Sending enquiry…";

  try {
    await api.sendInquiries(snapshot.map((item) => item.id));
  } catch (err) {
    showToast(err.message);
    return;
  } finally {
    buyButton.textContent = original;
    setBusy(buyButton, cart.length === 0);
  }

  const selectedNames = snapshot.map((item) => item.title);
  $("buyModalText").textContent =
    `We've recorded your interest in ${snapshot.length} ${snapshot.length === 1 ? "property" : "properties"}. ` +
    "Contact our property desk to confirm availability, schedule a visit and continue the purchase process.";

  const whatsappMessage = encodeURIComponent(
    `Hello Trichy Estates, I am interested in: ${selectedNames.join(", ")}. Please share availability and next steps.`
  );
  const waNumber = String(contact.whatsapp).replace(/\D/g, "");
  $("whatsappButton").href = `https://wa.me/${waNumber}?text=${whatsappMessage}`;

  showToast("Enquiry sent to our property desk");
  closeCartDrawer();
  modalBackdrop.classList.add("show");
  buyModal.classList.add("show");
  document.body.style.overflow = "hidden";
});

function closeModal() {
  modalBackdrop.classList.remove("show");
  buyModal.classList.remove("show");
  document.body.style.overflow = "";
}

closeBuyModal.addEventListener("click", closeModal);
modalBackdrop.addEventListener("click", closeModal);

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeCartDrawer();
    closeModal();
  }
});

/* ------------------------------------------------------------------
   Start-up
------------------------------------------------------------------- */

(async function init() {
  setAuthMode("login");
  loadContact();

  // Opened from a password-reset e-mail:  /?reset=<token>
  const token = new URLSearchParams(window.location.search).get("reset");
  if (token) {
    resetToken = token;
    showResetForm();
    return;
  }

  // Returning visitor with a saved session: verify it with the server before showing the app.
  if (!api.getToken()) return;

  authScreen.classList.add("hidden");   // avoid flashing the login form
  try {
    const user = await api.me();
    const saved = api.getUser();
    // Keep the "remember me" choice: re-save into whichever store already held the session.
    api.saveSession(api.getToken(), user, localStorage.getItem("trichy-estates-token") !== null);
    await showApp(user || saved);
  } catch (err) {
    if (err.status === 401) api.clearSession();
    authScreen.classList.remove("hidden");
    if (err.status !== 401) showToast(err.message);
  }
})();
