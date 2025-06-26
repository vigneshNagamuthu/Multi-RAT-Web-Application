package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

public class IpSettings {
    private List<Modem> modems = new ArrayList<>();

    public List<Modem> getModems() {
        return modems;
    }

    public void setModems(List<Modem> modems) {
        this.modems = modems;
    }

    public static class Modem {
        private String id;
        private String name;
        private String ip;
        private boolean power;

        public Modem() {}

        public Modem(String id, String name, String ip, boolean power) {
            this.id = id;
            this.name = name;
            this.ip = ip;
            this.power = power;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }

        public boolean isPower() { return power; }
        public void setPower(boolean power) { this.power = power; }
    }
}
