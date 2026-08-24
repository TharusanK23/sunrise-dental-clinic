(async () => {
    const user = await requireAuth();
    if (!user) return;

    await loadDentists();
    document.getElementById("dentistForm").addEventListener("submit", addDentist);

    async function loadDentists() {
        try {
            const dentists = await Api.get("/dentists");
            const body = document.getElementById("dentistsBody");
            body.innerHTML = dentists.map(d => `
                <tr>
                    <td>Dr. ${d.fullName}</td>
                    <td>${d.specialization}</td>
                    <td>${d.contactNumber || "-"}</td>
                    <td><span class="badge status-badge ${statusBadgeClass(d.status)}">${d.status}</span></td>
                    <td data-role-admin-only><button class="btn btn-sm btn-outline-danger" data-id="${d.id}">Delete</button></td>
                </tr>`).join("") || `<tr><td colspan="5" class="text-muted">No dentists yet.</td></tr>`;

            body.querySelectorAll("button[data-id]").forEach(btn => {
                btn.addEventListener("click", () => deleteDentist(btn.dataset.id));
            });

            if (user.role !== "ADMIN") {
                document.querySelectorAll("[data-role-admin-only]").forEach(el => el.style.display = "none");
            }
        } catch (err) {
            showAlert("alertBox", "Could not load dentists.");
        }
    }

    function statusBadgeClass(status) {
        switch (status) {
            case "AVAILABLE": return "bg-success";
            case "ON_LEAVE": return "bg-warning text-dark";
            case "INACTIVE": return "bg-secondary";
            default: return "bg-dark";
        }
    }

    async function deleteDentist(id) {
        if (!confirm("Delete this dentist? This cannot be undone.")) return;
        try {
            await Api.del("/dentists/" + id);
            await loadDentists();
        } catch (err) {
            alert("Could not delete this dentist (they may have existing appointments).");
        }
    }

    async function addDentist(e) {
        e.preventDefault();
        clearFieldErrors();
        clearAlert("dentistFormAlert");
        const payload = {
            fullName: document.getElementById("fullName").value.trim(),
            specialization: document.getElementById("specialization").value.trim(),
            contactNumber: document.getElementById("contactNumber").value.trim() || null
        };
        try {
            await Api.post("/dentists", payload);
            document.getElementById("dentistForm").reset();
            await loadDentists();
        } catch (err) {
            if (err.status === 400 && err.body.validationErrors) applyFieldErrors(err.body.validationErrors);
            else showAlert("dentistFormAlert", (err.body && err.body.message) || "Could not add the dentist.");
        }
    }

    function applyFieldErrors(errors) {
        Object.entries(errors).forEach(([field, message]) => {
            const el = document.getElementById("err-" + field);
            if (el) el.textContent = message;
        });
    }

    function clearFieldErrors() {
        document.querySelectorAll(".field-error").forEach(el => el.textContent = "");
    }
})();
