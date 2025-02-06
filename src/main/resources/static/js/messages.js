// Messages count block
function fetchMessagesCount(topicName, connectionName) {
    console.log("Fetching message count for topic:", topicName);

    const refreshButton = document.getElementById("refreshMessagesBtn");
    const spinner = document.createElement("span");

    // Add spinner to the button
    spinner.className = "spinner-border spinner-border-sm ms-2"; // Bootstrap spinner class
    spinner.setAttribute("role", "status");
    spinner.setAttribute("aria-hidden", "true");
    refreshButton.appendChild(spinner);
    refreshButton.disabled = true; // Disable the button
    refreshButton.style.filter = "blur(2px)"; // Apply blur effect

    activeConnectionName = connectionName;

    const payload = {
        topicName: topicName,
        serverName: connectionName
    };

    fetch('/messages/count', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch message count');
            }
            return response.json();
        })
        .then(data => {
            const messageElement = document.getElementById("numberOfMessages");
            if (!data.isSuccessful) {
                console.error("Error fetching message count:", data.message);
                messageElement.classList.add('text-danger');
                messageElement.innerText = data.message || "Failed to load message count.";
                return;
            }

            const totalMessages = data.result || 0;
            messageElement.classList.remove('text-danger');
            messageElement.innerText = `Number of Messages: ${totalMessages}`;
        })
        .catch(error => {
            console.error("Error fetching message count:", error);
            const messageElement = document.getElementById("numberOfMessages");
            messageElement.classList.add('text-danger');
            messageElement.innerText = 'Failed to load message count.';
        })
        .finally(() => {
            // Remove spinner and re-enable the button
            refreshButton.removeChild(spinner);
            refreshButton.disabled = false;
            refreshButton.style.filter = ""; // Remove blur effect
        });
}

function refreshMessagesCount() {
    const topicName = document.getElementById("dynamic-topic-name").innerText;
    fetchMessagesCount(topicName, activeConnectionName);
}

// get messages block`
document.getElementById("partition-combo").addEventListener("change", function () {
    const selectedPartition = this.value;
    console.log("Partition changed to:", selectedPartition);
});

const partitionDropdown = document.getElementById("partition-combo");

// Load partitions dynamically
function loadPartitionsForTopicOnClick() {
    // Preserve the current selection
    const currentSelection = partitionDropdown.value;

    // Remove all dynamic options (but keep the first one if needed)
    while (partitionDropdown.options.length > 1) {
        partitionDropdown.remove(1);
    }

    const topicName = document.getElementById("dynamic-topic-name").innerText;

    const payload = {
        topicName: topicName,
        serverName: activeConnectionName
    };

    fetch('/partitions/getall', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(response => response.json())
        .then(data => {
            if (!data.isSuccessful) {
                console.error("Error loading partitions:", data.message);
                partitionDropdown.innerHTML = `<option value="">${data.message}</option>`;
                partitionDropdown.style.color = "red"; // Indicate error visually
                return;
            }

            const partitions = data.result || [];
            if (partitions.length === 0) {
                partitionDropdown.innerHTML = '<option value="">No partitions found</option>';
                return;
            }

            // Populate dropdown with partitions
            partitions.forEach(partition => {
                const option = document.createElement("option");
                option.value = partition;
                option.textContent = `Partition ${partition}`;
                partitionDropdown.appendChild(option);
            });

            // Restore previous selection if it still exists
            if (currentSelection && [...partitionDropdown.options].some(opt => opt.value === currentSelection)) {
                partitionDropdown.value = currentSelection;
            }

            // Reset color to default for normal operation
            partitionDropdown.style.color = "";
        })
        .catch(error => {
            console.error("Error fetching partitions:", error);
            partitionDropdown.innerHTML = '<option value="">Failed to load partitions</option>';
            partitionDropdown.style.color = "red";
        });
}

// Get all messages including partition filtering
function getAllMessages() {
    clearMessageDetails();

    const topicName = document.getElementById("dynamic-topic-name").innerText;
    const fromDate = document.getElementById("fromDateTime").value;
    const toDate = document.getElementById("toDateTime").value;
    const messageCount = document.getElementById("messageCount").value || 100;
    const orderBy = document.getElementById("orderBy").value;
    const partition = partitionDropdown.value;

    const getAllMessagesButton = document.getElementById("getAllMessagesBtn");
    const messagesTable = document.getElementById("messagesTable");
    const messagesBody = document.getElementById("messagesBody");
    const noMessagesContainer = document.getElementById("noMessagesContainer");
    const pageContent = document.getElementById("pageContent");

    // Add spinner and disable button
    const spinner = document.createElement("span");
    spinner.className = "spinner-border spinner-border-sm ms-2";
    spinner.setAttribute("role", "status");
    spinner.setAttribute("aria-hidden", "true");
    getAllMessagesButton.appendChild(spinner);
    getAllMessagesButton.disabled = true;

    // Blur the page
    pageContent.classList.add("blur");

    const payload = {
        serverName: activeConnectionName,
        topicName: topicName,
        fromDate: fromDate,
        toDate: toDate,
        count: parseInt(messageCount, 10),
        order: orderBy,
        partition: partition !== "" ? parseInt(partition, 10) : null // Include partition only if selected
    };

    console.log("Request Payload:", payload);

    fetch('/messages/getall', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(response => response.json())
        .then(data => {
            messagesBody.innerHTML = "";
            noMessagesContainer.style.display = "none";

            if (!data.isSuccessful) {
                noMessagesContainer.innerText = data.message || "Failed to load messages.";
                noMessagesContainer.style.color = "red";
                noMessagesContainer.style.display = "block";
                messagesTable.style.display = "none";
                return;
            }

            const messages = data.result || [];
            if (messages.length > 0) {
                messages.forEach(message => {
                    const row = document.createElement("tr");
                    row.innerHTML = `
                        <td>
                            <button class="btn btn-outline-primary btn-sm view-message" title="View Message">
                                <i class="bi bi-eye"></i>
                            </button>
                        </td>
                        <td>${message.partition}</td>
                        <td>${message.offset}</td>
                        <td>${message.key || ''}</td>
                        <td>${message.value || ''}</td>
                        <td>${new Date(message.time).toLocaleString()}</td>
                    `;

                    row.querySelector(".view-message").addEventListener("click", () => {
                        displayMessageDetails(message);
                    });

                    messagesBody.appendChild(row);
                });

                messagesTable.style.display = "table";
                noMessagesContainer.style.display = "none";
            } else {
                noMessagesContainer.innerText = "No messages found.";
                noMessagesContainer.style.color = "black";
                noMessagesContainer.style.display = "block";
                messagesTable.style.display = "none";
            }
        })
        .catch(error => {
            console.error("Error fetching messages:", error);
            noMessagesContainer.innerText = 'Failed to load messages.';
            noMessagesContainer.style.color = "red";
            noMessagesContainer.style.display = "block";
            messagesTable.style.display = "none";
        })
        .finally(() => {
            getAllMessagesButton.removeChild(spinner);
            getAllMessagesButton.disabled = false;
            pageContent.classList.remove("blur");
        });
}

function displayMessageDetails(message) {
    // Populate each tab with the corresponding details
    document.getElementById("detail-partition").innerText = message.partition || "N/A";
    document.getElementById("detail-offset").innerText = message.offset || "N/A";
    document.getElementById("detail-key").innerText = message.key || "N/A";
    document.getElementById("detail-value").innerText = message.value || "N/A";
    document.getElementById("detail-timestamp").innerText = new Date(message.time).toLocaleString() || "N/A";

    // Show the message details section
    const messageDetails = document.getElementById("messageDetails");
    messageDetails.style.display = "block";

    // Remove 'active' class from all icons
    document.querySelectorAll('.view-message').forEach(icon => {
        icon.classList.remove('active');
    });

    // Add 'active' class to the clicked icon
    event.target.closest('.view-message').classList.add('active');

    // Activate the first tab (Partition) by default
    const partitionTab = document.getElementById("partition-tab");
    partitionTab.click();
}

// Helper function to clear the Message Details section
function clearMessageDetails() {
    // Assuming there is a section for message details with an ID like 'messageDetails'
    const messageDetails = document.getElementById("messageDetails");
    if (messageDetails) {
        messageDetails.style.display = "none"; // Hide the details section
        messageDetails.querySelector("#detail-partition").innerText = ""; // Clear partition detail
        messageDetails.querySelector("#detail-offset").innerText = "";    // Clear offset detail
        messageDetails.querySelector("#detail-key").innerText = "";       // Clear key detail
        messageDetails.querySelector("#detail-value").innerText = "";     // Clear value detail
        messageDetails.querySelector("#detail-timestamp").innerText = ""; // Clear timestamp detail
    }
}

function adjustMessageDisplay() {
    const detailValue = document.getElementById("detail-value");

    if (window.innerWidth < 768) {
        detailValue.style.whiteSpace = "normal";
        detailValue.style.wordWrap = "break-word";
        detailValue.style.overflowY = "hidden";
        detailValue.style.maxHeight = "none";
    } else {
        detailValue.style.whiteSpace = "pre-wrap";
        detailValue.style.wordWrap = "break-word";
        detailValue.style.overflowY = "auto";
        detailValue.style.maxHeight = "400px";
    }
}


// SEND MESSAGE
function openSendMessageModal(partition) {
    document.getElementById("sendMessageModal").setAttribute("data-partition", partition);
    sendMessageModal.show();
}
const sendMessageModal = new bootstrap.Modal(document.getElementById("sendMessageModal"));


function sendSingleMessage() {
    const partition = document.getElementById("sendMessageModal").getAttribute("data-partition");
    const key = document.getElementById("singleMessageKey").value.trim();
    const value = document.getElementById("singleMessageValue").value.trim();
    const topicName = getSelectedTopicName();
    const connectionName = getSelectedConnectionName();
    const modalBody = document.querySelector("#sendMessageModal .modal-body");

    const payload = {
        topicName,
        serverName: connectionName,
        partition: parseInt(partition),
        message: [{ key, value }]
    };

    fetch("messages/sendsingle", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    })
        .then(response => response.json())
        .then(data => {
            if (!data.isSuccessful) {
                sendMessageModal.hide();
                showNotification(data.message, "danger");
            }

            sendMessageModal.hide();
            showNotification(data.message, "success");

            // Clear input fields after successful send
            document.getElementById("singleMessageKey").value = "";
            document.getElementById("singleMessageValue").value = "";
        })
        .catch(error => {
            console.error("Error sending message:", error);
            showNotification(error.message, "danger");
        });
}

function sendMultipleMessages() {
    const partition = document.getElementById("sendMessageModal").getAttribute("data-partition");
    const messagesInput = document.getElementById("multipleMessagesInput").value.trim();
    const topicName = getSelectedTopicName();
    const connectionName = getSelectedConnectionName();
    const modalBody = document.querySelector("#sendMessageModal .modal-body");

    // Process the input string to create an array of key-value pairs
    let messages = messagesInput.split(",").map(msg => {
        const parts = msg.split(":");
        return {
            key: parts[0]?.trim() || "default-key",
            value: parts[1]?.trim() || ""
        };
    });

    // Validate parsed messages
    if (messages.length === 0 || messages.some(m => !m.value)) {
        showNotification("Invalid message format. Use 'key:value,key2:value2' format.", "danger");
        return;
    }

    const payload = {
        topicName,
        serverName: connectionName,
        partition: parseInt(partition),
        message: messages
    };

    fetch("/messages/sendmultiple", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    })
        .then(response => response.json())
        .then(data => {
            if (!data.isSuccessful) {
                sendMessageModal.hide();
                showNotification(data.message, "danger");
                return;
            }

            sendMessageModal.hide();
            showNotification(data.message, "success");

            // Clear input field after successful send
            document.getElementById("multipleMessagesInput").value = "";
        })
        .catch(error => {
            console.error("Error sending messages:", error);
            showNotification("Failed to send messages. Please try again.", "danger");
        });
}





