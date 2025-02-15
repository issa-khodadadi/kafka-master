package com.issa.kafka.master.services;

import com.issa.kafka.master.dto.views.ConsumerDetailView;
import com.issa.kafka.master.dto.views.ConsumerOffsetView;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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

            return new ResponseResult(ServiceResultStatus.DONE, true, consumerGroups.stream().sorted());
        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.FAIL_TO_GET_CONSUMERS, false);
        }
    }

    public ResponseResult getConsumerDetails(String serverName, String consumerGroup) {
        ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
        try {
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            DescribeConsumerGroupsResult describeConsumerGroupsResult =
                    adminClient.describeConsumerGroups(Collections.singletonList(consumerGroup));

            Map<String, ConsumerGroupDescription> consumerGroups = describeConsumerGroupsResult.all().get();

            if (!consumerGroups.containsKey(consumerGroup)) {
                return new ResponseResult(ServiceResultStatus.CONSUMER_GRP_NOT_FOUND, false);
            }

            ConsumerGroupDescription groupDescription = consumerGroups.get(consumerGroup);
            boolean isActive = groupDescription.state() == ConsumerGroupState.STABLE;
            String offsetStoredIn = groupDescription.coordinator().toString();
            boolean autoCommit = groupDescription.isSimpleConsumerGroup();

            ConsumerDetailView consumerDetailView = new ConsumerDetailView();
            consumerDetailView.setIsActive(isActive);
            consumerDetailView.setConsumerType("Kafka Consumer");
            consumerDetailView.setOffsetStoredIn(offsetStoredIn);
            consumerDetailView.setAutoCommit(autoCommit);

            return new ResponseResult(ServiceResultStatus.DONE, true, consumerDetailView);

        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.FAIL_TO_GET_CONSUMER_DETAIL, false);
        }
    }

    public ResponseResult getConsumerOffsets(String serverName, String consumerGroup) {
        ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
        try {
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }
            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(consumerGroup);
            Map<TopicPartition, OffsetAndMetadata> committedOffsets = offsetsResult.partitionsToOffsetAndMetadata().get();

            Set<TopicPartition> partitions = committedOffsets.keySet();
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> earliestOffsets =
                    adminClient.listOffsets(partitions.stream()
                                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.earliest())))
                            .all().get();

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets =
                    adminClient.listOffsets(partitions.stream()
                                    .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest())))
                            .all().get();

            List<ConsumerOffsetView> offsetList = new ArrayList<>();
            for (TopicPartition partition : partitions) {
                ConsumerOffsetView offsetView = new ConsumerOffsetView();
                offsetView.setTopic(partition.topic());
                offsetView.setPartition(partition.partition());

                Long startOffset = earliestOffsets.get(partition).offset();
                Long endOffset = latestOffsets.get(partition).offset();
                Long committedOffset = committedOffsets.get(partition).offset();
                Long lag = Math.max(endOffset - committedOffset, 0);

                offsetView.setStartOffset(startOffset);
                offsetView.setEndOffset(endOffset);
                offsetView.setOffset(committedOffset);
                offsetView.setLag(lag);

                offsetList.add(offsetView);
            }

            return new ResponseResult(ServiceResultStatus.DONE, true, offsetList);
        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.FAIL_TO_GET_CONSUMER_OFFSET, false);
        }
    }
}
