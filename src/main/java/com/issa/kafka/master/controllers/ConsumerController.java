package com.issa.kafka.master.controllers;

import com.issa.kafka.master.dto.filters.ConsumerServerNameFilter;
import com.issa.kafka.master.dto.filters.ServerNameFilter;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.services.ConsumerService;
import com.issa.kafka.master.services.KafkaService;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/consumers")
public class ConsumerController {
    private final ConsumerService consumerService;

    // get consumer list
    @PostMapping("/getall")
    public ResponseEntity<ResponseResult> getConsumers(@RequestBody ServerNameFilter serverNameFilter) {
        try {
            if (serverNameFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult getConsumersResponseResult =
                    consumerService.getConsumerGroups(serverNameFilter.getServerName());

            return ResponseEntity.ok(getConsumersResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

    @PostMapping("/details")
    public ResponseEntity<ResponseResult> getConsumerDetails(@RequestBody ConsumerServerNameFilter serverNameFilter) {
        try {
            if (serverNameFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult consumerDetails =
                    consumerService.getConsumerDetails(serverNameFilter.getServerName(),
                            serverNameFilter.getConsumerName());

            return ResponseEntity.ok(consumerDetails);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }
}
