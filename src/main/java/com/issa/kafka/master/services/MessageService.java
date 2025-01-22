package com.issa.kafka.master.services;

import com.issa.kafka.master.configs.ConfigService;
import com.issa.kafka.master.dto.filters.GetAllMessagesFilter;
import com.issa.kafka.master.dto.views.MessageView;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.utility.ResponseResult;
import com.issa.kafka.master.utility.Validation;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final KafkaService kafkaService;
    private final PartitionService partitionService;


    // get messages count on a topic
    public ResponseResult countMessagesInTopic(String topicName, String serverName) {
        try {
            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(serverName);
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            KafkaConsumer<String, String> consumer = kafkaService.createKafkaConsumer(serverName, adminClient);

            // Retrieve partition list for the topic
            List<TopicPartitionInfo> partitionInfos = adminClient.describeTopics(Collections.singletonList(topicName))
                    .all().get().get(topicName).partitions();

            List<TopicPartition> partitions = partitionInfos.stream()
                    .map(partitionInfo -> new TopicPartition(topicName, partitionInfo.partition()))
                    .toList();

            // Get beginning and end offsets for each partition
            Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

            long totalMessages = 0;

            for (TopicPartition partition : partitions) {
                long start = beginningOffsets.get(partition);
                long end = endOffsets.get(partition);
                totalMessages += (end - start);
            }

            return new ResponseResult(
                    ServiceResultStatus.DONE,
                    true,
                    totalMessages
            );
        } catch (Exception e) {
            return new ResponseResult(
                    ServiceResultStatus.FAIL_TO_GET_MESSAGE_COUNT,
                    false);
        }
    }

    // get all messages by filter
    public ResponseResult getMessagesFromTopic(GetAllMessagesFilter getAllMessagesFilter) {
        KafkaConsumer<String, String> consumer = null;
        try {
            ResponseResult validateFilter = this.validateGetMessagesFilter(getAllMessagesFilter);
            if (!validateFilter.getIsSuccessful()) {
                return validateFilter;
            }

            ResponseResult adminClientResponseResult = kafkaService.getAdminClient(getAllMessagesFilter.getServerName());
            if (!adminClientResponseResult.getIsSuccessful()) {
                return adminClientResponseResult;
            }

            AdminClient adminClient = (AdminClient) adminClientResponseResult.getResult();

            consumer = kafkaService.createKafkaConsumer(getAllMessagesFilter.getServerName(), adminClient);
            if (consumer == null) {
                return new ResponseResult(ServiceResultStatus.NO_CLIENT_FOUND, false);
            }

            List<MessageView> messages = this.retrieveMessages(consumer, getAllMessagesFilter, adminClient);
            sortMessages(messages, getAllMessagesFilter.getOrder());

            return new ResponseResult(ServiceResultStatus.DONE, true, messages);
        } catch (Exception e) {
            return handleException(e);
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    private ResponseResult validateGetMessagesFilter(GetAllMessagesFilter getAllMessagesFilter) {
        if (!Validation.isDateTimeValid(getAllMessagesFilter.getFromDate())) {
            new ResponseResult(ServiceResultStatus.FROM_DATE_INVALID, false);
        }
        if (!Validation.isDateTimeValid(getAllMessagesFilter.getToDate())) {
            new ResponseResult(ServiceResultStatus.TO_DATE_INVALID, false);
        }
        if (getAllMessagesFilter.getCount() > ConfigService.MAX_MSG_SIZE) {
            new ResponseResult(ServiceResultStatus.MESSAGE_NUMBERS_OVER_SIZED, false);
        }

        return new ResponseResult(ServiceResultStatus.DONE, true);
    }

    private List<MessageView> retrieveMessages(KafkaConsumer<String, String> consumer, GetAllMessagesFilter filter, AdminClient adminClient) {
        List<MessageView> messages = new ArrayList<>();
        long count = filter.getCount();
        String specifiedPartition = filter.getPartition();
        List<TopicPartition> partitions;

        if (!Validation.isNullOrBlank(specifiedPartition)) {
            int partitionNumber = Integer.parseInt(specifiedPartition);
            partitions = List.of(new TopicPartition(filter.getTopicName(), partitionNumber));
        } else {
            partitions = partitionService.getPartitions(filter, adminClient);
        }

        consumer.assign(partitions);

        boolean hasMoreMessages = true;
        while (hasMoreMessages && messages.size() < count) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            hasMoreMessages = !records.isEmpty();

            for (ConsumerRecord<String, String> record : records) {
                long messageTimestamp = record.timestamp();

                if (this.isWithinDateRange(messageTimestamp, filter.getFromDate(), filter.getToDate())) {
                    messages.add(new MessageView(
                            record.partition(),
                            record.offset(),
                            record.key(),
                            record.value(),
                            messageTimestamp
                    ));

                    if (messages.size() >= count) break;
                }
            }
        }

        return messages;
    }



    private boolean isWithinDateRange(long messageTime, String fromDate, String toDate) {
        long fromMillis = Long.MIN_VALUE;
        long toMillis = Long.MAX_VALUE;

        if (!Validation.isNullOrBlank(fromDate)) {
            try {
                fromMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(fromDate).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (!Validation.isNullOrBlank(toDate)) {
            try {
                toMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(toDate).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return messageTime >= fromMillis && messageTime <= toMillis;
    }

    private void sortMessages(List<MessageView> messages, String order) {
        if (Validation.isNullOrBlank(order) ||
                ConfigService.ORDER_NEWEST.equalsIgnoreCase(order)) {
            messages.sort((m1, m2) -> Long.compare(m2.getTime(), m1.getTime()));
        } else if (ConfigService.ORDER_OLDEST.equalsIgnoreCase(order)) {
            messages.sort(Comparator.comparingLong(MessageView::getTime));
        }
    }

    private ResponseResult handleException(Exception e) {
        return new ResponseResult(ServiceResultStatus.FAIL_TO_GET_MESSAGES, false);
    }
}
