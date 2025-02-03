package com.issa.kafka.master.services;

import com.issa.kafka.master.dto.forms.AddEditConfigForm;
import com.issa.kafka.master.dto.forms.CreateTopicForm;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.utility.ResponseResult;
import com.issa.kafka.master.utility.Validation;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {
    private final KafkaService kafkaService;

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
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            List<String> topics = adminClient.listTopics().names().get().stream().toList();
            return new ResponseResult(
                    ServiceResultStatus.DONE,
                    true,
                    topics
            );

        } catch (Exception e) {
            return new ResponseResult(
                    ServiceResultStatus.FAIL_TO_GET_TOPICS,
                    false);
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

            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(addEditConfigForm.getServerName());
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, addEditConfigForm.getTopicName());
            Config config = new Config(Collections.singletonList(new ConfigEntry(configKey, configValue)));
            adminClient.alterConfigs(Collections.singletonMap(resource, config)).all().get();

            return new ResponseResult(ServiceResultStatus.DONE, true);
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
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(connectionName);
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
            Map<ConfigResource, Config> configs = adminClient.describeConfigs(Collections.singletonList(resource)).all().get();

            Config topicConfig = configs.get(resource);
            for (ConfigEntry entry : topicConfig.entries()) {
                resultMap.put(entry.name(), entry.value());
            }

            return new ResponseResult(ServiceResultStatus.DONE, true, resultMap);
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
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(connectionName);
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

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
        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.FAIL_TO_DELETE_CONFIG, false);
        }
    }

    // Create Kafka topic
    public ResponseResult createTopic(CreateTopicForm createTopicForm) {
        ResponseResult validateCreateTopicFormResponseResult = this.validateCreateTopicForm(createTopicForm);
        if (!validateCreateTopicFormResponseResult.getIsSuccessful()) {
            return validateCreateTopicFormResponseResult;
        }

        try {
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(createTopicForm.getServerName());
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            List<String> topics = adminClient.listTopics().names().get().stream().toList();
            String topicName = createTopicForm.getTopicName();
            // Check if the topic name exists in the list
            if (topics.contains(topicName)) {
                return new ResponseResult(ServiceResultStatus.TOPIC_NAME_DUPLICATE, false);
            }

            NewTopic newTopic = new NewTopic(createTopicForm.getTopicName(), createTopicForm.getPartitions(), (short) createTopicForm.getReplicas())
                    .configs(createTopicForm.getConfigurations());

            CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));
            KafkaFuture<Void> future = result.values().get(createTopicForm.getTopicName());

            // Wait for topic creation
            future.get();
            return new ResponseResult(ServiceResultStatus.DONE, true);

        } catch (InterruptedException | ExecutionException e) {
            return new ResponseResult(
                    ServiceResultStatus.FAIL_TO_CREATE_TOPIC,
                    false
            );
        }
    }

    // validate create topic form
    private ResponseResult validateCreateTopicForm(CreateTopicForm createTopicForm) {
        if (Validation.isMoreThanSelectedLength(createTopicForm.getTopicName(), 50)) {
            return new ResponseResult(ServiceResultStatus.TOPIC_NAME_LENGTH_IS_OVERFLOW, false);
        }
        if (createTopicForm.getPartitions() > 1000) {
            return new ResponseResult(ServiceResultStatus.INVALID_PARTITION_COUNT, false);
        }
        if (createTopicForm.getReplicas() > 10) {
            return new ResponseResult(ServiceResultStatus.INVALID_REPLICA_COUNT, false);
        }

        for(Map.Entry<String, String> entry : createTopicForm.getConfigurations().entrySet()) {
            if (!Validation.isValidConfigKey(entry.getKey())) {
                return new ResponseResult(
                        ServiceResultStatus.INVALID_CONFIG_KEY,
                        false
                );
            }
            if (!Validation.isValidConfigValue(entry.getKey(), entry.getValue())) {
                return new ResponseResult(
                        ServiceResultStatus.INVALID_CONFIG_VALUE,
                        false
                );
            }
        }

        return new ResponseResult(ServiceResultStatus.DONE, true);
    }

    // delete topic
    public ResponseResult deleteTopic(String topicName, String connectionName) {
        try {
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(connectionName);
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            DeleteTopicsResult deleteTopicsResult = adminClient.deleteTopics(Collections.singleton(topicName));

            deleteTopicsResult.all().get();
            return new ResponseResult(ServiceResultStatus.DONE, true);
        } catch (InterruptedException | ExecutionException e) {
            return new ResponseResult(ServiceResultStatus.FAIL_TO_DELETE_TOPIC, false);
        }
    }
}
