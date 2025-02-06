document.addEventListener('DOMContentLoaded', function () {
    // Select all accordion collapses
    const accordions = document.querySelectorAll('.accordion-collapse');

    accordions.forEach(accordion => {
        // Add event listener for the 'shown.bs.collapse' event (when the accordion is expanded)
        accordion.addEventListener('shown.bs.collapse', function () {
            const serverName = accordion.getAttribute('data-server-name');

            if (accordion.id.startsWith('collapse-topics')) {
                fetchTopics(serverName);
            } else if (accordion.id.startsWith('collapse-consumers')) {
                fetchConsumers(serverName);
            }
        });
    });
});

// Resize Column Functionality
document.querySelectorAll("th").forEach(th => {
    th.style.position = "relative";

    const resizer = document.createElement("div");
    resizer.style.width = "5px";
    resizer.style.height = "100%";
    resizer.style.position = "absolute";
    resizer.style.top = "0";
    resizer.style.right = "0";
    resizer.style.cursor = "col-resize";
    resizer.addEventListener("mousedown", initResize);

    th.appendChild(resizer);
});

function initResize(e) {
    e.preventDefault();
    const th = e.target.parentNode;
    document.addEventListener("mousemove", resizeColumn);
    document.addEventListener("mouseup", stopResize);

    function resizeColumn(event) {
        const width = event.clientX - th.getBoundingClientRect().left;
        th.style.width = width + "px";
    }

    function stopResize() {
        document.removeEventListener("mousemove", resizeColumn);
        document.removeEventListener("mouseup", stopResize);
    }
}

// reset tabs
function resetTabs() {
    // Reset the message count
    document.getElementById("numberOfMessages").innerText = "Number of Messages: 0";

    // Clear the messages table in the Data tab
    const messagesBody = document.getElementById("messagesBody");
    messagesBody.innerHTML = ""; // Remove all rows
    document.getElementById("messagesTable").style.display = "none";
    document.getElementById("noMessagesContainer").style.display = "none";

    // Reset the Partitions tab
    const partitionList = document.getElementById("partition-list");
    if (partitionList) {
        partitionList.innerHTML = "";
    }

    // Clear the Message Details section
    clearMessageDetails();
    resetForms();

    // Reset any other content in the Config tab or other tabs as needed
    const configTabContent = document.getElementById("config-tab-content");
    if (configTabContent) {
        configTabContent.innerHTML = ""; // Clear the content
    }
}

// Add event listener to adjust on window resize
window.addEventListener("resize", adjustMessageDisplay);

// Call the function on page load
document.addEventListener("DOMContentLoaded", adjustMessageDisplay);

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

// =============== NOTIFICATION =========================== //
function showNotification(message, type = "info") {
    const alertDiv = document.createElement("div");

    // Define alert icon based on type
    let icon;
    switch (type) {
        case "success":
            icon = "✔️"; // Checkmark for success
            break;
        case "danger":
            icon = "❌"; // Cross for error
            break;
        case "warning":
            icon = "⚠️"; // Warning icon
            break;
        default:
            icon = "ℹ️"; // Info icon
            break;
    }

    // Create the alert with animation
    alertDiv.className = `custom-alert alert-${type}`;
    alertDiv.innerHTML = `
        <span class="alert-icon">${icon}</span>
        <span class="alert-message">${message}</span>
    `;

    document.body.appendChild(alertDiv);

    // Smooth fade-in effect
    setTimeout(() => {
        alertDiv.classList.add("show");
    }, 100);

    // Auto-remove after 4 seconds
    setTimeout(() => {
        alertDiv.classList.remove("show");
        setTimeout(() => alertDiv.remove(), 500);
    }, 4000);
}