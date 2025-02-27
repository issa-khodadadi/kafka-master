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
                    <p class="text-danger">${data.message}</p>
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
                <p class="text-danger">Fail to load partition details.</p>
            `;
        });
}
