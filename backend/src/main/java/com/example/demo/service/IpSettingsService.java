package com.example.demo.service;

import com.example.demo.model.IpSettings;
import com.example.demo.model.IpSettings.Modem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IpSettingsService {
    private final IpSettings ipSettings = new IpSettings();

    public IpSettingsService() {
        // Optional: preload default modems
        addModem(new Modem("modem1", "Modem 1", "192.168.1.1", false));
        addModem(new Modem("modem2", "Modem 2", "192.168.2.1", false));
    }

    public IpSettings getSettings() {
        return ipSettings;
    }

    public void addModem(Modem modem) {
        ipSettings.getModems().add(modem);
    }

    public boolean removeModemById(String id) {
        return ipSettings.getModems().removeIf(modem -> modem.getId().equalsIgnoreCase(id));
    }

    public Optional<Modem> getModemById(String id) {
        return ipSettings.getModems().stream()
                .filter(m -> m.getId().equalsIgnoreCase(id))
                .findFirst();
    }

    public boolean updateModemIp(String id, String ip) {
        return getModemById(id).map(m -> {
            m.setIp(ip);
            return true;
        }).orElse(false);
    }

    public boolean updateModemName(String id, String name) {
        return getModemById(id).map(m -> {
            m.setName(name);
            return true;
        }).orElse(false);
    }

    public boolean updateModemPower(String id, boolean on) {
        return getModemById(id).map(m -> {
            m.setPower(on);
            return true;
        }).orElse(false);
    }
}
