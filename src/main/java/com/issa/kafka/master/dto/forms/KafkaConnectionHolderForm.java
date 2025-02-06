package com.issa.kafka.master.dto.forms;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KafkaConnectionHolderForm {
    private String serverName;
    private String serverIP;
    private String serverPort;
    private Boolean isConnected;
}
