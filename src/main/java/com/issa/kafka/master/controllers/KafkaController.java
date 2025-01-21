package com.issa.kafka.master.controllers;

import com.issa.kafka.master.dto.filters.*;
import com.issa.kafka.master.dto.forms.AddEditConfigForm;
import com.issa.kafka.master.dto.forms.BaseTopicForm;
import com.issa.kafka.master.dto.forms.ConnectionForm;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.services.KafkaService;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class KafkaController {
    private final KafkaService kafkaService;


    // START AND CONNECTION APIS
    @GetMapping("/")
    public String index() {
        kafkaService.removeAllConnections();
        return "connect";
    }

    @PostMapping("/connect")
    public ResponseEntity<ResponseResult> connect(@RequestBody ConnectionForm form) {
        try {
            if (form.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult serviceResult = kafkaService.connectToServer(form);
            return ResponseEntity.ok(serviceResult);
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false)
            );
        }
    }

    @GetMapping("/main")
    public String showMainPage(Model model) {
        model.addAttribute("connections", kafkaService.getConnections());
        return "main-page";
    }

    // TOPICS APIS
    /*@PostMapping("/savecontenttype")
    public ResponseEntity<ResponseResult> saveTopicContentType(@RequestBody DoubleStringForm doubleStringForm) {
        try {
            if (doubleStringForm.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult topicsResponseResult = kafkaService.saveContentType(doubleStringForm.getKey(), doubleStringForm.getValue());
            return ResponseEntity.ok(topicsResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false));
        }
    }*/

    @PostMapping("/topics")
    public ResponseEntity<ResponseResult> getTopics(@RequestBody ServerNameFilter serverNameFilter) {
        try {
            if (serverNameFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult topicsResponseResult = kafkaService.getTopicsForServer(serverNameFilter.getServerName());
            return ResponseEntity.ok(topicsResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false));
        }
    }

    @PostMapping("/messages/count")
    public ResponseEntity<ResponseResult> countMessages(@RequestBody TopicServerNameFilter topicServerNameFilter) {
        try {
            if (topicServerNameFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult countResponseResult = kafkaService.countMessagesInTopic(topicServerNameFilter.getTopicName(), topicServerNameFilter.getServerName());
            return ResponseEntity.ok(countResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false));
        }
    }

    @PostMapping("/messages/getall")
    public ResponseEntity<ResponseResult> getAllMessages(@RequestBody GetAllMessagesFilter getAllMessagesFilter) {
        try {
            if (getAllMessagesFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult getAllMessageResponseResult = kafkaService.getMessagesFromTopic(getAllMessagesFilter);

            return ResponseEntity.ok(getAllMessageResponseResult);

        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false));
        }
    }

    @PostMapping("/partitions")
    public ResponseEntity<ResponseResult> getPartitions(@RequestBody TopicServerNameFilter topicServerNameFilter) {
        try {
            if (topicServerNameFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult getPartitionsResponseResult =
                    kafkaService.getPartitionsForTopic(topicServerNameFilter.getTopicName(), topicServerNameFilter.getServerName());

            return ResponseEntity.ok(getPartitionsResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

    // get topic config
    @PostMapping("/getallconfigs")
    public ResponseEntity<ResponseResult> getConfigs(@RequestBody BaseTopicForm topicForm) {
        try {
            if (topicForm.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult getAllConfigResponseResult = kafkaService.getConfigs(topicForm.getTopicName(), topicForm.getServerName());

            return ResponseEntity.ok(getAllConfigResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

    // Add or edit a configuration
    @PostMapping("/addoreditconfig")
    public ResponseEntity<ResponseResult> addOrEditConfig(@RequestBody AddEditConfigForm addEditConfigForm) {
        try {
            if (addEditConfigForm.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult addEditResponseResult = kafkaService.addOrEditConfig(addEditConfigForm);

            return ResponseEntity.ok(addEditResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

    // Delete a configuration
    @PostMapping("/delete")
    public ResponseEntity<ResponseResult> deleteConfig(@RequestBody ConfigDeletionFilter configDeletionFilter) {
        try {
            if (configDeletionFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult deleteConfigResponseResult =
                    kafkaService.deleteConfig(configDeletionFilter.getTopicName(),
                            configDeletionFilter.getServerName(), configDeletionFilter.getConfigName());

            return ResponseEntity.ok(deleteConfigResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

    @PostMapping("/partition/details")
    public ResponseEntity<ResponseResult> getPartitionDetails(@RequestBody PartitionFilter partitionFilter) {
        try {
            if (partitionFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult partitionDeailResponseResult =
                    kafkaService.getPartitionDetails(partitionFilter.getTopicName(),
                            partitionFilter.getServerName(), partitionFilter.getPartition());

            return ResponseEntity.ok(partitionDeailResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

    // CONSUMER GROUP APIS
    // get consumer list
    @PostMapping("/consumers")
    public ResponseEntity<ResponseResult> getConsumers(@RequestBody ServerNameFilter serverNameFilter) {
        try {
            if (serverNameFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult getConsumersResponseResult =
                    kafkaService.getConsumerGroups(serverNameFilter.getServerName());

            return ResponseEntity.ok(getConsumersResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }
}
