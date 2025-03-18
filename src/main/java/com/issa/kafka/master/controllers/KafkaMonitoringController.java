package com.issa.kafka.master.controllers;

import com.issa.kafka.master.services.KafkaMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Controller
@RequestMapping("/monitoring")
@RequiredArgsConstructor
public class KafkaMonitoringController {

    private final KafkaMonitoringService monitoringService;

    @GetMapping("/{serverName}")
    public String showServerMonitoringPage(@PathVariable String serverName, Model model) {
        model.addAttribute("serverName", serverName);
        return "monitoring";
    }

    @GetMapping(value = "/{serverName}/realtime-data", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, ?>> streamServerMonitoringData(@PathVariable String serverName) {
        return Flux.interval(Duration.ofSeconds(2))
                .flatMap(interval -> {
                    try {
                        return Mono.just(monitoringService.getMonitoringData(serverName));
                    } catch (Exception e) {
                        return Mono.just(Map.of("error", "Failed to fetch monitoring data", "kafkaStatus", "DOWN"));
                    }
                })
                .onErrorResume(e -> {
                    return Flux.just(Map.of("error", "Stream failed, retrying...", "kafkaStatus", "DOWN"));
                });
    }

    @GetMapping(value = "/{serverName}/status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamKafkaStatus(@PathVariable String serverName) {
        return Flux.interval(Duration.ofSeconds(2))
                .map(interval -> {
                    try {
                        return monitoringService.checkKafkaStatus(serverName) ? "UP" : "DOWN";
                    } catch (Exception e) {
                        return "DOWN";
                    }
                })
                .onErrorResume(e -> Flux.just("ERROR"));
    }
}