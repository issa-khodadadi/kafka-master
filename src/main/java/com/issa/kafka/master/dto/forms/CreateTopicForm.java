package com.issa.kafka.master.dto.forms;

import com.issa.kafka.master.utility.Validation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTopicForm extends BaseTopicForm {
    int partitions;
    int replicas;
    Map<String, String> configurations;

    public boolean isRequiredFieldsNotFilled() {
        return super.isRequiredFieldsNotFilled() ||
                Validation.isNullOrZero(partitions) ||
                Validation.isNullOrZero(replicas) ||
                configurations == null || configurations.isEmpty();
    }
}
