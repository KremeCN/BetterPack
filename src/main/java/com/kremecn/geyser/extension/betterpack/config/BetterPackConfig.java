package com.kremecn.geyser.extension.betterpack.config;

public class BetterPackConfig {
    private String transferIp = "127.0.0.1";
    private int transferPort = 19132;
    private String defaultLocale = "en_US";
    private int fixThreshold = 80;

    public String getTransferIp() {
        return transferIp;
    }

    public void setTransferIp(String transferIp) {
        this.transferIp = transferIp;
    }

    public int getTransferPort() {
        return transferPort;
    }

    public void setTransferPort(int transferPort) {
        this.transferPort = transferPort;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public int getFixThreshold() {
        return fixThreshold;
    }

    public void setFixThreshold(int fixThreshold) {
        this.fixThreshold = fixThreshold;
    }
}
