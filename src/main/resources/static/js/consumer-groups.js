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
                consumersList.innerHTML = '<li class="list-group-item text-muted">No consumers found.</li>';
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


