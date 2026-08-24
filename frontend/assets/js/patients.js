(async () => {
    const user = await requireAuth();
    if (!user) return;

    await loadPatients();

    document.getElementById("searchForm").addEventListener("submit", (e) => {
        e.preventDefault();
        loadPatients(document.getElementById("searchName").value.trim());
    });
    document.getElementById("resetSearch").addEventListener("click", () => {
        document.getElementById("searchName").value = "";
        loadPatients();
    });

    async function loadPatients(search) {
        try {
            const patients = await Api.get("/patients", search ? { search } : undefined);
            const body = document.getElementById("patientsBody");
            body.innerHTML = patients.map(p => `
                <tr>
                    <td>${p.fullName}</td>
                    <td>${p.address}</td>
                    <td>${p.contactNumber}</td>
                    <td>${p.email || "-"}</td>
                    <td>${p.dateOfBirth ? formatDate(p.dateOfBirth) : "-"}</td>
                </tr>`).join("") || `<tr><td colspan="5" class="text-muted">No patients found.</td></tr>`;
        } catch (err) {
            showAlert("alertBox", "Could not load patients.");
        }
    }
})();
