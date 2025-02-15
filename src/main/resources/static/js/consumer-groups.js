// fetch consumers
function fetchConsumers(connectionName) {
    console.log("Fetching consumers for connection:", connectionName);
    setSelectedConnection(connectionName);

    const payload = {
        serverName: connectionName
    };

    fetch("/consumers/getall", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Failed to fetch consumers");
            }
            return response.json();
        })
        .then(data => {
            const consumersListId = `consumersList-${connectionName}`;
            const consumersList = document.getElementById(consumersListId);
            if (!consumersList) {
                console.error(`Element with ID ${consumersListId} not found`);
                return;
            }
            consumersList.innerHTML = ''; // Clear the list

            if (!data.isSuccessful) {
                console.error("Error fetching consumers:", data.message);
                consumersList.innerHTML = `<li class="list-group-item text-danger">${data.message || "Failed to load consumers."}</li>`;
                return;
            }

            const consumerGroups = data.result || [];
            if (consumerGroups.length === 0) {
                consumersList.innerHTML = '<li class="list-group-item text-warning">No consumers found.</li>';
                return;
            }

            consumerGroups.forEach(consumer => {
                const li = document.createElement('li');
                li.classList.add('list-group-item');

                const button = document.createElement('button');
                button.classList.add('btn', 'btn-secondary', 'w-100', 'mt-2', 'consumer-button');
                button.innerText = consumer;

                button.addEventListener('click', () => {
                    console.log("Fetching details for consumer:", consumer);


                    document.querySelector(".consumer-details").style.display = "block";
                    document.querySelector(".topic-details").style.display = "none";

                    document.querySelectorAll(".consumer-button, .topic-button").forEach(btn => {
                        btn.classList.remove("active-consumer", "active-topic");
                    });

                    button.classList.add("active-consumer");

                    fetchConsumerDetails(consumer);

                    const consumerNameElement = document.getElementById("consumer-name");
                    consumerNameElement.style.display = "block";
                    consumerNameElement.innerText = `Consumer: ${consumer}`;

                    document.getElementById("consumer-tabs").style.display = "block";
                    document.getElementById("general-tab").click();
                });

                li.appendChild(button);
                consumersList.appendChild(li);
            });
        })
        .catch(error => {
            console.error("Error fetching consumers:", error);
            const consumersList = document.getElementById(`consumersList-${connectionName}`);
            consumersList.innerHTML = '<li class="list-group-item text-danger">Failed to load consumers.</li>';
        });
}

function fetchConsumerDetails(consumerName) {
    console.log("Fetching details for consumer:", consumerName);

    const connectionName = getSelectedConnectionName();

    setSelectedConsumerName(consumerName);

    const payload = {
        serverName: connectionName,
        consumerName: consumerName
    };

    fetch("/consumers/details", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) throw new Error("Failed to fetch consumer details");
            return response.json();
        })
        .then(data => {
            if (!data.isSuccessful) {
                console.error("Error fetching consumer details:", data.message);
                document.getElementById("general").innerHTML =
                    `<p class="text-danger">${data.message || "Failed to load consumer details."}</p>`;
                return;
            }

            const details = data.result;

            document.getElementById("general").innerHTML = `
                <p><strong>Consumer Type:</strong> ${details.consumerType}</p>
                <p><strong>Active:</strong> ${details.isActive ? "Yes" : "No"}</p>
                <p><strong>Offset Stored In:</strong> ${details.offsetStoredIn}</p>
                <p><strong>Auto Commit:</strong> ${details.autoCommit ? "Enabled" : "Disabled"}</p>
            `;
        })
        .catch(error => {
            console.error("Error fetching consumer details:", error);
            document.getElementById("general").innerHTML =
                `<p class="text-danger">An error occurred while loading consumer details.</p>`;
        });
}

function fetchConsumerOffsets() {
    const consumerName = getSelectedConsumerName();
    const connectionName = getSelectedConnectionName();

    console.log("Fetching offsets for consumer:", consumerName);

    const payload = {
        serverName: connectionName,
        consumerName: consumerName
    };

    fetch("/consumers/offsets", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) throw new Error("Failed to fetch consumer offsets");
            return response.json();
        })
        .then(data => {
            if (!data.isSuccessful) {
                console.error("Error fetching consumer offsets:", data.message);
                document.getElementById("offsets-table-body").innerHTML =
                    `<tr><td colspan="6" class="text-danger">${data.message || "Failed to load consumer offsets."}</td></tr>`;
                return;
            }

            const offsets = data.result;
            const offsetsTableBody = document.getElementById("offsets-table-body");
            offsetsTableBody.innerHTML = ""; // Clear previous data

            offsets.forEach(offset => {
                const row = document.createElement("tr");

                row.innerHTML = `
                    <td>${offset.topic}</td>
                    <td>${offset.partition}</td>
                    <td>${offset.startOffset}</td>
                    <td>${offset.endOffset}</td>
                    <td><span id="offset-value-${offset.topic}-${offset.partition}">${offset.offset}</span></td>
                    <td>${offset.lag}</td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="editOffset('${offset.topic}', ${offset.partition}, ${offset.offset})">
                            <i class="bi bi-pencil-square"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="stopConsumer('${consumerName}')">
                            <i class="bi bi-stop-circle"></i>
                        </button>
                        <button class="btn btn-sm btn-success" onclick="resumeConsumer('${consumerName}')">
                            <i class="bi bi-play-circle"></i>
                        </button>
                    </td>
                `;

                offsetsTableBody.appendChild(row);
            });
        })
        .catch(error => {
            console.error("Error fetching consumer offsets:", error);
            document.getElementById("offsets-table-body").innerHTML =
                `<tr><td colspan="6" class="text-danger">An error occurred while loading consumer offsets.</td></tr>`;
        });
}

// Edit offset function (opens a prompt)
function editOffset(topic, partition, currentOffset) {
    const newOffset = prompt(`Enter new offset for ${topic} (Partition ${partition}):`, currentOffset);
    if (newOffset !== null) {
        document.getElementById(`offset-value-${topic}-${partition}`).innerText = newOffset;
        // You can send an API request here to update the offset in Kafka
    }
}

// Stop consumer function
function stopConsumer(consumerName) {
    console.log("Stopping consumer:", consumerName);
    alert(`Consumer ${consumerName} has been stopped.`);
}

// Resume consumer function
function resumeConsumer(consumerName) {
    console.log("Resuming consumer:", consumerName);
    alert(`Consumer ${consumerName} has been resumed.`);
}

let selectedConsumerName = null;

function setSelectedConsumerName(consumerName) {
    selectedConsumerName = consumerName;
}

function getSelectedConsumerName() {
    if (!selectedConsumerName) {
        console.error("No consumer selected");
        return null;
    }
    return selectedConsumerName;
}


