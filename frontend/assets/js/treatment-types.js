(async () => {
    const user = await requireAuth();
    if (!user) return;

    await loadTreatmentTypes();
    document.getElementById("treatmentTypeForm").addEventListener("submit", addTreatmentType);

    async function loadTreatmentTypes() {
        try {
            const types = await Api.get("/treatment-types");
            const body = document.getElementById("treatmentTypesBody");
            body.innerHTML = types.map(t => `
                <tr>
                    <td>${t.treatmentName}</td>
                    <td>${formatCurrency(t.consultationFee)}</td>
                    <td>${t.description || "-"}</td>
                    <td data-role-admin-only><button class="btn btn-sm btn-outline-danger" data-id="${t.id}">Delete</button></td>
                </tr>`).join("") || `<tr><td colspan="4" class="text-muted">No treatment types yet.</td></tr>`;

            body.querySelectorAll("button[data-id]").forEach(btn => {
                btn.addEventListener("click", () => deleteTreatmentType(btn.dataset.id));
            });

            if (user.role !== "ADMIN") {
                document.querySelectorAll("[data-role-admin-only]").forEach(el => el.style.display = "none");
            }
        } catch (err) {
            showAlert("alertBox", "Could not load treatment types.");
        }
    }

    async function deleteTreatmentType(id) {
        if (!confirm("Delete this treatment type? This cannot be undone.")) return;
        try {
            await Api.del("/treatment-types/" + id);
            await loadTreatmentTypes();
        } catch (err) {
            alert("Could not delete this treatment type (it may be used by existing appointments).");
        }
    }

    async function addTreatmentType(e) {
        e.preventDefault();
        clearFieldErrors();
        clearAlert("typeFormAlert");
        const payload = {
            treatmentName: document.getElementById("treatmentName").value.trim(),
            consultationFee: Number(document.getElementById("consultationFee").value),
            description: document.getElementById("description").value.trim() || null
        };
        try {
            await Api.post("/treatment-types", payload);
            document.getElementById("treatmentTypeForm").reset();
            await loadTreatmentTypes();
        } catch (err) {
            if (err.status === 400 && err.body.validationErrors) applyFieldErrors(err.body.validationErrors);
            else showAlert("typeFormAlert", (err.body && err.body.message) || "Could not add the treatment type.");
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
