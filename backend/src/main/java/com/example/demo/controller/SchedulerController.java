package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class SchedulerController {

    @GetMapping("/api/schedulers")
    public List<String> getAvailableSchedulers() {
        List<String> schedulers = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("cat /proc/sys/net/mptcp/available_schedulers");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip lines that start with an underscore or are empty
                if (line.trim().isEmpty() || line.trim().startsWith("_")) continue;
                // Split the first non-underscore, non-empty line
                schedulers = Arrays.asList(line.trim().split("\\s+"));
                break;
            }
        } catch (Exception e) {
            schedulers = List.of("default");
        }
        return schedulers;
    }
} 