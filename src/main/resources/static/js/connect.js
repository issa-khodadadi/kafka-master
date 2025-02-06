/*
function submitForm() {
    const serverName = document.getElementById('serverName').value.trim();
    const serverIP = document.getElementById('serverIP').value.trim();
    const serverPort = document.getElementById('serverPort').value.trim();

    const errorMessageElement = document.getElementById('errorMessage');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const svgElement = document.querySelector('.loading-svg');
    const connectButton = document.getElementById('connectButton');
    const logoContainer = document.getElementById('logo-container');

    // Hide the error message initially and remove alert classes
    errorMessageElement.style.display = 'none';
    errorMessageElement.classList.remove('alert-danger', 'alert-success');

    // Show loading overlay and disable button
    loadingOverlay.style.display = 'flex';
    document.body.classList.add('loading');
    connectButton.disabled = true;

    // Reset the SVG to the loading state
    svgElement.classList.remove('bold-complete');
    svgElement.classList.add('loading-animation');

    const payload = {
        serverName: serverName,
        serverIP: serverIP,
        serverPort: serverPort
    };

    fetch('/connect', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Server error'); // Handle HTTP errors
            }
            return response.json(); // Parse JSON response
        })
        .then(data => {
            if (data.isSuccessful) {
                // Successful connection: Apply the final animation and change logo layout
                logoContainer.classList.add('inline');  // Prepare for inline layout
                logoContainer.classList.add('show');    // Apply the final visibility

                // Change the SVG to indicate success
                svgElement.classList.remove('loading-animation');
                svgElement.classList.add('bold-complete');

                // Redirect after a short delay (match duration of animation)
                setTimeout(() => {
                    window.location.href = "/main";  // Redirect after animation completes
                }, 1500); // Matches the duration of the transition animation
            } else {
                // Display error message from backend in red
                errorMessageElement.classList.add('alert', 'alert-danger');
                errorMessageElement.style.display = 'block';
                errorMessageElement.textContent = data.message || "An error occurred. Please try again.";
            }
        })
        .catch(error => {
            // Handle unexpected errors and display in red
            errorMessageElement.classList.add('alert', 'alert-danger');
            errorMessageElement.style.display = 'block';
            errorMessageElement.textContent = error.message || "An unexpected error occurred. Please try again.";
        })
        .finally(() => {
            // Hide the loading overlay and re-enable the button
            loadingOverlay.style.display = 'none';
            document.body.classList.remove('loading');
            connectButton.disabled = false;
        });
}

function submitNewConnection() {
    const serverName = document.getElementById('serverName').value.trim();
    const serverIP = document.getElementById('serverIP').value.trim();
    const serverPort = document.getElementById('serverPort').value.trim();

    const errorMessageElement = document.getElementById('errorMessage');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const svgElement = document.querySelector('.loading-svg');
    const connectButton = document.getElementById('connectButton');
    const logoContainer = document.getElementById('logo-container'); // Logo and app-name wrapper

    // Hide the error message initially and remove alert classes
    errorMessageElement.style.display = 'none';
    errorMessageElement.classList.remove('alert-danger', 'alert-success');

    // Show loading overlay and disable button
    loadingOverlay.style.display = 'flex';
    document.body.classList.add('loading');
    connectButton.disabled = true;

    // Reset the SVG to the loading state
    svgElement.classList.remove('bold-complete');
    svgElement.classList.add('loading-animation');

    const payload = {
        serverName: serverName,
        serverIP: serverIP,
        serverPort: serverPort
    };

    fetch('/connect', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Server error');
            }
            return response.json();
        })
        .then(data => {
            if (data.isSuccessful) {
                // Successful connection: Apply the final animation and change logo layout
                // logoContainer.classList.add('inline');  // Prepare for inline layout
                // logoContainer.classList.add('show');    // Apply the final visibility

                // Change the SVG to indicate success
                // svgElement.classList.remove('loading-animation');
                // svgElement.classList.add('bold-complete');

                setTimeout(() => {
                    window.location.href = "/main";
                }, 1500);
            } else {
                // Display error message from backend in red
                errorMessageElement.classList.add('alert', 'alert-danger');
                errorMessageElement.style.display = 'block';
                errorMessageElement.textContent = data.message || "An error occurred. Please try again.";
            }
        })
        .catch(error => {
            // Handle unexpected errors and display in red
            errorMessageElement.classList.add('alert', 'alert-danger');
            errorMessageElement.style.display = 'block';
            errorMessageElement.textContent = error.message || "An unexpected error occurred. Please try again.";
        })
        .finally(() => {
            loadingOverlay.style.display = 'none';
            document.body.classList.remove('loading');
        });
}
*/

// Utility function to handle form submission for connections
function handleConnectionSubmission(isNewConnection = false) {
    const serverName = document.getElementById('serverName').value.trim();
    const serverIP = document.getElementById('serverIP').value.trim();
    const serverPort = document.getElementById('serverPort').value.trim();

    const errorMessageElement = document.getElementById('errorMessage');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const svgElement = document.querySelector('.loading-svg');
    const connectButton = document.getElementById('connectButton');
    const logoContainer = document.getElementById('logo-container');

    // Reset UI elements
    resetConnectErrorMessage(errorMessageElement);
    showConnectLoadingState(loadingOverlay, connectButton, svgElement);

    const payload = { serverName, serverIP, serverPort };

    fetch('/connect', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) throw new Error('Server error');
            return response.json();
        })
        .then(data => {
            if (data.isSuccessful) {
                handleSuccessfulConnection(isNewConnection, svgElement, logoContainer);
            } else {
                displayErrorMessage(errorMessageElement, data.message);
            }
        })
        .catch(error => {
            displayErrorMessage(errorMessageElement, error.message);
        })
        .finally(() => {
            hideConnectLoadingState(loadingOverlay, connectButton);
        });
}

// Helper function to reset error message UI
function resetConnectErrorMessage(errorMessageElement) {
    errorMessageElement.style.display = 'none';
    errorMessageElement.classList.remove('alert-danger', 'alert-success');
}

// Helper function to show loading state
function showConnectLoadingState(loadingOverlay, connectButton, svgElement) {
    loadingOverlay.style.display = 'flex';
    document.body.classList.add('loading');
    connectButton.disabled = true;

    svgElement.classList.remove('bold-complete');
    svgElement.classList.add('loading-animation');
}

// Helper function to hide loading state
function hideConnectLoadingState(loadingOverlay, connectButton) {
    loadingOverlay.style.display = 'none';
    document.body.classList.remove('loading');
    connectButton.disabled = false;
}

// Helper function to handle a successful connection
function handleSuccessfulConnection(isNewConnection, svgElement, logoContainer) {
    if (!isNewConnection) {
        logoContainer.classList.add('inline', 'show');

        svgElement.classList.remove('loading-animation');
        svgElement.classList.add('bold-complete');
    }

    setTimeout(() => {
        window.location.href = "/main";
    }, 1500);
}

// Helper function to display an error message
function displayErrorMessage(errorMessageElement, message) {
    errorMessageElement.classList.add('alert', 'alert-danger');
    errorMessageElement.style.display = 'block';
    errorMessageElement.textContent = message || "An unexpected error occurred. Please try again.";
}

// Refactored submit functions
function submitForm() {
    handleConnectionSubmission(false);
}

function submitNewConnection() {
    handleConnectionSubmission(true);
}
