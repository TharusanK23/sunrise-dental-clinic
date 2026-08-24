(async () => {
    const user = await requireAuth();
    if (!user) return;

    const params = new URLSearchParams(location.search);
    const number = params.get("number");
    if (!number) {
        showAlert("alertBox", "No appointment number was supplied.");
        return;
    }

    if (params.get("created") === "1") {
        showAlert("alertBox", "Appointment " + number + " was created successfully.", "success");
    }

    try {
        const a = await Api.get("/appointments/" + encodeURIComponent(number));
        renderAppointment(a);
    } catch (err) {
        if (err.status === 404) {
            showAlert("alertBox", "No appointment found with number " + number + ".");
        } else {
            showAlert("alertBox", "Could not load the appointment.");
        }
        return;
    }

    function renderAppointment(a) {
        document.getElementById("detailCard").style.display = "";
        document.getElementById("apptNumber").textContent = a.appointmentNumber;
        document.getElementById("apptStatus").textContent = a.status;

        document.getElementById("patName").textContent = a.patient.fullName;
        document.getElementById("patAddress").textContent = a.patient.address;
        document.getElementById("patContact").textContent = a.patient.contactNumber;
        document.getElementById("patEmail").textContent = a.patient.email || "-";

        document.getElementById("dentistName").textContent = "Dr. " + a.dentist.fullName;
        document.getElementById("dentistSpec").textContent = a.dentist.specialization;
        document.getElementById("treatmentName").textContent = a.treatmentType.treatmentName;
        document.getElementById("consultationFee").textContent = formatCurrency(a.treatmentType.consultationFee);

        document.getElementById("scheduleInfo").textContent = `${formatDate(a.appointmentDate)} at ${a.appointmentTime.substring(0,5)}`;
        document.getElementById("notesInfo").textContent = a.notes || "-";

        document.getElementById("createdBy").textContent = a.createdByUsername;
        document.getElementById("createdAt").textContent = formatDateTime(a.createdAt);

        document.getElementById("generateBillBtn").href = "billing.html?number=" + encodeURIComponent(a.appointmentNumber);

        const cancelBtn = document.getElementById("cancelBtn");
        const completeBtn = document.getElementById("completeBtn");
        const finished = a.status === "CANCELLED" || a.status === "COMPLETED";
        cancelBtn.disabled = finished;
        completeBtn.disabled = finished;

        cancelBtn.addEventListener("click", async () => {
            if (!confirm("Cancel appointment " + a.appointmentNumber + "?")) return;
            try {
                await Api.post("/appointments/" + encodeURIComponent(a.appointmentNumber) + "/cancel");
                window.location.reload();
            } catch (err) {
                showAlert("alertBox", "Could not cancel the appointment.");
            }
        });

        completeBtn.addEventListener("click", async () => {
            if (!confirm("Mark appointment " + a.appointmentNumber + " as completed?")) return;
            try {
                await Api.patch("/appointments/" + encodeURIComponent(a.appointmentNumber) + "/status", { status: "COMPLETED" });
                window.location.reload();
            } catch (err) {
                showAlert("alertBox", "Could not update the appointment status.");
            }
        });
    }
})();
