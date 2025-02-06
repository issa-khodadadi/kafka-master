package com.issa.kafka.master.dto.forms;

import com.issa.kafka.master.utility.Validation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageForm extends BaseTopicForm {
    int partition;
    List<Map<String, String>> message;

    public boolean isRequiredFieldsNotFilled() {
        return super.isRequiredFieldsNotFilled() ||
                Validation.isNullOrZero(partition) ||
                isMessageNullOrEmpty() ||
                isAnyValueNullInMessage();
    }

    private boolean isMessageNullOrEmpty() {
        return Optional.ofNullable(message)
                .map(List::isEmpty)
                .orElse(true);
    }

    private boolean isAnyValueNullInMessage() {
        return Optional.ofNullable(message)
                .flatMap(msgList -> msgList.stream()
                        .flatMap(map -> map.values().stream())
                        .filter(Validation::isNullOrBlank)
                        .findAny())
                .isPresent();
    }
}
