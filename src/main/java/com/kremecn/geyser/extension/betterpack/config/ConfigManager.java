package com.kremecn.geyser.extension.betterpack.config;

import com.kremecn.geyser.extension.betterpack.util.LoggerUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public class ConfigManager {

    private final Path dataFolder;
    private BetterPackConfig config;

    public ConfigManager(Path dataFolder) {
        this.dataFolder = dataFolder;
        this.config = new BetterPackConfig();
    }

    public void loadConfig() {
        File configFile = dataFolder.resolve("config.yml").toFile();

        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        try (InputStream inputStream = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(inputStream);

            if (data != null) {
                if (data.containsKey("transfer-ip")) {
                    config.setTransferIp((String) data.get("transfer-ip"));
                }
                if (data.containsKey("transfer-port")) {
                    config.setTransferPort((int) data.get("transfer-port"));
                }
                if (data.containsKey("default-locale")) {
                    config.setDefaultLocale((String) data.get("default-locale"));
                }
                if (data.containsKey("fix-threshold")) {
                    config.setFixThreshold((int) data.get("fix-threshold"));
                }
            }
            LoggerUtil.info("Configuration loaded.");
        } catch (Exception e) {
            LoggerUtil.error("Failed to load config.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveDefaultConfig() {
        File configFile = dataFolder.resolve("config.yml").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("# BetterPack Configuration\n");
            writer.write("transfer-ip: 127.0.0.1\n");
            writer.write("transfer-port: 19132\n");
            writer.write("default-locale: en_US\n");
            writer.write("fix-threshold: 80\n");
            LoggerUtil.info("Default configuration created.");
        } catch (Exception e) {
            LoggerUtil.error("Failed to save default config.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public BetterPackConfig getConfig() {
        return config;
    }
}
