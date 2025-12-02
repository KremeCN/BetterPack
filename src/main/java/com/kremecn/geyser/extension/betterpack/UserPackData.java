package com.kremecn.geyser.extension.betterpack;

import com.kremecn.geyser.extension.betterpack.util.FileSaveUtil;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserPackData {

    private final Path storagePath;
    private final Map<String, List<String>> userPacks = new ConcurrentHashMap<>();
    private final PackManager packManager;

    public UserPackData(Path dataFolder, PackManager packManager) {
        this.storagePath = dataFolder.resolve("users");
        this.packManager = packManager;
        this.defaultsPath = storagePath.getParent().resolve("defaults.txt");
        this.versionPath = storagePath.getParent().resolve("version.txt");
        FileSaveUtil.makeDir(storagePath, "user storage");
        loadDefaults();
    }

    private List<String> defaultPacks = new ArrayList<>();
    private int configVersion = 0;
    private final Path defaultsPath;
    private final Path versionPath;

    private void loadDefaults() {
        defaultPacks = FileSaveUtil.load(defaultsPath);
        List<String> versionLines = FileSaveUtil.load(versionPath);
        if (!versionLines.isEmpty()) {
            try {
                configVersion = Integer.parseInt(versionLines.get(0));
            } catch (NumberFormatException e) {
                configVersion = 0;
            }
        }
    }

    public void saveDefaults() {
        FileSaveUtil.save(defaultPacks, defaultsPath);
    }

    public void saveVersion() {
        List<String> lines = new ArrayList<>();
        lines.add(String.valueOf(configVersion));
        FileSaveUtil.save(lines, versionPath);
    }

    public void pushToAll() {
        configVersion++;
        saveVersion();
    }

    public List<String> getDefaultPacks() {
        return defaultPacks;
    }

    public void setDefaultPacks(List<String> packs) {
        this.defaultPacks = packs;
        saveDefaults();
    }

    public void loadUser(String xuid) {
        Path userFile = storagePath.resolve(xuid + ".txt");
        List<String> lines = FileSaveUtil.load(userFile);

        int userVersion = -1;
        List<String> packs = new ArrayList<>();

        if (!lines.isEmpty()) {
            // Check if first line is version (starts with v)
            if (lines.get(0).startsWith("v")) {
                try {
                    userVersion = Integer.parseInt(lines.get(0).substring(1));
                    packs.addAll(lines.subList(1, lines.size()));
                } catch (NumberFormatException e) {
                    // Legacy format or error
                    packs.addAll(lines);
                }
            } else {
                packs.addAll(lines);
            }
        }

        if (userVersion < configVersion) {
            // Reset to defaults
            userPacks.put(xuid, new ArrayList<>(defaultPacks));
            saveUser(xuid);
        } else {
            userPacks.put(xuid, packs);
        }
    }

    public void saveUser(String xuid) {
        if (userPacks.containsKey(xuid)) {
            Path userFile = storagePath.resolve(xuid + ".txt");
            List<String> data = new ArrayList<>();
            data.add("v" + configVersion);
            data.addAll(userPacks.get(xuid));
            FileSaveUtil.save(data, userFile);
        }
    }

    public void resetUser(String xuid) {
        userPacks.remove(xuid);
        Path userFile = storagePath.resolve(xuid + ".txt");
        try {
            java.nio.file.Files.deleteIfExists(userFile);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public List<ResourcePack> getPacksForUser(String xuid) {
        if (!userPacks.containsKey(xuid)) {
            loadUser(xuid);
        }
        List<String> packIds = userPacks.getOrDefault(xuid, new ArrayList<>());
        List<ResourcePack> packs = new ArrayList<>();
        for (String id : packIds) {
            ResourcePack pack = packManager.getPack(id);
            if (pack != null) {
                packs.add(pack);
            }
        }
        return packs;
    }

    public List<String> getPackIdsForUser(String xuid) {
        return new ArrayList<>(userPacks.getOrDefault(xuid, new ArrayList<>()));
    }

    public void setPacksForUser(String xuid, List<String> packs) {
        userPacks.put(xuid, packs);
        saveUser(xuid);
    }

    public void addPack(String xuid, String packId) {
        List<String> packs = getPackIdsForUser(xuid);
        if (!packs.contains(packId)) {
            packs.add(packId); // Add to end (lowest priority usually, or highest depending on Geyser
                               // implementation. Geyser applies in order, so last one overrides previous ones
                               // if they conflict? No, usually list order matters. We'll assume order in list
                               // = order of application)
            setPacksForUser(xuid, packs);
        }
    }

    public void removePack(String xuid, String packId) {
        List<String> packs = getPackIdsForUser(xuid);
        if (packs.remove(packId)) {
            setPacksForUser(xuid, packs);
        }
    }

    public void movePackUp(String xuid, String packId) {
        List<String> packs = getPackIdsForUser(xuid);
        int index = packs.indexOf(packId);
        if (index > 0) {
            packs.remove(index);
            packs.add(index - 1, packId);
            setPacksForUser(xuid, packs);
        }
    }

    public void movePackDown(String xuid, String packId) {
        List<String> packs = getPackIdsForUser(xuid);
        int index = packs.indexOf(packId);
        if (index != -1 && index < packs.size() - 1) {
            packs.remove(index);
            packs.add(index + 1, packId);
            setPacksForUser(xuid, packs);
        }
    }
}
