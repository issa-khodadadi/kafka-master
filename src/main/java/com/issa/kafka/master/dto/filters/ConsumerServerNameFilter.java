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
public class ConsumerServerNameFilter extends ServerNameFilter {
    String consumerName;

    public boolean isRequiredFieldsNotFilled() {
        return super.isRequiredFieldsNotFilled() ||
                Validation.isNullOrBlank(consumerName);
    }
}
