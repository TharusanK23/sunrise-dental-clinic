(async () => {
    const user = await requireAuth();
    if (!user) return;

    if (user.role !== "ADMIN") {
        document.getElementById("accessDenied").classList.remove("d-none");
        return;
    }
    document.getElementById("adminContent").classList.remove("d-none");

    await loadUsers();
    document.getElementById("userForm").addEventListener("submit", createUser);
    document.getElementById("editUserForm").addEventListener("submit", saveEditedUser);

    const editModalEl = document.getElementById("editUserModal");
    const editModal = new bootstrap.Modal(editModalEl);

    async function loadUsers() {
        try {
            const users = await Api.get("/users");
            const body = document.getElementById("usersBody");
            body.innerHTML = users.map(u => `
                <tr>
                    <td>${u.username}</td>
                    <td>${u.fullName}</td>
                    <td>${u.email}</td>
                    <td><span class="badge bg-secondary status-badge">${u.role}</span></td>
                    <td><span class="badge status-badge ${u.enabled !== false ? "bg-success" : "bg-secondary"}">${u.enabled !== false ? "Active" : "Disabled"}</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline-brand me-1" data-edit-id="${u.id}" data-full-name="${escapeAttr(u.fullName)}" data-email="${escapeAttr(u.email)}" data-username="${escapeAttr(u.username)}">Edit</button>
                        <button class="btn btn-sm btn-outline-danger" data-id="${u.id}">Deactivate</button>
                    </td>
                </tr>`).join("");

            body.querySelectorAll("button[data-id]").forEach(btn => {
                btn.addEventListener("click", async () => {
                    if (!confirm("Deactivate this staff account?")) return;
                    try {
                        await Api.patch("/users/" + btn.dataset.id + "/deactivate");
                        await loadUsers();
                    } catch (err) {
                        alert("Could not deactivate this account.");
                    }
                });
            });

            body.querySelectorAll("button[data-edit-id]").forEach(btn => {
                btn.addEventListener("click", () => openEditModal(btn.dataset));
            });
        } catch (err) {
            showAlert("alertBox", "Could not load staff accounts.");
        }
    }

    function escapeAttr(value) {
        return String(value || "").replace(/&/g, "&amp;").replace(/"/g, "&quot;");
    }

    function openEditModal(data) {
        document.querySelectorAll("#editUserForm .field-error").forEach(el => el.textContent = "");
        clearAlert("editFormAlert");
        document.getElementById("editUserId").value = data.editId;
        document.getElementById("editUsername").value = data.username;
        document.getElementById("editFullName").value = data.fullName;
        document.getElementById("editEmail").value = data.email;
        editModal.show();
    }

    async function saveEditedUser(e) {
        e.preventDefault();
        document.querySelectorAll("#editUserForm .field-error").forEach(el => el.textContent = "");
        clearAlert("editFormAlert");

        const id = document.getElementById("editUserId").value;
        const payload = {
            fullName: document.getElementById("editFullName").value.trim(),
            email: document.getElementById("editEmail").value.trim()
        };

        try {
            await Api.patch("/users/" + id, payload);
            editModal.hide();
            await loadUsers();
        } catch (err) {
            if (err.status === 400 && err.body.validationErrors) {
                const fieldToInputId = { fullName: "editFullName", email: "editEmail" };
                Object.entries(err.body.validationErrors).forEach(([field, message]) => {
                    const el = document.getElementById("err-" + (fieldToInputId[field] || field));
                    if (el) el.textContent = message;
                });
            } else {
                showAlert("editFormAlert", (err.body && err.body.message) || "Could not update this staff account.");
            }
        }
    }

    async function createUser(e) {
        e.preventDefault();
        document.querySelectorAll(".field-error").forEach(el => el.textContent = "");
        clearAlert("formAlert");

        const payload = {
            username: document.getElementById("username").value.trim(),
            password: document.getElementById("password").value,
            fullName: document.getElementById("fullName").value.trim(),
            email: document.getElementById("email").value.trim(),
            role: document.getElementById("role").value
        };

        try {
            await Api.post("/users", payload);
            document.getElementById("userForm").reset();
            await loadUsers();
        } catch (err) {
            if (err.status === 400 && err.body.validationErrors) {
                Object.entries(err.body.validationErrors).forEach(([field, message]) => {
                    const el = document.getElementById("err-" + field);
                    if (el) el.textContent = message;
                });
            } else {
                showAlert("formAlert", (err.body && err.body.message) || "Could not create the staff account.");
            }
        }
    }
})();
