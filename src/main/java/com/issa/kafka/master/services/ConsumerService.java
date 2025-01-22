package com.issa.kafka.master.services;

import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsumerService {
    private final KafkaService kafkaService;

    public ResponseResult getConsumerGroups(String serverName) {
        ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
        try {
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            Collection<ConsumerGroupListing> groups = adminClient.listConsumerGroups().all().get();
            List<String> consumerGroups = new ArrayList<>();
            for (ConsumerGroupListing group : groups) {
                consumerGroups.add(group.groupId());
            }
            return new ResponseResult(ServiceResultStatus.DONE, true, consumerGroups);
        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.FAIL_TO_GET_CONSUMERS, false);
        }
    }
}
