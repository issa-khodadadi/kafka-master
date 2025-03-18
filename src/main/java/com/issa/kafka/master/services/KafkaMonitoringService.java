package com.issa.kafka.master.services;

import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class KafkaMonitoringService {
    private final KafkaService kafkaService;

    public Map<String, Object> getMonitoringData(String serverName) {
        Map<String, Object> monitoringData = new HashMap<>();
        boolean isKafkaUp = checkKafkaStatus(serverName);
        monitoringData.put("kafkaStatus", isKafkaUp ? "UP" : "DOWN");

        if (!isKafkaUp) {
            monitoringData.put("consumerVolume", new HashMap<>());
            monitoringData.put("cpuUsage", 0);
            monitoringData.put("memoryUsage", 0);
            return monitoringData;
        }

        monitoringData.put("consumerVolume", getConsumerVolume(serverName));

        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double cpuLoad = osBean.getCpuLoad() * 100;
        monitoringData.put("cpuUsage", (int) Math.max(cpuLoad, 0));

        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        int memoryUsage = (int) (((totalMemory - freeMemory) * 100) / totalMemory);
        monitoringData.put("memoryUsage", memoryUsage);

        return monitoringData;
    }

    public boolean checkKafkaStatus(String serverName) {
        try {
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
            if (!adminClientResponseResult.getIsSuccessful()) {
                return false;
            }
            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();
            adminClient.listTopics().names().get();
            return true;
        } catch (KafkaException | InterruptedException | ExecutionException e) {
            return false;
        }
    }

    private Map<String, Long> getConsumerVolume(String serverName) {
        Map<String, Long> consumerVolumes = new HashMap<>();
        try {
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
            if (!adminClientResponseResult.getIsSuccessful()) {
                return consumerVolumes;
            }
            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            ListConsumerGroupsResult consumerGroupsResult = adminClient.listConsumerGroups();
            Collection<ConsumerGroupListing> consumerGroups = consumerGroupsResult.all().get();

            for (ConsumerGroupListing group : consumerGroups) {
                Map<TopicPartition, OffsetAndMetadata> offsets = adminClient.listConsumerGroupOffsets(group.groupId()).partitionsToOffsetAndMetadata().get();
                long totalMessages = offsets.values().stream().mapToLong(OffsetAndMetadata::offset).sum();
                consumerVolumes.put(group.groupId(), totalMessages);
            }

        } catch (Exception e) {
            consumerVolumes.put("Error", 0L);
        }
        return consumerVolumes;
    }
}