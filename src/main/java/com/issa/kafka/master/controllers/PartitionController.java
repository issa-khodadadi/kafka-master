package com.issa.kafka.master.controllers;

import com.issa.kafka.master.dto.filters.PartitionFilter;
import com.issa.kafka.master.dto.filters.TopicServerNameFilter;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.services.KafkaService;
import com.issa.kafka.master.services.PartitionService;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/partitions")
public class PartitionController {
    private final PartitionService partitionService;

    @PostMapping("/getall")
    public ResponseEntity<ResponseResult> getPartitions(@RequestBody TopicServerNameFilter topicServerNameFilter) {
        try {
            if (topicServerNameFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult getPartitionsResponseResult =
                    partitionService.getPartitionsForTopic(topicServerNameFilter.getTopicName(), topicServerNameFilter.getServerName());

            return ResponseEntity.ok(getPartitionsResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

    @PostMapping("/detail")
    public ResponseEntity<ResponseResult> getPartitionDetails(@RequestBody PartitionFilter partitionFilter) {
        try {
            if (partitionFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult partitionDeailResponseResult =
                    partitionService.getPartitionDetails(partitionFilter.getTopicName(),
                            partitionFilter.getServerName(), partitionFilter.getPartition());

            return ResponseEntity.ok(partitionDeailResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

}
