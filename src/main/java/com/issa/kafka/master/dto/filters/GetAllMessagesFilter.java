package com.issa.kafka.master.dto.filters;

import com.issa.kafka.master.utility.Validation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetAllMessagesFilter extends TopicServerNameFilter {
    String fromDate;
    String toDate;
    Long count;
    String keyType;
    String valueType;
    String partition;
    String order;

    public boolean isRequiredFieldsNotFilled() {
        return super.isRequiredFieldsNotFilled() ||
                Validation.isNullOrBlank(topicName);
    }
}
