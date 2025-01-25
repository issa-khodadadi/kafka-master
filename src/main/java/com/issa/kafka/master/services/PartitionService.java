package com.issa.kafka.master.services;

import com.issa.kafka.master.dto.filters.GetAllMessagesFilter;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PartitionService {
    private final KafkaService kafkaService;

    // get partitions of a topic
    public ResponseResult getPartitionsForTopic(String topicName, String serverName) throws ExecutionException, InterruptedException {
        ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
        if (!adminClientResponseResult.getIsSuccessful()) {
            return adminClientResponseResult;
        }

        AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

        DescribeTopicsResult result = adminClient.describeTopics(List.of(topicName));
        TopicDescription description = result.all().get().get(topicName);

        List<Integer> partitionList = description.partitions().stream()
                .map(TopicPartitionInfo::partition)
                .toList();

        return new ResponseResult(ServiceResultStatus.DONE, true, partitionList);
    }

    public List<TopicPartition> getPartitions(GetAllMessagesFilter filter, AdminClient adminClient) {
        try {
            String topicName = filter.getTopicName();

            List<TopicPartitionInfo> partitionInfos = adminClient.describeTopics(Collections.singletonList(topicName))
                    .all().get().get(topicName).partitions();

            return partitionInfos.stream()
                    .map(partitionInfo -> new TopicPartition(topicName, partitionInfo.partition()))
                    .toList();
        } catch (Exception e) {
            // TODO: should change the exception output
            return List.of();
        }
    }

    // get partition details
    public ResponseResult getPartitionDetails(String topicName, String connectionName, int partition) {
        try {
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(connectionName);
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            TopicPartition topicPartition = new TopicPartition(topicName, partition);
            // retch replica and offset details
            List<TopicPartitionInfo> partitions = adminClient.describeTopics(Collections.singletonList(topicName))
                    .all().get().get(topicName).partitions();

            TopicPartitionInfo partitionInfo = partitions.get(partition);

            Map<String, Object> details = new HashMap<>();
            details.put("replicas", partitionInfo.replicas().stream()
                    .map(replica -> Map.of("id", replica.id(), "isInSync", replica))
                    .toList());

            KafkaConsumer<String, String> consumer = kafkaService.createKafkaConsumer(connectionName, adminClient);
            consumer.assign(Collections.singleton(topicPartition));

            long startOffset = consumer.beginningOffsets(Collections.singleton(topicPartition)).get(topicPartition);
            long endOffset = consumer.endOffsets(Collections.singleton(topicPartition)).get(topicPartition);

            details.put("offsets", Map.of("start", startOffset, "end", endOffset, "size", endOffset - startOffset));

            consumer.close();

            return new ResponseResult(ServiceResultStatus.DONE, true, details);
        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.FAIL_TO_GET_PARTITION_DETAIL, false);
        }
    }
}
