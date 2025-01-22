package com.issa.kafka.master.controllers;

import com.issa.kafka.master.dto.filters.ConfigDeletionFilter;
import com.issa.kafka.master.dto.filters.ServerNameFilter;
import com.issa.kafka.master.dto.forms.AddEditConfigForm;
import com.issa.kafka.master.dto.forms.BaseTopicForm;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.services.KafkaService;
import com.issa.kafka.master.services.TopicService;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/topics")
public class TopicController {
    private final TopicService topicService;

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

    @PostMapping("/getall")
    public ResponseEntity<ResponseResult> getTopics(@RequestBody ServerNameFilter serverNameFilter) {
        try {
            if (serverNameFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult topicsResponseResult = topicService.getTopicsForServer(serverNameFilter.getServerName());
            return ResponseEntity.ok(topicsResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false));
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

            ResponseResult getAllConfigResponseResult = topicService.getConfigs(topicForm.getTopicName(), topicForm.getServerName());

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

            ResponseResult addEditResponseResult = topicService.addOrEditConfig(addEditConfigForm);

            return ResponseEntity.ok(addEditResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

    // Delete a configuration
    @PostMapping("/deleteconfig")
    public ResponseEntity<ResponseResult> deleteConfig(@RequestBody ConfigDeletionFilter configDeletionFilter) {
        try {
            if (configDeletionFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult deleteConfigResponseResult =
                    topicService.deleteConfig(configDeletionFilter.getTopicName(),
                            configDeletionFilter.getServerName(), configDeletionFilter.getConfigName());

            return ResponseEntity.ok(deleteConfigResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }
}
