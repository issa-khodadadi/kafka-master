package com.issa.kafka.master.services;

import com.issa.kafka.master.configs.ConfigService;
import com.issa.kafka.master.dto.filters.PartitionFilter;
import com.issa.kafka.master.dto.forms.KafkaConnectionHolderForm;
import com.issa.kafka.master.dto.views.MessageView;
import com.issa.kafka.master.dto.filters.GetAllMessagesFilter;
import com.issa.kafka.master.dto.forms.AddEditConfigForm;
import com.issa.kafka.master.dto.forms.ConnectionForm;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.utility.Validation;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KafkaService {
    private final Map<String, AdminClient> clients = new HashMap<>();
    private final Map<String, KafkaConnectionHolderForm> connections = new HashMap<>();
    private final Map<String, String> contentTypeMap = new HashMap<>();

    // public services
    public ResponseResult getAdminClient(String serverName) {
        AdminClient adminClient = clients.get(serverName);
        if (adminClient == null) {
            return new ResponseResult(
                    ServiceResultStatus.NO_CLIENT_FOUND,
                    false
            );
        }

        return new ResponseResult(ServiceResultStatus.DONE, true, adminClient);
    }

    public ResponseResult getCurrentConnection(String connectionName) {
        KafkaConnectionHolderForm currentConnection = connections.get(connectionName);
        if (currentConnection == null) {
            return new ResponseResult(
                    ServiceResultStatus.NO_CONNECTION_FOUND,
                    false
            );
        }

        return new ResponseResult(ServiceResultStatus.DONE, true, currentConnection);
    }

    public KafkaConsumer<String, String> createKafkaConsumer(String connectionName, AdminClient adminClient) {
        if (adminClient == null) {
            return null;
        }

        KafkaConnectionHolderForm currentConnection = connections.get(connectionName);
        String bootstrapServer = currentConnection.getServerIP() + ":" + currentConnection.getServerPort();

        // Create properties for the Kafka consumer
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServer);
        props.put("key.deserializer", contentTypeMap.getOrDefault("key", StringDeserializer.class.getName()));
        props.put("value.deserializer", contentTypeMap.getOrDefault("value", StringDeserializer.class.getName()));
        props.put("auto.offset.reset", "earliest");

        return new KafkaConsumer<>(props);
    }

    // ===== CONNECT TO SERVER
    // remove conn
    public void removeAllConnections() {
        clients.clear();
        connections.clear();
    }

    // add connection
    private AdminClient createAdminClient(String ip, String port) {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, ip + ":" + port);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
        config.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, "1000");
        config.put(AdminClientConfig.RETRIES_CONFIG, "1");

        return AdminClient.create(config);
    }

    // get connection
    public Collection<KafkaConnectionHolderForm> getConnections() {
        return connections.values();
    }

    // connect to a server
    public ResponseResult connectToServer(ConnectionForm form) {
        ResponseResult validationResult = this.isConnectionFormValid(form);
        if (!validationResult.getIsSuccessful()) {
            return validationResult;
        }

        KafkaConnectionHolderForm kafkaConnectionHolderForm = new KafkaConnectionHolderForm(form.getServerName(), form.getServerIP(), form.getServerPort(), true);

        if (isServerNameExists(kafkaConnectionHolderForm.getServerName())) {
            return new ResponseResult(ServiceResultStatus.SERVER_NAME_DUPLICATE, false);
        }
        if (isServerIdExists(kafkaConnectionHolderForm.getServerIP(), kafkaConnectionHolderForm.getServerPort())) {
            return new ResponseResult(ServiceResultStatus.SERVER_ID_DUPLICATE, false);
        }


        try {
            AdminClient client = createAdminClient(kafkaConnectionHolderForm.getServerIP(), kafkaConnectionHolderForm.getServerPort());
            client.listTopics().names().get(3, TimeUnit.SECONDS);
            connections.put(kafkaConnectionHolderForm.getServerName(), kafkaConnectionHolderForm);
            clients.put(kafkaConnectionHolderForm.getServerName(), client);
        }  catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.CHECK_CONNECTION_PARAMS, false);
        }

        return new ResponseResult(ServiceResultStatus.DONE, true);
    }

    // validate connection form
    private ResponseResult isConnectionFormValid(ConnectionForm connectionForm) {
        if (!Validation.isServerNameMaxLengthValid(connectionForm.getServerName())) {
            return new ResponseResult(ServiceResultStatus.SERVER_NAME_LENGTH_OVERFLOWED, false);
        }
        if (!Validation.isValidIP(connectionForm.getServerIP())) {
            return new ResponseResult(ServiceResultStatus.SERVER_IP_NOT_VALID, false);
        }
        if (!Validation.isValidPort(connectionForm.getServerPort())) {
            return new ResponseResult(ServiceResultStatus.SERVER_PORT_NOT_VALID, false);
        }

        return new ResponseResult(ServiceResultStatus.DONE, true);
    }

    // check if server name duplicate
    private boolean isServerNameExists(String serverName) {
        return connections.values().stream().anyMatch(server -> server.getServerName().equalsIgnoreCase(serverName));
    }

    // Method to check if the server ID exists (IP:Port)
    private boolean isServerIdExists(String serverIp, String serverPort) {
        return connections.values().stream()
                .anyMatch(connection -> connection.getServerIP().equals(serverIp) && connection.getServerPort().equals(serverPort));
    }

    // disconnect, reconnect
    // Disconnect a Kafka Server
    public ResponseResult disconnectServer(String serverName) {
        if (!clients.containsKey(serverName)) {
            return new ResponseResult(ServiceResultStatus.NO_CLIENT_FOUND, false, "No active connection found.");
        }

        KafkaConnectionHolderForm connectionForm = connections.get(serverName);
        connectionForm.setIsConnected(false);

        try {
            AdminClient client = clients.remove(serverName); // Remove from active clients
            if (client != null) {
                client.close(); // Properly close the Kafka connection
            }
            return new ResponseResult(ServiceResultStatus.DONE, true);
        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.ERROR, false);
        }
    }

    // Reconnect to a Kafka Server
    public ResponseResult reconnectServer(String serverName) {
        if (!connections.containsKey(serverName)) {
            return new ResponseResult(ServiceResultStatus.NO_CLIENT_FOUND, false);
        }

        try {
            KafkaConnectionHolderForm connectionDetails = connections.get(serverName);
            AdminClient newClient = createAdminClient(connectionDetails.getServerIP(), connectionDetails.getServerPort());

            // Validate the new connection
            newClient.listTopics().names().get(3, TimeUnit.SECONDS);

            clients.put(serverName, newClient); // Store new connection

            return new ResponseResult(ServiceResultStatus.DONE, true);
        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.ERROR, false);
        }
    }
}
