// fetch consumers
/*function fetchConsumers(connectionName) {
    console.log("Fetching consumers for connection:", connectionName);
    fetch(`/consumers?connectionName=${encodeURIComponent(connectionName)}`)
        .then(response => {
            if (!response.ok) throw new Error('Failed to fetch consumers');
            return response.json();
        })
        .then(data => {
            const consumersListId = `consumersList-${connectionName}`;
            const consumersList = document.getElementById(consumersListId);
            if (!consumersList) {
                console.error(`Element with ID ${consumersListId} not found`);
                return;
            }
            consumersList.innerHTML = '';
            data.forEach(consumer => {
                const li = document.createElement('li');
                li.classList.add('list-group-item');

                // Create button with ellipsis and tooltip for full name
                const button = document.createElement('button');
                button.classList.add('btn', 'btn-secondary', 'w-100', 'mt-2', 'consumer-button');
                button.innerText = consumer;
                button.setAttribute('data-bs-toggle', 'tooltip');
                button.setAttribute('data-bs-placement', 'top');
                button.setAttribute('title', consumer); // Tooltip with full consumer name

                button.addEventListener('click', () => {
                    console.log("Fetching details for consumer:", consumer);
                    // displayConsumerDetails(consumer, connectionName);

                    // Update UI to show consumer name and tabs
                    const consumerNameElement = document.getElementById("consumer-name");
                    consumerNameElement.innerText = consumer;
                    consumerNameElement.style.display = "block";
                    document.getElementById("consumer-tabs").style.display = "block";
                    document.getElementById("general-tab").click(); // Activate the "General" tab
                });

                li.appendChild(button);
                consumersList.appendChild(li);
            });

            // Initialize Bootstrap tooltips
            const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function (tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });
        })
        .catch(error => {
            console.error("Error fetching consumers:", error);
            const consumersList = document.getElementById(`consumersList-${connectionName}`);
            consumersList.innerHTML = '<li class="list-group-item text-danger">Failed to load consumers.</li>';
        });
}*/


function fetchConsumers(connectionName) {
    console.log("Fetching consumers for connection:", connectionName);

    // Prepare the payload for the POST request
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

                // Create button with ellipsis and tooltip for full name
                const button = document.createElement('button');
                button.classList.add('btn', 'btn-secondary', 'w-100', 'mt-2', 'consumer-button');
                button.innerText = consumer;
                button.setAttribute('data-bs-toggle', 'tooltip');
                button.setAttribute('data-bs-placement', 'top');
                button.setAttribute('title', consumer); // Tooltip with full consumer name

                button.addEventListener('click', () => {
                    console.log("Fetching details for consumer:", consumer);
                    // Update UI to show consumer name and tabs
                    const consumerNameElement = document.getElementById("consumer-name");
                    consumerNameElement.innerText = consumer;
                    consumerNameElement.style.display = "block";
                    document.getElementById("consumer-tabs").style.display = "block";
                    document.getElementById("general-tab").click(); // Activate the "General" tab
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


