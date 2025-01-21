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
public class ServerNameFilter {
    String serverName;

    public boolean isRequiredFieldsNotFilled() {
        return Validation.isNullOrBlank(serverName);
    }
}
