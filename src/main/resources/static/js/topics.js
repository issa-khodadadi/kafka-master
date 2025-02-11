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

                    button.addEventListener('click', () => {
                        handleTopicSelection(topic);

                        // Show topic details and hide consumer details
                        document.querySelector(".topic-details").style.display = "block";
                        document.querySelector(".consumer-details").style.display = "none";

                        // Remove active state from all topic and consumer buttons
                        document.querySelectorAll(".topic-button, .consumer-button").forEach(btn => {
                            btn.classList.remove("active-topic", "active-consumer");
                        });

                        // Add active state to the clicked topic button
                        button.classList.add("active-topic");

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
    deleteTopicBtn.disabled=true;
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


// Function to load more configurations
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
function openAddTopicModal() {
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
        serverName: getSelectedConnectionName(),
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
            showNotification(data.message, "success");
        })
        .catch(error => {
            console.error("Error creating topic:", error);
            const errorDiv = document.createElement("div");
            errorDiv.className = "error-message text-danger mt-2";
            errorDiv.textContent = error.message || "Failed to create topic.";
            modalBody.appendChild(errorDiv);
        })
        .finally(() => {
            // Restore the Save button state
            saveButton.disabled = false;
            saveButton.innerHTML = "Save Topic";
        });
}

// delete topic
const deleteTopicBtn = document.getElementById("deleteTopicBtn");

// Function to handle topic selection
function handleTopicSelection(topicName) {
    deleteTopicBtn.disabled = false;
    deleteTopicBtn.setAttribute("data-topic-name", topicName);
}

deleteTopicBtn.addEventListener("click", function () {
    const topicName = deleteTopicBtn.getAttribute("data-topic-name");

    if (!topicName) {
        alert("No topic selected.");
        return;
    }

    // Set topic name in the delete modal
    document.getElementById("topicToDelete").textContent = topicName;
});

// Function to delete a topic
document.addEventListener("DOMContentLoaded", function () {
    const deleteTopicBtn = document.getElementById("deleteTopicBtn");
    let selectedTopic = "";

    // Enable delete button when a topic is selected
    function handleTopicSelection(topicName) {
        selectedTopic = topicName;
        deleteTopicBtn.disabled = false;
        deleteTopicBtn.setAttribute("data-topic-name", topicName);
    }

    // Show topic name in delete confirmation modal
    deleteTopicBtn.addEventListener("click", function () {
        const topicName = deleteTopicBtn.getAttribute("data-topic-name");

        if (!topicName) {
            alert("No topic selected.");
            return;
        }

        // Set topic name in delete modal
        document.getElementById("topicToDelete").textContent = topicName;
    });

    // Handle delete confirmation
    document.getElementById("confirmDeleteTopicBtn").addEventListener("click", function () {
        const topicName = deleteTopicBtn.getAttribute("data-topic-name");
        if (!topicName) {
            alert("No topic selected.");
            return;
        }

        // Perform the delete operation
        deleteTopic(topicName);
    });

    // Function to delete a topic
    function deleteTopic(topicName) {
        const modalBody = document.querySelector("#deleteTopicModal .modal-body");
        const connectionName = getSelectedConnectionName();

        // Remove any existing error messages
        const existingError = modalBody.querySelector(".error-message");
        if (existingError) existingError.remove();

        if (!connectionName) {
            alert("Please select a connection.");
            return;
        }

        const payload = {
            topicName: topicName,
            serverName: connectionName,
        };

        fetch("/topics/delete", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(payload),
        })
            .then((response) => response.json())
            .then((data) => {
                if (!data.isSuccessful) {
                    throw new Error(data.message || "Failed to delete topic.");
                }

                // Show success message
                showNotification(data.message, "success");

                // Refresh topics list
                refreshTopics(connectionName);

                // Disable delete button after deletion
                deleteTopicBtn.disabled = true;

                // Close modal only on success
                const deleteTopicModal = bootstrap.Modal.getInstance(document.getElementById("deleteTopicModal"));
                deleteTopicModal.hide();
            })
            .catch((error) => {
                console.error("Error deleting topic:", error);

                // Show error inside the modal (do not close it)
                const errorDiv = document.createElement("div");
                errorDiv.className = "error-message text-danger mt-2";
                errorDiv.textContent = error.message || "Failed to delete topic.";
                modalBody.appendChild(errorDiv);
            });
    }

    // Attach click event to each topic in the list
    document.querySelectorAll(".list-group").forEach(list => {
        list.addEventListener("click", function (event) {
            const clickedTopic = event.target.closest("li");
            if (clickedTopic) {
                handleTopicSelection(clickedTopic.textContent.trim());
            }
        });
    });
});


// ===================== SHARED METHODS =====================//
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

