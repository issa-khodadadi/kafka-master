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
import org.apache.kafka.common.errors.UnknownMemberIdException;
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

    public ResponseResult getConsumerOffsets(String serverName, String consumerName) {
        try {
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();
            DescribeConsumerGroupsResult consumerGroupsResult = adminClient.describeConsumerGroups(Collections.singleton(consumerName));
            ConsumerGroupDescription consumerGroupDescription = consumerGroupsResult.describedGroups().get(consumerName).get();
            ConsumerGroupState groupState = consumerGroupDescription.state();

            // Check if the consumer is paused
            Boolean isPaused = (groupState == ConsumerGroupState.STABLE && consumerGroupDescription.members().isEmpty()) ||
                    (groupState == ConsumerGroupState.EMPTY);

            ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(consumerName);
            Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata().get();

            List<ConsumerOffsetView> offsetList = new ArrayList<>();
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                TopicPartition partition = entry.getKey();
                OffsetAndMetadata metadata = entry.getValue();
                Long startOffset = adminClient.listOffsets(Map.of(partition, OffsetSpec.earliest())).all().get().get(partition).offset();
                Long endOffset = adminClient.listOffsets(Map.of(partition, OffsetSpec.latest())).all().get().get(partition).offset();

                ConsumerOffsetView offsetView = new ConsumerOffsetView();
                offsetView.setTopic(partition.topic());
                offsetView.setPartition(partition.partition());
                offsetView.setStartOffset(startOffset);
                offsetView.setEndOffset(endOffset);
                offsetView.setOffset(metadata.offset());
                offsetView.setLag(Math.max(endOffset - metadata.offset(), 0));
                offsetView.setIsPaused(isPaused); // Send pause state to frontend

                offsetList.add(offsetView);
            }

            return new ResponseResult(ServiceResultStatus.DONE, true, offsetList);
        } catch (Exception e) {
            return new ResponseResult(ServiceResultStatus.ERROR, false);
        }
    }

    public ResponseResult updateConsumerOffset(String serverName, String consumerName, String topic,
                                               Integer partition, Long offset, String offsetType) {
        ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
        try {
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(consumerName);
            Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata().get();

            TopicPartition targetPartition = new TopicPartition(topic, partition);
            Long newOffset = offset;

            if ("START".equals(offsetType)) {
                newOffset = adminClient.listOffsets(Map.of(targetPartition, OffsetSpec.earliest())).all().get()
                        .get(targetPartition).offset();
            } else if ("END".equals(offsetType)) {
                newOffset = adminClient.listOffsets(Map.of(targetPartition, OffsetSpec.latest())).all().get()
                        .get(targetPartition).offset();
            }

            OffsetAndMetadata currentOffsetAndMetadata = offsets.get(targetPartition);
            if (currentOffsetAndMetadata != null && currentOffsetAndMetadata.offset() == newOffset) {
                return new ResponseResult(ServiceResultStatus.NO_UPDATE_NEEDED_FOR_OFFSET, false);
            }

            adminClient.alterConsumerGroupOffsets(consumerName, Map.of(targetPartition, new OffsetAndMetadata(newOffset))).all().get();

            return new ResponseResult(ServiceResultStatus.DONE, true);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseResult(ServiceResultStatus.FAIL_TO_UPDATE_OFFSET, false);
        }
    }
}
