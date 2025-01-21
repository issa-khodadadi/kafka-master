package com.issa.kafka.master.utility;

import com.issa.kafka.master.enums.ServiceResultStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResponseResult {
    ServiceResultStatus status;
    Boolean isSuccessful;
    String message;
    Object result;

    public ResponseResult(ServiceResultStatus status, Boolean isSuccessful) {
        this.status = status;
        this.isSuccessful = isSuccessful;

        MessageResolver resolver = MessageResolver.getInstance();
        if (resolver != null) {
            this.message = resolver.getMessage(status);
        } else {
            this.message = "MessageResolver is not initialized.";
        }
    }

    public ResponseResult(ServiceResultStatus status, Boolean isSuccessful, Object result) {
        this.status = status;
        this.isSuccessful = isSuccessful;

        MessageResolver resolver = MessageResolver.getInstance();
        if (resolver != null) {
            this.message = resolver.getMessage(status);
        } else {
            this.message = "MessageResolver is not initialized.";
        }

        this.result = result;
    }
}
