package com.issa.kafka.master.controllers;

import com.issa.kafka.master.dto.filters.GetAllMessagesFilter;
import com.issa.kafka.master.dto.filters.TopicServerNameFilter;
import com.issa.kafka.master.dto.forms.SendMessageForm;
import com.issa.kafka.master.enums.ServiceResultStatus;
import com.issa.kafka.master.services.MessageService;
import com.issa.kafka.master.utility.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/messages")
public class MessageController {
    private final MessageService messageService;

    @PostMapping("/count")
    public ResponseEntity<ResponseResult> countMessages(@RequestBody TopicServerNameFilter topicServerNameFilter) {
        try {
            if (topicServerNameFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult countResponseResult =
                    messageService.countMessagesInTopic(topicServerNameFilter.getTopicName(),
                            topicServerNameFilter.getServerName());
            return ResponseEntity.ok(countResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false));
        }
    }

    @PostMapping("/getall")
    public ResponseEntity<ResponseResult> getAllMessages(@RequestBody GetAllMessagesFilter getAllMessagesFilter) {
        try {
            if (getAllMessagesFilter.isRequiredFieldsNotFilled()) {
                return ResponseEntity.ok(
                        new ResponseResult(
                                ServiceResultStatus.FIELDS_REQUIRED,
                                false));
            }

            ResponseResult getAllMessageResponseResult = messageService.getMessagesFromTopic(getAllMessagesFilter);

            return ResponseEntity.ok(getAllMessageResponseResult);

        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false));
        }
    }


    @PostMapping("/sendsingle")
    public ResponseEntity<ResponseResult> sendMessage(@RequestBody SendMessageForm sendMessageForm) {
        try {
            ResponseResult sendMessageResponseResult =
                    messageService.sendMessage(sendMessageForm.getTopicName(), sendMessageForm.getServerName(),
                            sendMessageForm.getPartition(), sendMessageForm.getMessage().get(0));

            return ResponseEntity.ok(sendMessageResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false));
        }
    }

    @PostMapping("/sendmultiple")
    public ResponseEntity<ResponseResult> sendMessages(@RequestBody SendMessageForm sendMessageForm) {
        try {
            ResponseResult sendMessageResponseResult =
                    messageService.sendMessages(sendMessageForm.getTopicName(), sendMessageForm.getServerName(),
                            sendMessageForm.getPartition(), sendMessageForm.getMessage());

            return ResponseEntity.ok(sendMessageResponseResult);
        } catch (Exception e) {
            return ResponseEntity.ok(
                    new ResponseResult(
                            ServiceResultStatus.ERROR,
                            false));
        }
    }
}
