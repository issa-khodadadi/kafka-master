package com.issa.kafka.master.dto.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerOffsetUpdateForm extends BaseTopicForm {
    String consumerName;
    Integer partition;
    Long offset;
    String offsetType;


    public boolean isRequiredFieldsNotFilled() {
        return super.isRequiredFieldsNotFilled();
    }
}
