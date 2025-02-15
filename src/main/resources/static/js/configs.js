// config tab functions
// get all configs
function loadConfigs() {
    const topicName = getSelectedTopicName();
    const connectionName = getSelectedConnectionName();

    if (!topicName || !connectionName) {
        alert("Please select a topic and connection.");
        return;
    }

    const refreshButton = document.getElementById("refreshConfigsBtn");
    const spinner = document.createElement("span");
    spinner.className = "spinner-border spinner-border-sm ms-2"; // Bootstrap spinner class
    spinner.setAttribute("role", "status");
    spinner.setAttribute("aria-hidden", "true");

    // Disable the button and add spinner
    refreshButton.disabled = true;
    refreshButton.appendChild(spinner);
    refreshButton.style.filter = "blur(2px)";

    const tableBody = document.getElementById("config-table-body");

    const payload = {
        topicName: topicName,
        serverName: connectionName,
    };

    fetch(`/topics/getallconfigs`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Failed to fetch configurations");
            }
            return response.json();
        })
        .then(data => {
            tableBody.innerHTML = ""; // Clear the table body

            if (!data.isSuccessful) {
                console.error("Error fetching configurations:", data.message);
                const errorRow = document.createElement("tr");
                const errorCell = document.createElement("td");
                errorCell.colSpan = 3; // Span across all columns
                errorCell.textContent = data.message || "Failed to load configurations.";
                errorCell.classList.add("text-danger", "text-center");
                errorRow.appendChild(errorCell);
                tableBody.appendChild(errorRow);
                showNotification(data.message, 'danger')

                return;
            }

            const configs = data.result || {};
            if (Object.keys(configs).length === 0) {
                const noConfigRow = document.createElement("tr");
                const noConfigCell = document.createElement("td");
                noConfigCell.colSpan = 3; // Span across all columns
                noConfigCell.textContent = "No configurations found.";
                noConfigCell.classList.add("text-center"); // Center the text
                noConfigRow.appendChild(noConfigCell);
                tableBody.appendChild(noConfigRow);
                return;
            }

            // Populate table with configurations
            for (const [key, value] of Object.entries(configs)) {
                const row = document.createElement("tr");

                const keyCell = document.createElement("td");
                keyCell.textContent = key || "N/A";

                const valueCell = document.createElement("td");
                valueCell.textContent = value || "N/A";

                const actionsCell = document.createElement("td");

                // Edit Button
                const editBtn = document.createElement("button");
                editBtn.classList.add("btn", "btn-warning", "me-2");
                editBtn.innerHTML = '<i class="bi bi-pencil"></i>';
                editBtn.onclick = () => openEditConfigModal(key, value);

                // Delete Button
                const deleteBtn = document.createElement("button");
                deleteBtn.classList.add("btn", "btn-danger");
                deleteBtn.innerHTML = '<i class="bi bi-trash"></i>';
                deleteBtn.onclick = () => deleteConfig(key);

                actionsCell.appendChild(editBtn);
                actionsCell.appendChild(deleteBtn);

                row.appendChild(keyCell);
                row.appendChild(valueCell);
                row.appendChild(actionsCell);

                tableBody.appendChild(row);
            }
        })
        .catch(error => {
            console.error("Error loading configurations:", error);
            tableBody.innerHTML = ""; // Clear the table body
            const errorRow = document.createElement("tr");
            const errorCell = document.createElement("td");
            errorCell.colSpan = 3; // Span across all columns
            errorCell.textContent = "Failed to load configs.";
            errorCell.classList.add("text-danger", "text-center"); // Add error styling
            errorRow.appendChild(errorCell);
            tableBody.appendChild(errorRow);
        })
        .finally(() => {
            // Clean up the button and spinner after fetching
            refreshButton.removeChild(spinner);
            refreshButton.disabled = false; // Re-enable the button
            refreshButton.style.filter = ""; // Remove blur effect
        });
}

// add / edit config
function saveConfig() {
    const topicName = getSelectedTopicName();
    const connectionName = getSelectedConnectionName();
    const configNameInput = document.getElementById("configName");
    const configValueInput = document.getElementById("configValue");
    const modalBody = document.querySelector("#configModal .modal-body");

    const configName = configNameInput.value.trim();
    const configValue = configValueInput.value.trim();

    let hasError = false;

    // Reset error styles and clear previous error messages
    configNameInput.style.border = "";
    configValueInput.style.border = "";
    const existingError = modalBody.querySelector(".error-message");
    if (existingError) existingError.remove();

    // Validate inputs
    if (!configName) {
        configNameInput.style.border = "2px solid red";
        hasError = true;
    }
    if (!configValue) {
        configValueInput.style.border = "2px solid red";
        hasError = true;
    }
    if (hasError) {
        const errorDiv = document.createElement("div");
        errorDiv.className = "error-message text-danger mt-2";
        errorDiv.textContent = "Please fill in all required fields.";
        modalBody.appendChild(errorDiv);
        return;
    }

    const endpoint = `/topics/addoreditconfig`;
    const payload = {
        topicName,
        serverName: connectionName, // Updated key to match the backend parameter
        configKey: configName,      // Updated key to match AddEditConfigForm
        configValue,                // Key matches AddEditConfigForm
    };

    fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    })
        .then((response) => response.json()) // Expecting a JSON response
        .then((data) => {
            if (!data.isSuccessful) {
                const errorDiv = document.createElement("div");
                errorDiv.className = "error-message text-danger mt-2";
                errorDiv.textContent = data.message || "Failed to save configuration.";
                // modalBody.appendChild(errorDiv);
                throw new Error(data.message);
            }
            showNotification(data.message || "Configuration saved successfully.", "success");
            loadConfigs(); // Reload configurations after successful save

            // Close the modal on success
            const configModal = bootstrap.Modal.getInstance(document.getElementById("configModal"));
            if (configModal) {
                configModal.hide();
            }
        })
        .catch((error) => {
            console.error("Error saving configuration:", error);
            const errorDiv = document.createElement("div");
            errorDiv.className = "error-message text-danger mt-2";
            errorDiv.textContent = error.message || "Failed to save configuration. Check the console for details.";
            modalBody.appendChild(errorDiv);
        });
}

// reset config popup form
function resetConfigModal() {
    const configNameInput = document.getElementById("configName");
    const configValueInput = document.getElementById("configValue");
    const modalBody = document.querySelector("#configModal .modal-body");

    // Clear input fields
    configNameInput.value = "";
    configValueInput.value = "";

    // Reset styles
    configNameInput.style.border = "";
    configValueInput.style.border = "";

    // Remove any existing error messages
    const existingError = modalBody.querySelector(".error-message");
    if (existingError) existingError.remove();

    // Enable fields for add mode
    configNameInput.readOnly = false;
    configValueInput.required = true;
}

function openAddConfigModal() {
    resetConfigModal(); // Reset the modal

    // Update modal title
    document.getElementById("configModalLabel").textContent = "Add Config";

    // Show the modal
    const modal = new bootstrap.Modal(document.getElementById("configModal"));
    modal.show();
}

// delete config
function openEditConfigModal(key, value) {
    resetConfigModal(); // Reset the modal

    const configNameInput = document.getElementById("configName");
    const configValueInput = document.getElementById("configValue");

    // Populate fields with existing data
    configNameInput.value = key;
    configValueInput.value = value;

    // Make the key read-only and value editable
    configNameInput.readOnly = true;
    configValueInput.required = true;

    // Update modal title
    document.getElementById("configModalLabel").textContent = "Edit Config";

    // Show the modal
    const modal = new bootstrap.Modal(document.getElementById("configModal"));
    modal.show();
}

let configToDeleteName = null;

function deleteConfig(configName) {
    configToDeleteName = configName; // Store the config name globally for later use
    const configToDeleteElement = document.getElementById("configToDelete");
    configToDeleteElement.textContent = configName; // Update modal with config name

    // Show the delete confirmation modal
    const deleteModal = new bootstrap.Modal(document.getElementById("deleteConfigModal"));
    deleteModal.show();
}

document.getElementById("confirmDeleteBtn").addEventListener("click", function () {
    const topicName = getSelectedTopicName();
    const connectionName = getSelectedConnectionName();

    if (!topicName || !connectionName || !configToDeleteName) {
        showNotification("All fields are required.", "danger");
        return;
    }

    const payload = {
        topicName: topicName,
        serverName: connectionName,
        configName: configToDeleteName,
    };

    fetch(`topics/deleteconfig`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    })
        .then((response) => response.json()) // Expecting JSON response
        .then((data) => {
            if (!data.isSuccessful) {
                throw new Error(data.message || "Failed to delete configuration.");
            }
            showNotification(data.message || "Configuration deleted successfully.", "success");
            loadConfigs(); // Refresh the list
        })
        .catch((error) => {
            console.error("Error deleting configuration:", error);
            showNotification(error.message || "Failed to delete configuration. Check the console for details.", "danger");
        })
        .finally(() => {
            // Hide the modal after the delete operation
            const deleteModal = bootstrap.Modal.getInstance(document.getElementById("deleteConfigModal"));
            if (deleteModal) {
                deleteModal.hide();
            }
        });
});