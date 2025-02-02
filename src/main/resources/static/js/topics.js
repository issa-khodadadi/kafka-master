function saveContentTypes() {
    const keyContentType = document.getElementById("keyContentType");
    const valueContentType = document.getElementById("valueContentType");

    const payload = {
        key: keyContentType.value.trim(),
        value: valueContentType.value.trim()
    };

    fetch('/savecontenttype', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(response => response.json())
        .then(data => {
            if (!data.isSuccessful) {
                // Display error message in red
                // notificationContainer.classList.add('alert', 'alert-danger');
                // notificationContainer.style.display = 'block';
                // notificationContainer.textContent = data.message || "An error occurred. Please try again.";
                return;
            }

            // Handle successful response

        })
        .catch(error => {
            console.error("Error fetching topics:", error);

        });
}

// fetch all topics
function fetchTopics(connectionName) {
    const errorMessageElement = document.getElementById('errorMessage');
    const topicsList = document.getElementById(`topicsList-${connectionName}`);
    const loadingOverlay = document.getElementById('loadingOverlay');

    resetErrorMessage(errorMessageElement);
    showLoadingState(loadingOverlay);

    setSelectedConnection(connectionName);
    // The API expects a plain string in the body
    const payload = {
        serverName: connectionName,
    };

    fetch('/topics/getall', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload) // Send plain string as JSON
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch topics');
            }
            return response.json();
        })
        .then(data => {
            if (!data.isSuccessful) {
                // Display error message in red
                errorMessageElement.classList.add('alert', 'alert-danger');
                errorMessageElement.style.display = 'block';
                errorMessageElement.textContent = data.message || "An error occurred. Please try again.";
                topicsList.innerHTML = `<li class="list-group-item text-danger">${data.message || "Failed to load topics."}</li>`;
                return;
            }

            // Handle successful response
            const topics = data.result || [];
            topicsList.innerHTML = '';

            if (topics.length === 0) {
                topicsList.innerHTML = '<li class="list-group-item text-warning">No topic exist on server.</li>';
            } else {
                topics.forEach(topic => {
                    const button = document.createElement('button');
                    button.classList.add('btn', 'topic-button', 'mt-1'); // Added `mt-1` for spacing
                    button.innerText = topic;

                    // Add click event to fetch messages and highlight the selected button
                    button.addEventListener('click', () => {
                        // Remove the active state from all buttons
                        document.querySelectorAll(`#topicsList-${connectionName} .btn`).forEach(btn => {
                            btn.classList.remove('active');
                        });

                        // Add active state to the clicked button
                        button.classList.add('active');

                        fetchMessagesCount(topic, connectionName);
                        resetTabs();

                        const topicNameElement = document.getElementById("topic-name");
                        const dynamicTopicNameElement = document.getElementById("dynamic-topic-name");

                        dynamicTopicNameElement.innerText = topic;
                        topicNameElement.style.display = "block";
                        document.getElementById("topic-tabs").style.display = "block";
                        document.getElementById("properties-tab").click();
                    });

                    topicsList.appendChild(button); // Directly append the button to the list
                });

                initializeTooltips();
            }
        })
        .catch(error => {
            console.error("Error fetching topics:", error);
            errorMessageElement.classList.add('alert', 'alert-danger');
            errorMessageElement.style.display = 'block';
            errorMessageElement.textContent = error.message || "An unexpected error occurred. Please try again.";
            topicsList.innerHTML = '<li class="list-group-item text-danger">Failed to load topics.</li>';
        })
        .finally(() => hideLoadingState(loadingOverlay));
}

function refreshTopics() {
    const connectionName = getSelectedConnectionName();
    fetchTopics(connectionName);
}

function resetErrorMessage(errorMessageElement) {
    errorMessageElement.style.display = 'none';
    errorMessageElement.classList.remove('alert-danger', 'alert-success');
}

function showLoadingState(loadingOverlay) {
    loadingOverlay.style.display = 'flex';
    document.body.classList.add('loading');
}

function hideLoadingState(loadingOverlay) {
    loadingOverlay.style.display = 'none';
    document.body.classList.remove('loading');
}

function initializeTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));
}

// config tab functions
let editConfigKey = null;

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
                errorCell.classList.add("text-danger", "text-center"); // Add error styling
                errorRow.appendChild(errorCell);
                tableBody.appendChild(errorRow);
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

function showNotification(message, type) {
    const notification = document.getElementById("notification");
    notification.className = `alert alert-${type} fade show`; // Set the alert type and visibility
    notification.textContent = message; // Set the message content
    notification.style.display = "block"; // Ensure the notification is visible

    // Automatically hide the notification after 2 seconds
    setTimeout(() => {
        notification.classList.remove("show");
        notification.classList.add("fade");
        setTimeout(() => {
            notification.style.display = "none"; // Completely hide after fade-out
        }, 150); // Allow time for the fade-out effect
    }, 2000);
}

function getSelectedTopicName() {
    return document.getElementById("dynamic-topic-name").textContent.trim();
}

let selectedConnectionName = null;

function setSelectedConnection(name) {
    selectedConnectionName = name;
}

function getSelectedConnectionName() {
    if (!selectedConnectionName) {
        alert("Please select a connection.");
        return null;
    }
    return selectedConnectionName;
}


// partition tab functions
function loadPartitions() {
    const topicName = getSelectedTopicName();
    const connectionName = getSelectedConnectionName();

    if (!topicName || !connectionName) {
        alert("Please select a topic and connection.");
        return;
    }

    const payload = {
        topicName: topicName,
        serverName: connectionName,
    };

    fetch('/partitions/getall', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
    })
        .then(response => response.json())
        .then(data => {
            if (!data.isSuccessful) {
                console.error("Error loading partitions:", data.message);
                alert(data.message || "Failed to load partitions.");
                return;
            }

            const partitions = data.result || [];
            const partitionTabs = document.getElementById("partition-tabs");
            partitionTabs.innerHTML = "";

            partitions.forEach(partition => {
                const tab = document.createElement("li");
                tab.classList.add("list-group-item", "list-group-item-action");
                tab.textContent = `Partition ${partition}`;
                tab.dataset.partition = partition;

                tab.addEventListener("click", () => {
                    // Highlight selected partition
                    document.querySelectorAll("#partition-tabs .list-group-item").forEach(item => {
                        item.classList.remove("active");
                    });
                    tab.classList.add("active");

                    // Load partition details
                    loadPartitionDetails(topicName, connectionName, partition);
                });

                partitionTabs.appendChild(tab);
            });
        })
        .catch(error => console.error("Error loading partitions:", error));
}

function loadPartitionDetails(topicName, connectionName, partition) {
    // Prepare the payload for the POST request
    const payload = {
        topicName: topicName,
        serverName: connectionName, // Match the backend parameter
        partition: partition, // Partition number
    };

    // Fetch partition details using the new API
    fetch("/partitions/detail", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload), // Send the payload as JSON
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error("Failed to fetch partition details");
            }
            return response.json();
        })
        .then((data) => {
            if (!data.isSuccessful) {
                console.error("Error fetching partition details:", data.message);
                document.getElementById("partition-details").innerHTML = `
                    <p class="text-danger">Failed to load partition details: ${data.message}</p>
                `;
                return;
            }

            const details = data.result; // Extract the result object
            const partitionDetails = document.getElementById("partition-details");
            partitionDetails.innerHTML = `
                <h5>Partition ${partition} Details</h5>
                <table class="table table-bordered table-striped">
                    <thead>
                        <tr>
                            <th>Replica ID</th>
                            <th>In-Sync</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${details.replicas
                .map(
                    (replica) => `
                                <tr>
                                    <td>${replica.id}</td>
                                    <td>${replica.isInSync ? "Yes" : "No"}</td>
                                </tr>
                            `
                )
                .join("")}
                    </tbody>
                </table>

                <h6>Offsets</h6>
                <table class="table table-bordered table-striped">
                    <thead>
                        <tr>
                            <th>Start Offset</th>
                            <th>End Offset</th>
                            <th>Size</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>${details.offsets.start}</td>
                            <td>${details.offsets.end}</td>
                            <td>${details.offsets.size}</td>
                        </tr>
                    </tbody>
                </table>

                <button class="btn btn-primary mt-3" onclick="openSendMessageModal(${partition})">Send Message</button>
            `;
        })
        .catch((error) => {
            console.error("Error loading partition details:", error);
            document.getElementById("partition-details").innerHTML = `
                <p class="text-danger">Error loading partition details. Check the console for details.</p>
            `;
        });
}

function openSendMessageModal(partition) {
    document.getElementById("sendMessageModal").setAttribute("data-partition", partition);
    const modal = new bootstrap.Modal(document.getElementById("sendMessageModal"));
    modal.show();
}

function sendSingleMessage() {
    // Get values from the DOM
    const partition = document.getElementById("sendMessageModal").getAttribute("data-partition");
    const key = document.getElementById("singleMessageKey").value.trim();
    const value = document.getElementById("singleMessageValue").value.trim();

    // Get the topic name and connection name from your form or modal
    const topicName = getSelectedTopicName();
    const connectionName = getSelectedConnectionName();

    // Validate required fields
    if (!topicName || !connectionName || !key || !value || !partition) {
        alert("Please fill in all required fields.");
        return;
    }

    // Prepare the message object
    const message = {
        key: key,
        value: value
    };

    // Send the POST request to the API
    fetch(`/sendMessage?topicName=${encodeURIComponent(topicName)}&connectionName=${encodeURIComponent(connectionName)}&partition=${partition}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(message), // Send message object as the body
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(errorMessage => { throw new Error(errorMessage); });
            }
            return response.text();
        })
        .then(message => {
            alert(message); // Show success message or any other response from the server
        })
        .catch(error => {
            console.error("Error sending message:", error);
            alert("Error: " + error.message); // Show error message to the user
        });
}

function sendMultipleMessages() {
    const partition = document.getElementById("sendMessageModal").getAttribute("data-partition");
    const messages = document.getElementById("multipleMessagesInput").value;

    fetch(`/sendMessages`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ partition, messages: JSON.parse(messages) }),
    })
        .then(response => response.text())
        .then(message => alert(message))
        .catch(error => console.error("Error sending messages:", error));
}

// reset tab forms in topic detail
function resetForms() {
    // Reset all form inputs in the Properties tab
    document.getElementById("keyContentType").selectedIndex = 0;
    document.getElementById("valueContentType").selectedIndex = 0;

    // Reset all form inputs in the Data tab
    document.getElementById("fromDateTime").value = "";
    document.getElementById("toDateTime").value = "";
    document.getElementById("messageCount").value = 100;
    document.getElementById("orderBy").selectedIndex = 0;

    // Reset Partition Dropdown in messages filter
    partitionDropdown.innerHTML = '<option></option>';

    // Reset Partition Details Section
    const partitionDetails = document.getElementById("partition-details");
    partitionDetails.innerHTML = `
        <div class="alert alert-info">Select a partition to see details.</div>
    `;

    // Reset Send Message Modal
    const sendMessageModal = document.getElementById("sendMessageModal");
    sendMessageModal.removeAttribute("data-partition");
    document.getElementById("singleMessageKey").value = "";
    document.getElementById("singleMessageValue").value = "";
    document.getElementById("multipleMessagesInput").value = "";
}


let selectedServerAddress = null;

function setSelectedServerAddress(serverAddress) {
    selectedServerAddress = serverAddress;
}

function getSelectedServerAddress() {
    if (!selectedServerAddress) {
        alert("Please select a connection.");
        return null;
    }
    return selectedServerAddress;
}

// Function to load more configurations
// Function to toggle hidden configurations
// Function to toggle hidden configurations
// Function to reset the modal with default values
function resetAddTopicModal() {
    // Set default values for main fields
    document.getElementById("topicNameInput").value = '';
    document.getElementById("partitionsInput").value = '1';
    document.getElementById("replicasInput").value = '1';

    // Set default values for existing configurations
    const defaultConfigs = {
        "cleanup.policy": "delete",
        "retention.ms": "604800000",
        "min.insync.replicas": "1",
        "compression.type": "producer",
        "segment.bytes": "1073741824",
        "message.downconversion.enable": "true",
        "flush.ms": "9223372036854775807",
        "max.message.bytes": "1048588",
        "unclean.leader.election.enable": "false",
        "retention.bytes": "-1",
        "delete.retention.ms": "86400000",
        "segment.ms": "604800000",
        "segment.index.bytes": "10485760"
    };

    // Update existing configuration fields
    document.querySelectorAll("#configTableBody input, #configTableBodyHidden input").forEach(input => {
        const configKey = input.closest("tr").firstElementChild.textContent.trim();
        if (defaultConfigs[configKey] !== undefined) {
            input.value = defaultConfigs[configKey]; // Reset to default
        }
    });

    // Remove dynamically added custom configurations
    document.querySelectorAll("#configTableBody .custom-key, #configTableBody .custom-value").forEach(input => {
        input.closest("tr").remove();
    });

    // Collapse hidden configurations section
    const hiddenConfigs = document.getElementById("hiddenConfigs");
    if (hiddenConfigs.classList.contains("expanded")) {
        hiddenConfigs.classList.remove("expanded");
        document.getElementById("loadMoreConfigsButton").innerHTML = '<i class="bi bi-chevron-down"></i>';
    }

    // Reset modal size to small
    const modalDialog = document.querySelector("#addTopicModal .modal-dialog");
    modalDialog.classList.remove("modal-lg");
    modalDialog.classList.add("modal-md");
}

// Function to open the modal
function openAddTopicModal(button) {
    const serverAddress = button.getAttribute("data-server-address");
    setSelectedServerAddress(serverAddress);

    // Reset all fields to default values when opening the modal
    resetAddTopicModal();

    // Update modal title
    document.getElementById("addTopicModalLabel").textContent = "Add New Topic";

    // Show the modal using Bootstrap's Modal API
    const modal = new bootstrap.Modal(document.getElementById("addTopicModal"));
    modal.show();
}


// Function to toggle hidden configurations
function toggleConfigs() {
    const hiddenConfigs = document.getElementById("hiddenConfigs");
    const button = document.getElementById("loadMoreConfigsButton");
    const modalDialog = document.querySelector("#addTopicModal .modal-dialog");

    if (hiddenConfigs.classList.contains("expanded")) {
        hiddenConfigs.classList.remove("expanded");
        button.innerHTML = '<i class="bi bi-chevron-down"></i>';
        modalDialog.classList.remove("modal-lg");
        modalDialog.classList.add("modal-md");
    } else {
        hiddenConfigs.classList.add("expanded");
        button.innerHTML = '<i class="bi bi-chevron-up"></i>';
        modalDialog.classList.remove("modal-md");
        modalDialog.classList.add("modal-lg");
    }
}

// Function to add a custom configuration
function addCustomConfig() {
    const configTableBody = document.getElementById("configTableBody");

    // Create a new row for the custom configuration
    const newRow = document.createElement("tr");
    newRow.innerHTML = `
        <td class="text-center fixed-width">
            <button class="btn btn-sm btn-danger" onclick="removeConfig(this)">
                <i class="bi bi-dash"></i>
            </button>
        </td>
        <td><input type="text" class="form-control" placeholder="Configuration Key"></td>
        <td><input type="text" class="form-control" placeholder="Configuration Value"></td>
    `;

    configTableBody.appendChild(newRow);
}

// Function to remove a configuration
function removeConfig(button) {
    button.closest("tr").remove();
}


// Add topic
/*function addTopic() {
    // Get input values from the modal
    const topicNameInput = document.getElementById("topicNameInput");
    const partitionsInput = document.getElementById("partitionsInput");
    const replicasInput = document.getElementById("replicasInput");
    const modalBody = document.querySelector("#addTopicModal .modal-body");

    const topicName = topicNameInput.value.trim();
    const partitions = parseInt(partitionsInput.value);
    const replicas = parseInt(replicasInput.value);

    let hasError = false;

    // Reset error styles and remove previous error messages
    topicNameInput.style.border = "";
    partitionsInput.style.border = "";
    replicasInput.style.border = "";
    const existingError = modalBody.querySelector(".error-message");
    if (existingError) existingError.remove();

    // Get configurations from both visible and hidden config tables
    const configTableBody = document.querySelectorAll("#configTableBody tr, #configTableBodyHidden tr");
    const configurations = {};

    configTableBody.forEach(row => {
        const keyElement = row.children[1]; // 2nd column = Configuration Key
        const valueElement = row.children[2].querySelector("input");

        if (keyElement && valueElement) {
            const key = keyElement.textContent.trim();
            const value = valueElement.value.trim();
            if (key && value) {
                configurations[key] = value;
            }
        }
    });

    // Construct the payload
    const payload = {
        serverName: getSelectedServerAddress(),
        topicName: topicName,
        partitions: partitions,
        replicas: replicas,
        configurations: configurations
    };

    // Define the API endpoint
    const endpoint = "/topics/add";

    // Send the request to the backend
    fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    })
        .then(response => response.json())
        .then(data => {
            if (!data.success) {
                throw new Error(data.message || "Failed to create topic.");
            }

            // Refresh the page or topic list after successful creation
            fetchTopics(getSelectedConnectionName());

            // Close the modal on success
            const addTopicModal = bootstrap.Modal.getInstance(document.getElementById("addTopicModal"));
            if (addTopicModal) {
                addTopicModal.hide();
            }


        })
        .catch(error => {
            console.error("Error creating topic:", error);
            const errorDiv = document.createElement("div");
            errorDiv.className = "error-message text-danger mt-2";
            errorDiv.textContent = error.message || "Failed to create topic. Check the console for details.";
            modalBody.appendChild(errorDiv);
        });
}*/
// Add topic function with loading spinner and auto-refresh
function addTopic() {
    // Get input values from the modal
    const topicNameInput = document.getElementById("topicNameInput");
    const partitionsInput = document.getElementById("partitionsInput");
    const replicasInput = document.getElementById("replicasInput");
    const saveButton = document.querySelector("#addTopicModal .btn-primary"); // Save button
    const modalBody = document.querySelector("#addTopicModal .modal-body");

    const topicName = topicNameInput.value.trim();
    const partitions = parseInt(partitionsInput.value);
    const replicas = parseInt(replicasInput.value);

    let hasError = false;

    // Reset error styles and remove previous error messages
    topicNameInput.style.border = "";
    partitionsInput.style.border = "";
    replicasInput.style.border = "";
    const existingError = modalBody.querySelector(".error-message");
    if (existingError) existingError.remove();

    // Get configurations from both visible and hidden config tables
    const configTableBody = document.querySelectorAll("#configTableBody tr, #configTableBodyHidden tr");
    const configurations = {};

    configTableBody.forEach(row => {
        const keyElement = row.children[1]; // 2nd column = Configuration Key
        const valueElement = row.children[2].querySelector("input");

        if (keyElement && valueElement) {
            const key = keyElement.textContent.trim();
            const value = valueElement.value.trim();
            if (key && value) {
                configurations[key] = value;
            }
        }
    });

    // Construct the payload
    const payload = {
        serverName: getSelectedServerAddress(),
        topicName: topicName,
        partitions: partitions,
        replicas: replicas,
        configurations: configurations
    };

    // Define the API endpoint
    const endpoint = "/topics/add";

    // Add loading spinner to the Save button
    saveButton.disabled = true; // Disable the button
    saveButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Saving...';

    // Send the request to the backend
    fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    })
        .then(response => response.json())
        .then(data => {
            if (!data.isSuccessful) {
                throw new Error(data.message || "Failed to create topic.");
            }

            // Refresh the topics list after successful creation
            refreshTopics(getSelectedConnectionName());

            // Close the modal on success
            const addTopicModal = bootstrap.Modal.getInstance(document.getElementById("addTopicModal"));
            if (addTopicModal) {
                addTopicModal.hide();
            }

            // Show a success notification
            showAddTopicNotification("Topic created successfully!", "success");
        })
        .catch(error => {
            console.error("Error creating topic:", error);
            const errorDiv = document.createElement("div");
            errorDiv.className = "error-message text-danger mt-2";
            errorDiv.textContent = error.message || "Failed to create topic. Check the console for details.";
            modalBody.appendChild(errorDiv);
        })
        .finally(() => {
            // Restore the Save button state
            saveButton.disabled = false;
            saveButton.innerHTML = "Save Topic";
        });
}
function showAddTopicNotification(message, type = "info") {
    const alertDiv = document.createElement("div");
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed bottom-0 end-0 m-3`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;

    document.body.appendChild(alertDiv);

    setTimeout(() => {
        alertDiv.remove();
    }, 4000); // Auto-remove after 4 seconds
}
