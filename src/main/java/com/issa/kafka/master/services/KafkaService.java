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

        KafkaConnectionHolderForm kafkaConnectionHolderForm = new KafkaConnectionHolderForm(form.getServerName(), form.getServerIP(), form.getServerPort());

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
    // ====== END OF CONNECTION METHODS


    // ====== TOPICS SERVICES
//    public ResponseResult saveContentType(String key, String value) {
//        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
//            return new ResponseResult(ServiceResultStatus.FIELDS_REQUIRED, false);
//        }
//
//        contentTypeMap.put("key", key);
//        contentTypeMap.put("value", value);
//
//        return new ResponseResult(ServiceResultStatus.DONE, true);
//    }

    // get topic list
    public ResponseResult getTopicsForServer(String serverName) {
        try {
            AdminClient adminClient = clients.get(serverName);
            if (adminClient != null) {
                List<String> topics = adminClient.listTopics().names().get().stream().toList();
                return new ResponseResult(
                        ServiceResultStatus.DONE,
                        true,
                        topics
                );
            } else {
                return new ResponseResult(
                        ServiceResultStatus.NO_CLIENT_FOUND,
                        false
                );
            }
        } catch (Exception e) {
            return new ResponseResult(
                    ServiceResultStatus.FAIL_TO_GET_TOPICS,
                    false);
        }
    }

    // get messages count on a topic
    public ResponseResult countMessagesInTopic(String topicName, String serverName) {
        try {
            AdminClient adminClient = clients.get(serverName);
            KafkaConsumer<String, String> consumer = this.createKafkaConsumer(serverName, adminClient);

            if (adminClient != null) {
                // Retrieve partition list for the topic
                List<TopicPartitionInfo> partitionInfos = adminClient.describeTopics(Collections.singletonList(topicName))
                        .all().get().get(topicName).partitions();

                List<TopicPartition> partitions = partitionInfos.stream()
                        .map(partitionInfo -> new TopicPartition(topicName, partitionInfo.partition()))
                        .toList();

                // Get beginning and end offsets for each partition
                Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions);
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

                long totalMessages = 0;

                for (TopicPartition partition : partitions) {
                    long start = beginningOffsets.get(partition);
                    long end = endOffsets.get(partition);
                    totalMessages += (end - start);
                }

                return new ResponseResult(
                        ServiceResultStatus.DONE,
                        true,
                        totalMessages
                );
            } else {
                return new ResponseResult(
                        ServiceResultStatus.NO_CLIENT_FOUND,
                        false
                );
            }
        } catch (Exception e) {
            return new ResponseResult(
                    ServiceResultStatus.FAIL_TO_GET_MESSAGE_COUNT,
                    false);
        }
    }

    // get all messages by filter
    public ResponseResult getMessagesFromTopic(GetAllMessagesFilter getAllMessagesFilter) {
        KafkaConsumer<String, String> consumer = null;
        try {
            ResponseResult validateFilter = this.validateGetMessagesFilter(getAllMessagesFilter);
            if (!validateFilter.getIsSuccessful()) {
                return validateFilter;
            }

            AdminClient adminClient = clients.get(getAllMessagesFilter.getServerName());
            if (adminClient == null) {
                return new ResponseResult(ServiceResultStatus.NO_CLIENT_FOUND, false);
            }

            consumer = createKafkaConsumer(getAllMessagesFilter.getServerName(), adminClient);
            if (consumer == null) {
                return new ResponseResult(ServiceResultStatus.NO_CLIENT_FOUND, false);
            }

            List<MessageView> messages = this.retrieveMessages(consumer, getAllMessagesFilter);
            sortMessages(messages, getAllMessagesFilter.getOrder());

            return new ResponseResult(ServiceResultStatus.DONE, true, messages);
        } catch (Exception e) {
            return handleException(e);
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    private ResponseResult validateGetMessagesFilter(GetAllMessagesFilter getAllMessagesFilter) {
        if (!Validation.isDateTimeValid(getAllMessagesFilter.getFromDate())) {
            new ResponseResult(ServiceResultStatus.FROM_DATE_INVALID, false);
        }
        if (!Validation.isDateTimeValid(getAllMessagesFilter.getToDate())) {
            new ResponseResult(ServiceResultStatus.TO_DATE_INVALID, false);
        }
        if (getAllMessagesFilter.getCount() > ConfigService.MAX_MSG_SIZE) {
            new ResponseResult(ServiceResultStatus.MESSAGE_NUMBERS_OVER_SIZED, false);
        }

        return new ResponseResult(ServiceResultStatus.DONE, true);
    }

    private List<MessageView> retrieveMessages(KafkaConsumer<String, String> consumer, GetAllMessagesFilter filter) throws Exception {
        List<MessageView> messages = new ArrayList<>();
        long count = filter.getCount();
        String specifiedPartition = filter.getPartition();
        List<TopicPartition> partitions;

        if (!Validation.isNullOrBlank(specifiedPartition)) {
            int partitionNumber = Integer.parseInt(specifiedPartition);
            partitions = List.of(new TopicPartition(filter.getTopicName(), partitionNumber));
        } else {
            partitions = getPartitions(filter);
        }

        consumer.assign(partitions);

        boolean hasMoreMessages = true;
        while (hasMoreMessages && messages.size() < count) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            hasMoreMessages = !records.isEmpty();

            for (ConsumerRecord<String, String> record : records) {
                long messageTimestamp = record.timestamp();

                if (this.isWithinDateRange(messageTimestamp, filter.getFromDate(), filter.getToDate())) {
                    messages.add(new MessageView(
                            record.partition(),
                            record.offset(),
                            record.key(),
                            record.value(),
                            messageTimestamp
                    ));

                    if (messages.size() >= count) break;
                }
            }
        }

        return messages;
    }

    private List<TopicPartition> getPartitions(GetAllMessagesFilter filter) throws Exception {
        String topicName = filter.getTopicName();
        AdminClient adminClient = clients.get(filter.getServerName());

        List<TopicPartitionInfo> partitionInfos = adminClient.describeTopics(Collections.singletonList(topicName))
                .all().get().get(topicName).partitions();

        return partitionInfos.stream()
                .map(partitionInfo -> new TopicPartition(topicName, partitionInfo.partition()))
                .toList();
    }

    private boolean isWithinDateRange(long messageTime, String fromDate, String toDate) {
        long fromMillis = Long.MIN_VALUE;
        long toMillis = Long.MAX_VALUE;

        if (!Validation.isNullOrBlank(fromDate)) {
            try {
                fromMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(fromDate).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (!Validation.isNullOrBlank(toDate)) {
            try {
                toMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(toDate).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return messageTime >= fromMillis && messageTime <= toMillis;
    }

    private void sortMessages(List<MessageView> messages, String order) {
        if (Validation.isNullOrBlank(order) ||
                ConfigService.ORDER_NEWEST.equalsIgnoreCase(order)) {
            messages.sort((m1, m2) -> Long.compare(m2.getTime(), m1.getTime()));
        } else if (ConfigService.ORDER_OLDEST.equalsIgnoreCase(order)) {
            messages.sort(Comparator.comparingLong(MessageView::getTime));
        }
    }

    private ResponseResult handleException(Exception e) {
        return new ResponseResult(ServiceResultStatus.FAIL_TO_GET_MESSAGES, false);
    }

    // get partitions of a topic
    public ResponseResult getPartitionsForTopic(String topicName, String serverName) throws ExecutionException, InterruptedException {
        AdminClient client = clients.get(serverName);
        if (client != null) {
            DescribeTopicsResult result = client.describeTopics(List.of(topicName));
            TopicDescription description = result.all().get().get(topicName);

            List<Integer> partitionList = description.partitions().stream()
                    .map(TopicPartitionInfo::partition)
                    .toList();

            return new ResponseResult(ServiceResultStatus.DONE, true, partitionList);
        } else {
            return new ResponseResult(
                    ServiceResultStatus.NO_CLIENT_FOUND,
                    false
            );
        }
    }

    // Add or edit a topic configuration
    public ResponseResult addOrEditConfig(AddEditConfigForm addEditConfigForm) {
        try {
            String configKey = addEditConfigForm.getConfigKey();
            String configValue = addEditConfigForm.getConfigValue();

            ResponseResult validateKeyValue = this.validateConfigKeyValue(configKey, configValue);
            if (!validateKeyValue.getIsSuccessful()) {
                return validateKeyValue;
            }

            AdminClient adminClient = clients.get(addEditConfigForm.getServerName());
            if (adminClient != null) {
                ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, addEditConfigForm.getTopicName());
                Config config = new Config(Collections.singletonList(new ConfigEntry(configKey, configValue)));
                adminClient.alterConfigs(Collections.singletonMap(resource, config)).all().get();

                return new ResponseResult(ServiceResultStatus.DONE, true);
            } else {
                return new ResponseResult(
                        ServiceResultStatus.NO_CLIENT_FOUND,
                        false
                );
            }
        } catch (Exception e) {
            return new ResponseResult(
                    ServiceResultStatus.FAIL_TO_ADD_OR_EDIT_CONFIG,
                    false
            );
        }
    }

    // validate config key and value
    private ResponseResult validateConfigKeyValue(String configKey, String configValue) {
        if (!Validation.isValidConfigKey(configKey)) {
            return new ResponseResult(
                    ServiceResultStatus.INVALID_CONFIG_KEY,
                    false
            );
        }
        if (!Validation.isValidConfigValue(configKey, configValue)) {
            return new ResponseResult(
                    ServiceResultStatus.INVALID_CONFIG_VALUE,
                    false
            );
        }

        return new ResponseResult(ServiceResultStatus.DONE, true);
    }

    // Fetch configurations for a specific topic
    public ResponseResult getConfigs(String topicName, String connectionName) {
        try {
            Map<String, String> resultMap = new HashMap<>();
            AdminClient adminClient = clients.get(connectionName);

            if (adminClient != null) {
                ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                Map<ConfigResource, Config> configs = adminClient.describeConfigs(Collections.singletonList(resource)).all().get();

                Config topicConfig = configs.get(resource);
                for (ConfigEntry entry : topicConfig.entries()) {
                    resultMap.put(entry.name(), entry.value());
                }

                return new ResponseResult(ServiceResultStatus.DONE, true, resultMap);
            } else {
                return new ResponseResult(
                        ServiceResultStatus.NO_CLIENT_FOUND,
                        false
                );
            }
        } catch (Exception e) {
            return new ResponseResult(
                    ServiceResultStatus.FAIL_TO_ADD_OR_EDIT_CONFIG,
                    false
            );
        }
    }

    // Delete a topic configuration (set to null)
    public ResponseResult deleteConfig(String topicName, String connectionName, String configName) {
        try {
            AdminClient adminClient = clients.get(connectionName);

            if (adminClient != null) {
                ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);

                Config currentConfig = adminClient.describeConfigs(Collections.singleton(resource)).all().get().get(resource);

                Optional<ConfigEntry> entryToDelete = currentConfig.entries()
                        .stream()
                        .filter(configEntry -> configEntry.name().equals(configName))
                        .findFirst();

                if (entryToDelete.isPresent()) {
                    ConfigEntry newEntry = new ConfigEntry(configName, "false");

                    Map<String, String> updatedEntries = currentConfig.entries()
                            .stream()
                            .filter(configEntry -> !configEntry.name().equals(configName))
                            .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value));

                    updatedEntries.put(newEntry.name(), newEntry.value());

                    Config newConfig = new Config(updatedEntries.entrySet()
                            .stream()
                            .map(entry -> new ConfigEntry(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList()));

                    adminClient.alterConfigs(Collections.singletonMap(resource, newConfig)).all().get();
                    return new ResponseResult(ServiceResultStatus.DONE, true);
                } else {
                    return new ResponseResult(ServiceResultStatus.CONFIG_NOT_FOUND, false);
                }
            } else {
                return new ResponseResult(ServiceResultStatus.NO_CLIENT_FOUND, false);
            }
        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.FAIL_TO_DELETE_CONFIG, false);
        }
    }

    private KafkaConsumer<String, String> createKafkaConsumer(String connectionName, AdminClient adminClient) {
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

    // get partition details
    public ResponseResult getPartitionDetails(String topicName, String connectionName, int partition) {
        AdminClient adminClient = clients.get(connectionName);
        if (adminClient != null) {
            try {
                TopicPartition topicPartition = new TopicPartition(topicName, partition);
                // retch replica and offset details
                List<TopicPartitionInfo> partitions = adminClient.describeTopics(Collections.singletonList(topicName))
                        .all().get().get(topicName).partitions();

                TopicPartitionInfo partitionInfo = partitions.get(partition);

                Map<String, Object> details = new HashMap<>();
                details.put("replicas", partitionInfo.replicas().stream()
                        .map(replica -> Map.of("id", replica.id(), "isInSync", replica))
                        .toList());

                KafkaConsumer<String, String> consumer = createKafkaConsumer(connectionName, adminClient);
                consumer.assign(Collections.singleton(topicPartition));

                long startOffset = consumer.beginningOffsets(Collections.singleton(topicPartition)).get(topicPartition);
                long endOffset = consumer.endOffsets(Collections.singleton(topicPartition)).get(topicPartition);

                details.put("offsets", Map.of("start", startOffset, "end", endOffset, "size", endOffset - startOffset));

                consumer.close();

                return new ResponseResult(ServiceResultStatus.DONE, true, details);
            } catch (Exception e) {
                return new ResponseResult(ServiceResultStatus.FAIL_TO_GET_PARTITION_DETAIL, false);
            }
        } else {
            return new ResponseResult(ServiceResultStatus.NO_CLIENT_FOUND, false);
        }
    }
    // ===== END OF TOPICS SERVICES

    // ===== CONSUMER GROUPS SERVICES
    public ResponseResult getConsumerGroups(String serverName) {
        AdminClient adminClient = clients.get(serverName);
        if (adminClient != null) {
            try {
                Collection<ConsumerGroupListing> groups = adminClient.listConsumerGroups().all().get();
                List<String> consumerGroups = new ArrayList<>();
                for (ConsumerGroupListing group : groups) {
                    consumerGroups.add(group.groupId());
                }
                return new ResponseResult(ServiceResultStatus.DONE, true, consumerGroups);
            } catch (Exception e) {
                return new ResponseResult(ServiceResultStatus.FAIL_TO_GET_CONSUMERS, false);
            }

        } else {
            return new ResponseResult(ServiceResultStatus.NO_CLIENT_FOUND, false);
        }
    }


}
