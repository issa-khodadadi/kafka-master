document.addEventListener("DOMContentLoaded", function () {
    const serverName = document.body.getAttribute("data-server-name");
    const monitoringUrl = `/monitoring/${serverName}/realtime-data`;
    const statusUrl = `/monitoring/${serverName}/status`;

    const kafkaStatusLabel = document.getElementById("kafkaStatusLabel");
    const kafkaStatusCircle = document.getElementById("kafkaStatusCircle");

    const cpuUsageChartCtx = document.getElementById("cpuUsageChart").getContext("2d");
    const memoryUsageChartCtx = document.getElementById("memoryUsageChart").getContext("2d");
    const consumerVolumeChartCtx = document.getElementById("consumerVolumeChart").getContext("2d");

    const consumerColors = {};

    const cpuChart = createLineChart(cpuUsageChartCtx, "CPU Usage (%)", "#3498db");
    const memoryChart = createLineChart(memoryUsageChartCtx, "Memory Usage (%)", "#e74c3c");
    let consumerVolumeChart = createPieChart(consumerVolumeChartCtx, "Consumer Volume");

    function createLineChart(ctx, label, color) {
        return new Chart(ctx, {
            type: "line",
            data: {
                labels: [],
                datasets: [{
                    label: label,
                    data: [],
                    borderColor: color,
                    backgroundColor: `${color}20`,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        ticks: {
                            callback: function(value) {
                                return value + "%";
                            }
                        }
                    }
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: function(tooltipItem) {
                                return tooltipItem.raw + "%";
                            }
                        }
                    }
                },
                animation: { duration: 500 }
            }
        });
    }

    function createPieChart(ctx, label) {
        return new Chart(ctx, {
            type: "pie",
            data: {
                labels: [],
                datasets: [{
                    label: label,
                    data: [],
                    backgroundColor: [],
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        display: true,
                        position: "right"
                    }
                }
            }
        });
    }

    function updateCharts(data) {
        if (!data || data.error) {
            console.error("Error fetching monitoring data:", data?.error || "No data received");
            return;
        }

        updateStatus(data.kafkaStatus);
        updateLineChart(cpuChart, data.cpuUsage);
        updateLineChart(memoryChart, data.memoryUsage);
        updatePieChart(consumerVolumeChart, data.consumerVolume);
    }

    function updateStatus(status) {
        kafkaStatusLabel.innerText = status === "UP" ? "Kafka Server: UP 🟢" : "Kafka Server: DOWN 🔴";
        kafkaStatusCircle.style.backgroundColor = status === "UP" ? "green" : "red";
    }

    function updateLineChart(chart, value) {
        const now = new Date().toLocaleTimeString();
        if (chart.data.labels.length > 30) {
            chart.data.labels.shift();
            chart.data.datasets[0].data.shift();
        }
        chart.data.labels.push(now);
        chart.data.datasets[0].data.push(value);
        chart.update();
    }

    function updatePieChart(chart, consumerData) {
        if (!consumerData) return;

        const consumerNames = Object.keys(consumerData);
        const consumerValues = Object.values(consumerData);

        consumerNames.forEach(name => {
            if (!consumerColors[name]) {
                consumerColors[name] = getRandomColor();
            }
        });

        chart.data.labels = consumerNames;
        chart.data.datasets[0].data = consumerValues;
        chart.data.datasets[0].backgroundColor = consumerNames.map(name => consumerColors[name]);
        chart.update();
    }

    function getRandomColor() {
        const letters = "0123456789ABCDEF";
        let color = "#";
        for (let i = 0; i < 6; i++) {
            color += letters[Math.floor(Math.random() * 16)];
        }
        return color;
    }

    function startSSE() {
        let eventSource = new EventSource(monitoringUrl);

        eventSource.onmessage = function (event) {
            const data = JSON.parse(event.data);
            updateCharts(data);
        };

        eventSource.onerror = function () {
            console.error("Error with real-time monitoring data stream. Retrying...");
            eventSource.close();
            setTimeout(startSSE, 5000);
        };
    }

    startSSE();
});
