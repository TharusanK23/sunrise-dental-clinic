(async () => {
    const user = await requireAuth();
    if (!user) return;

    const today = new Date().toISOString().substring(0, 10);
    const monthAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().substring(0, 10);
    document.getElementById("fromDate").value = monthAgo;
    document.getElementById("toDate").value = today;

    document.getElementById("revenueForm").addEventListener("submit", async (e) => {
        e.preventDefault();
        await loadRevenue();
    });

    await loadRevenue();
    await loadUtilization();

    async function loadRevenue() {
        const from = document.getElementById("fromDate").value;
        const to = document.getElementById("toDate").value;
        try {
            const rows = await Api.get("/reports/revenue", { from, to });
            const body = document.getElementById("revenueBody");
            if (rows.length === 0) {
                body.innerHTML = `<tr><td colspan="3" class="text-muted">No billed revenue in this date range.</td></tr>`;
                return;
            }
            const totalRevenue = rows.reduce((sum, r) => sum + Number(r.totalRevenue), 0);
            body.innerHTML = rows.map(r => `
                <tr><td>${formatDate(r.periodLabel)}</td><td>${r.appointmentCount}</td><td>${formatCurrency(r.totalRevenue)}</td></tr>`).join("")
                + `<tr class="fw-bold border-top"><td>Total</td><td></td><td>${formatCurrency(totalRevenue)}</td></tr>`;
        } catch (err) {
            showAlert("alertBox", "Could not load the revenue report. Make sure database/schema.sql (with sp_daily_revenue_report) has been imported.");
        }
    }

    async function loadUtilization() {
        try {
            const rows = await Api.get("/reports/dentist-utilization");
            const body = document.getElementById("utilizationBody");
            body.innerHTML = rows.map(r => `
                <tr><td>Dr. ${r.fullName}</td><td>${r.specialization}</td><td>${r.timesBooked}</td></tr>`).join("")
                || `<tr><td colspan="3" class="text-muted">No data yet.</td></tr>`;
        } catch (err) {
            showAlert("alertBox", "Could not load the dentist utilisation report. Make sure database/schema.sql (with vw_dentist_utilization) has been imported.");
        }
    }
})();
