package com.issa.kafka.master.utility;

import com.issa.kafka.master.enums.ServiceResultStatus;
import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@PropertySource("classpath:server-status-message.properties")
public class MessageResolver {
    @Getter
    private static MessageResolver instance;

    private final MessageSource messageSource;

    public MessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
        instance = this;
    }

    public String getMessage(ServiceResultStatus status) {
        return messageSource.getMessage(
                status.name(),
                null,
                Locale.getDefault()
        );
    }
}

