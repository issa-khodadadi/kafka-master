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

// function sendSingleMessage() {
//     // Get values from the DOM
//     const partition = document.getElementById("sendMessageModal").getAttribute("data-partition");
//     const key = document.getElementById("singleMessageKey").value.trim();
//     const value = document.getElementById("singleMessageValue").value.trim();
//
//     // Get the topic name and connection name from your form or modal
//     const topicName = getSelectedTopicName();
//     const connectionName = getSelectedConnectionName();
//
//     // Validate required fields
//     if (!topicName || !connectionName || !key || !value || !partition) {
//         alert("Please fill in all required fields.");
//         return;
//     }
//
//     // Prepare the message object
//     const message = {
//         key: key,
//         value: value
//     };
//
//     // Send the POST request to the API
//     fetch(`/sendMessage?topicName=${encodeURIComponent(topicName)}&connectionName=${encodeURIComponent(connectionName)}&partition=${partition}`, {
//         method: "POST",
//         headers: { "Content-Type": "application/json" },
//         body: JSON.stringify(message), // Send message object as the body
//     })
//         .then(response => {
//             if (!response.ok) {
//                 return response.text().then(errorMessage => { throw new Error(errorMessage); });
//             }
//             return response.text();
//         })
//         .then(message => {
//             alert(message); // Show success message or any other response from the server
//         })
//         .catch(error => {
//             console.error("Error sending message:", error);
//             alert("Error: " + error.message); // Show error message to the user
//         });
// }
//
// function sendMultipleMessages() {
//     const partition = document.getElementById("sendMessageModal").getAttribute("data-partition");
//     const messages = document.getElementById("multipleMessagesInput").value;
//
//     fetch(`/sendMessages`, {
//         method: "POST",
//         headers: { "Content-Type": "application/json" },
//         body: JSON.stringify({ partition, messages: JSON.parse(messages) }),
//     })
//         .then(response => response.text())
//         .then(message => alert(message))
//         .catch(error => console.error("Error sending messages:", error));
// }