/**
 * Thin fetch wrapper for the Sunrise Dental Clinic REST API.
 *
 * The backend authenticates via an HttpOnly cookie (see AuthController), so every
 * request is sent with `credentials: 'include'`. A 401 response means the session
 * cookie is missing/expired, so the user is bounced back to the login page.
 */
// Defaults to the local backend for localhost/XAMPP development; on the
// live Render deployment (a *.onrender.com static site) it defaults to the
// deployed backend service instead - either default can still be overridden
// per-page by setting window.SDC_API_BASE before this script loads.
const API_BASE = window.SDC_API_BASE || (
    location.hostname.endsWith(".onrender.com")
        ? "https://sunrise-dental-clinic-api.onrender.com/api"
        : "http://localhost:8082/api"
);

class ApiError extends Error {
    constructor(status, body) {
        super((body && body.message) || `Request failed with status ${status}`);
        this.status = status;
        this.body = body || {};
    }
}

async function apiRequest(path, { method = "GET", body, query } = {}) {
    let url = API_BASE + path;
    if (query) {
        const params = new URLSearchParams();
        Object.entries(query).forEach(([k, v]) => {
            if (v !== undefined && v !== null && v !== "") params.append(k, v);
        });
        const qs = params.toString();
        if (qs) url += (url.includes("?") ? "&" : "?") + qs;
    }

    const response = await fetch(url, {
        method,
        credentials: "include",
        headers: body ? { "Content-Type": "application/json" } : {},
        body: body ? JSON.stringify(body) : undefined
    });

    if (response.status === 204) return null;

    let payload = null;
    const text = await response.text();
    if (text) {
        try {
            payload = JSON.parse(text);
        } catch (e) {
            payload = null;
        }
    }

    if (!response.ok) {
        if (response.status === 401 && !location.pathname.endsWith("/index.html") && !location.pathname.endsWith("/frontend/")) {
            sessionStorage.removeItem("sdc_user");
            const isRoot = location.pathname === "/" || location.pathname.endsWith("/frontend/");
            if (!isRoot) {
                window.location.href = computeLoginPath();
            }
        }
        throw new ApiError(response.status, payload);
    }

    return payload;
}

function computeLoginPath() {
    // Works whether the current page is /index.html or /pages/whatever.html
    return location.pathname.includes("/pages/") ? "../index.html" : "index.html";
}

const Api = {
    get: (path, query) => apiRequest(path, { method: "GET", query }),
    post: (path, body) => apiRequest(path, { method: "POST", body }),
    put: (path, body) => apiRequest(path, { method: "PUT", body }),
    patch: (path, body) => apiRequest(path, { method: "PATCH", body }),
    del: (path) => apiRequest(path, { method: "DELETE" })
};

function formatCurrency(amount) {
    const value = Number(amount || 0);
    return "Rs. " + value.toLocaleString("en-LK", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(dateStr) {
    if (!dateStr) return "";
    const d = new Date(dateStr + "T00:00:00");
    return d.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
}

function formatDateTime(isoStr) {
    if (!isoStr) return "";
    const d = new Date(isoStr);
    return d.toLocaleString("en-GB", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function showAlert(containerId, message, type = "danger") {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show" role="alert">
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>`;
}

function clearAlert(containerId) {
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = "";
}

function extractFieldErrors(apiError) {
    return (apiError && apiError.body && apiError.body.validationErrors) || {};
}
