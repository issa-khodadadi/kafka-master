<img src="logo.png" alt="Kafka Master Logo" style="padding:10px; border-radius:8px;" width="200">

# kafka-master
this is a user interface to simplify kafka servers managemen.

Kafka Master is a Java-based management application for Apache Kafka servers. It provides an easy way to monitor and manage Kafka clusters, including topics, partitions, messages, consumers, and configurations, through a user-friendly web interface.

## Features

- **Topic Management**: 
  - View a list of all topics in the Kafka cluster.
  - Inspect details of each topic, including configurations and partitions.

- **Message Management**:
  - View messages published on a specific topic.
  - Publish messages to a topic or specific partition.

- **Partition Management**:
  - View partitions for a given topic.
  - Monitor the partition details, such as leader, replicas, and offsets.

- **Consumer Management**:
  - View active consumers and their details.
  - Monitor consumer group offsets and lag.

- **Configurations**:
  - View and edit configuration settings for topics and brokers.

## Prerequisites

Before running Kafka Master, ensure the following:

1. **Java 17**: Kafka Master requires Java 17 or higher. Install it from [Oracle](https://www.oracle.com/java/technologies/javase-downloads.html) or [OpenJDK](https://openjdk.org/).

2. **Maven**: Maven is required to build and run the project. Install it from [Apache Maven](https://maven.apache.org/).

3. **Apache Kafka**: A running Kafka cluster is required. 
   - [Download Kafka](https://kafka.apache.org/downloads)
   - Install and start the Kafka server.

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/Kafka-Master.git
   cd Kafka-Master
   ```

2. Build the project using Maven:
   ```bash
   mvn clean install
   ```

## Running the Application

1. Start the application:
   ```bash
   mvn spring-boot:run
   ```

2. Open your browser and navigate to:
   ```
   http://localhost:8020
   ```

3. Enter the Kafka server details:
   - **Server Name**: A name for your Kafka server.
   - **IP Address**: The IP address of the Kafka broker.
   - **Port**: The port number of the Kafka broker.

4. Click submit to be redirected to the main page, where you can view and manage Kafka topics, partitions, messages, consumers, and configurations.

## Usage

1. **Topics**:
   - Navigate to the Topics section to view and manage Kafka topics.
   - Click on a topic to inspect its partitions and configurations.

2. **Messages**:
   - Select a topic to view messages.
   - Publish a new message by providing the content and (optionally) a target partition.

3. **Partitions**:
   - View partition details such as leader, replicas, ISR, and offsets.

4. **Consumers**:
   - Explore active consumer groups.
   - Monitor consumer offsets and lag for efficient troubleshooting.

## Contributing

Contributions are welcome! If you'd like to contribute to Kafka Master:

1. Fork the repository.
2. Create a new branch for your feature/bug fix.
3. Commit your changes and push the branch.
4. Submit a pull request.

## License

Kafka Master is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Troubleshooting

1. **Kafka Cluster Connection Issues**:
   - Ensure the Kafka broker is running and accessible.
   - Verify the server details entered on the login page.

2. **Application Not Starting**:
   - Ensure Java 17 and Maven are correctly installed.
   - Check the logs for errors during startup.

3. **Missing Dependencies**:
   - Run `mvn clean install` to rebuild the project and ensure all dependencies are resolved.

## Contact

For issues or feature requests, please open an issue in this repository or contact the maintainer via GitHub.
