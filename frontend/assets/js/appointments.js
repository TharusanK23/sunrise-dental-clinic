(async () => {
    const user = await requireAuth();
    if (!user) return;

    const newCard = document.getElementById("newAppointmentCard");
    const params = new URLSearchParams(location.search);

    document.getElementById("toggleFormBtn").addEventListener("click", () => newCard.classList.toggle("d-none"));
    document.getElementById("cancelFormBtn").addEventListener("click", () => newCard.classList.add("d-none"));
    if (params.get("new") === "1") newCard.classList.remove("d-none");

    document.querySelectorAll('input[name="patientMode"]').forEach(radio => {
        radio.addEventListener("change", () => {
            const existing = document.getElementById("modeExisting").checked;
            document.getElementById("existingPatientBlock").style.display = existing ? "" : "none";
            document.getElementById("newPatientBlock").style.display = existing ? "none" : "";
        });
    });

    await loadTreatmentTypes();
    await loadPatients();
    await loadAppointments();

    document.getElementById("searchForm").addEventListener("submit", (e) => {
        e.preventDefault();
        const number = document.getElementById("searchNumber").value.trim();
        if (number) window.location.href = "appointment-detail.html?number=" + encodeURIComponent(number);
    });

    document.getElementById("checkAvailabilityBtn").addEventListener("click", checkAvailability);
    document.getElementById("appointmentForm").addEventListener("submit", submitAppointment);

    async function loadTreatmentTypes() {
        try {
            const types = await Api.get("/treatment-types");
            const select = document.getElementById("treatmentTypeSelect");
            select.innerHTML = types.map(t => `<option value="${t.id}">${t.treatmentName} (${formatCurrency(t.consultationFee)})</option>`).join("");
        } catch (err) {
            showAlert("alertBox", "Could not load treatment types.");
        }
    }

    async function loadPatients() {
        try {
            const patients = await Api.get("/patients");
            const select = document.getElementById("patientSelect");
            select.innerHTML = patients.map(p => `<option value="${p.id}">${p.fullName} - ${p.contactNumber}</option>`).join("");
        } catch (err) { /* non-fatal */ }
    }

    async function checkAvailability() {
        clearFieldErrors();
        const appointmentDate = document.getElementById("appointmentDate").value;
        const appointmentTime = document.getElementById("appointmentTime").value;
        const dentistSelect = document.getElementById("dentistSelect");

        if (!appointmentDate || !appointmentTime) {
            showAlert("formAlert", "Please choose both an appointment date and time before checking availability.");
            return;
        }
        clearAlert("formAlert");

        try {
            const dentists = await Api.get("/dentists/available", { appointmentDate, appointmentTime: appointmentTime + ":00" });
            if (dentists.length === 0) {
                dentistSelect.innerHTML = `<option value="">No dentists available at this date/time</option>`;
            } else {
                dentistSelect.innerHTML = `<option value="">-- Select a dentist --</option>` +
                    dentists.map(d => `<option value="${d.id}">Dr. ${d.fullName} (${d.specialization})</option>`).join("");
            }
        } catch (err) {
            showAlert("formAlert", "Could not check dentist availability.");
        }
    }

    async function submitAppointment(e) {
        e.preventDefault();
        clearFieldErrors();
        clearAlert("formAlert");

        const isExisting = document.getElementById("modeExisting").checked;
        const payload = {
            patientId: isExisting ? Number(document.getElementById("patientSelect").value) : null,
            patientFullName: isExisting ? null : document.getElementById("patientFullName").value.trim(),
            patientAddress: isExisting ? null : document.getElementById("patientAddress").value.trim(),
            patientContactNumber: isExisting ? null : document.getElementById("patientContactNumber").value.trim(),
            patientEmail: isExisting ? null : (document.getElementById("patientEmail").value.trim() || null),
            dentistId: Number(document.getElementById("dentistSelect").value) || null,
            treatmentTypeId: Number(document.getElementById("treatmentTypeSelect").value) || null,
            appointmentDate: document.getElementById("appointmentDate").value,
            appointmentTime: document.getElementById("appointmentTime").value ? document.getElementById("appointmentTime").value + ":00" : null,
            notes: document.getElementById("notes").value.trim() || null
        };

        const btn = document.getElementById("submitAppointmentBtn");
        btn.disabled = true;
        btn.textContent = "Saving...";

        try {
            const appointment = await Api.post("/appointments", payload);
            window.location.href = "appointment-detail.html?number=" + encodeURIComponent(appointment.appointmentNumber) + "&created=1";
        } catch (err) {
            if (err.status === 400 && err.body && err.body.validationErrors) {
                applyFieldErrors(err.body.validationErrors);
            } else if (err.status === 409) {
                showAlert("formAlert", err.body.message || "This dentist is already booked at the selected date/time.");
            } else if (err.status === 404) {
                showAlert("formAlert", err.body.message || "The selected dentist or treatment type could not be found.");
            } else {
                showAlert("formAlert", "Could not save the appointment. Please check the form and try again.");
            }
        } finally {
            btn.disabled = false;
            btn.textContent = "Save Appointment";
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

    async function loadAppointments() {
        try {
            const appointments = await Api.get("/appointments");
            const body = document.getElementById("appointmentsBody");
            if (appointments.length === 0) {
                body.innerHTML = `<tr><td colspan="7" class="text-muted">No appointments yet. Click "New Appointment" to create one.</td></tr>`;
                return;
            }
            body.innerHTML = appointments.slice().reverse().map(a => `
                <tr>
                    <td><a href="appointment-detail.html?number=${encodeURIComponent(a.appointmentNumber)}">${a.appointmentNumber}</a></td>
                    <td>${a.patient.fullName}</td>
                    <td>Dr. ${a.dentist.fullName}</td>
                    <td>${a.treatmentType.treatmentName}</td>
                    <td>${formatDate(a.appointmentDate)} ${a.appointmentTime ? a.appointmentTime.substring(0,5) : ""}</td>
                    <td><span class="badge bg-secondary status-badge">${a.status}</span></td>
                    <td><a class="btn btn-sm btn-outline-brand" href="appointment-detail.html?number=${encodeURIComponent(a.appointmentNumber)}">View</a></td>
                </tr>`).join("");
        } catch (err) {
            showAlert("alertBox", "Could not load appointments.");
        }
    }
})();
