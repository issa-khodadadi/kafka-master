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

function openAddTopicModal() {
    // Reset the modal (clear input fields or reset values)
    resetAddTopicModal();

    // Update modal title
    document.getElementById("addTopicModalLabel").textContent = "Add New Topic";

    // Show the modal using Bootstrap's Modal API
    const modal = new bootstrap.Modal(document.getElementById("addTopicModal"));
    modal.show();
}

function resetAddTopicModal() {
    // Reset the input fields in the modal
    document.getElementById("topicNameInput").value = '';
    document.getElementById("topicConfigInput").value = '';
}