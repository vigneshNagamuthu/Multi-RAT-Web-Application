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
        private String interfaceName;
        private String port; // ✅ New field added

        public Modem() {}

        public Modem(String id, String name, String ip, boolean power, String interfaceName) {
            this.id = id;
            this.name = name;
            this.ip = ip;
            this.power = power;
            this.interfaceName = interfaceName;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }

        public boolean isPower() { return power; }
        public void setPower(boolean power) { this.power = power; }

        public String getInterfaceName() { return interfaceName; }
        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }

        public String getPort() { return port; }
        public void setPort(String port) { this.port = port; } // ✅ Getter/setter for port
    }
}