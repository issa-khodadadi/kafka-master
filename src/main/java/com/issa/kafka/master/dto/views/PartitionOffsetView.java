package com.issa.kafka.master.dto.views;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PartitionOffsetView {
    Integer partition;
    Long offset;
}
