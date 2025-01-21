package com.issa.kafka.master.configs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@RequiredArgsConstructor
@Configuration("configService")
public class ConfigService {
    public static String ORDER_NEWEST;
    public static String ORDER_OLDEST;
    public static Long MAX_MSG_SIZE;

    @Value("${com.kafka.get-message-filter-order-newest}")
    public void setOrderNewest(String ORDER_NEWEST) {
        ConfigService.ORDER_NEWEST = ORDER_NEWEST;
    }

    @Value("${com.kafka.get-message-filter-order-oldest}")
    public void setOrderOldest(String ORDER_OLDEST) {
        ConfigService.ORDER_OLDEST = ORDER_OLDEST;
    }

    @Value("${com.kafka.get-message-filter-max-message-size}")
    public void setMaxMsgSize(Long MAX_MSG_SIZE) {
        ConfigService.MAX_MSG_SIZE = MAX_MSG_SIZE;
    }
}
