package com.issa.kafka.master.dto.forms;

import com.issa.kafka.master.utility.Validation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionForm extends BaseForm {
    String serverIP;
    String serverPort;


    public boolean isRequiredFieldsNotFilled() {
        return super.isRequiredFieldsNotFilled() ||
                Validation.isNullOrBlank(serverIP) ||
                Validation.isNullOrBlank(serverPort);
    }
}
