package com.issa.kafka.master.dto.views;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConsumerDetailView {
    Boolean isActive;
    String consumerType;
    String offsetStoredIn;
    Boolean autoCommit;
}