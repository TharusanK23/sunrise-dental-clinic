(async () => {
    const user = await requireAuth();
    if (!user) return;

    try {
        const summary = await Api.get("/reports/dashboard");
        document.getElementById("kpiTotalDentists").textContent = summary.totalDentists;
        document.getElementById("kpiAvailableDentists").textContent = summary.availableDentists;
        document.getElementById("kpiActiveAppointments").textContent = summary.activeAppointments;
        document.getElementById("kpiTodaysAppointments").textContent = summary.todaysAppointments;
        document.getElementById("kpiTotalPatients").textContent = summary.totalPatients;
        document.getElementById("kpiTotalRevenue").textContent = formatCurrency(summary.totalRevenue);
        document.getElementById("kpiUnpaidAmount").textContent = formatCurrency(summary.unpaidAmount);
    } catch (err) {
        showAlert("alertBox", "Could not load dashboard summary. Make sure database/schema.sql has been imported (it provides the stored procedure/view the dashboard depends on).");
    }

    try {
        const appointments = await Api.get("/appointments");
        const recent = appointments.slice(-5).reverse();
        const body = document.getElementById("recentAppointmentsBody");
        if (recent.length === 0) {
            body.innerHTML = `<tr><td colspan="4" class="text-muted">No appointments yet.</td></tr>`;
        } else {
            body.innerHTML = recent.map(a => `
                <tr>
                    <td><a href="appointment-detail.html?number=${encodeURIComponent(a.appointmentNumber)}">${a.appointmentNumber}</a></td>
                    <td>${a.patient.fullName}</td>
                    <td>${a.dentist.fullName}</td>
                    <td><span class="badge bg-secondary status-badge">${a.status}</span></td>
                </tr>`).join("");
        }
    } catch (err) {
        /* dashboard KPIs already reported the connectivity problem, if any */
    }
})();
