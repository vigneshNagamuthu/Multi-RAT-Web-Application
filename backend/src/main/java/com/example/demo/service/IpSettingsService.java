package com.example.demo.service;

import com.example.demo.model.IpSettings;
import org.springframework.stereotype.Service;

@Service
public class IpSettingsService {
    private final IpSettings ipSettings = new IpSettings();

    public IpSettings getSettings() {
        return ipSettings;
    }

    public void updateIf1(String ip) {
        ipSettings.setIf1(ip);
    }

    public void updateIf2(String ip) {
        ipSettings.setIf2(ip);
    }

    public void updateServer(String ip) {
        ipSettings.setServer(ip);
    }
}
