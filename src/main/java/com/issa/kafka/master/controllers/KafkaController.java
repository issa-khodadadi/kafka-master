package com.issa.kafka.master.controllers;

import com.issa.kafka.master.dto.filters.*;
import com.issa.kafka.master.dto.forms.AddEditConfigForm;
import com.issa.kafka.master.dto.forms.BaseTopicForm;
import com.issa.kafka.master.dto.forms.ConnectionForm;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.services.KafkaService;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
public class KafkaController {
    private final KafkaService kafkaService;


    // START AND CONNECTION APIS
    @GetMapping("/")
    public String index() {
        kafkaService.removeAllConnections();
        return "connect";
    }

    @PostMapping("/connect")
    public ResponseEntity<ResponseResult> connect(@RequestBody ConnectionForm form) {
        try {
            if (form.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult serviceResult = kafkaService.connectToServer(form);
            return ResponseEntity.ok(serviceResult);
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false)
            );
        }
    }

    @GetMapping("/main")
    public String showMainPage(Model model) {
        model.addAttribute("connections", kafkaService.getConnections());
        return "main-page";
    }

    // disconnect reconnect
    @PostMapping("/disconnect")
    public ResponseEntity<ResponseResult> disconnect(@RequestBody ServerNameFilter serverFilter) {
        try {
            ResponseResult disconnectResponse = kafkaService.disconnectServer(serverFilter.getServerName());
            return ResponseEntity.ok(disconnectResponse);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

    @PostMapping("/reconnect")
    public ResponseEntity<ResponseResult> reconnect(@RequestBody ServerNameFilter serverFilter) {
        try {
            ResponseResult reconnectResponse = kafkaService.reconnectServer(serverFilter.getServerName());
            return ResponseEntity.ok(reconnectResponse);
        } catch (Exception e) {
            return ResponseEntity.ok(new ResponseResult(ServiceResultStatus.ERROR, false));
        }
    }

}
