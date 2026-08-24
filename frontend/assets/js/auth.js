/** Session guard + shared navbar behaviour, included on every authenticated page. */

async function requireAuth() {
    try {
        const user = await Api.get("/auth/me");
        sessionStorage.setItem("sdc_user", JSON.stringify(user));
        renderNavbar(user);
        return user;
    } catch (e) {
        window.location.href = computeLoginPath();
        return null;
    }
}

function requireRole(roles) {
    const raw = sessionStorage.getItem("sdc_user");
    if (!raw) return false;
    const user = JSON.parse(raw);
    return roles.includes(user.role);
}

function renderNavbar(user) {
    const nameEl = document.getElementById("navUserName");
    const roleEl = document.getElementById("navUserRole");
    if (nameEl) nameEl.textContent = user.fullName;
    if (roleEl) roleEl.textContent = user.role;

    document.querySelectorAll("[data-role-admin-only]").forEach(el => {
        el.style.display = user.role === "ADMIN" ? "" : "none";
    });

    const currentFile = location.pathname.split("/").pop();
    document.querySelectorAll(".navbar-nav .nav-link").forEach(link => {
        if (link.getAttribute("href") === currentFile) link.classList.add("active");
    });

    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", async (e) => {
            e.preventDefault();
            try {
                await Api.post("/auth/logout");
            } catch (err) {
                /* ignore - we are logging out regardless */
            }
            sessionStorage.removeItem("sdc_user");
            window.location.href = computeLoginPath();
        });
    }
}
