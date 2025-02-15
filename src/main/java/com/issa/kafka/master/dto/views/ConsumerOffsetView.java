package com.issa.kafka.master.dto.views;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConsumerOffsetView extends PartitionOffsetView {
    String topic;
    Long startOffset;
    Long endOffset;
    Long lag;
}
