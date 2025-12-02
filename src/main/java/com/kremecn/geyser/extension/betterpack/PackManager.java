package com.kremecn.geyser.extension.betterpack;

import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.ResourcePackManifest;
import com.kremecn.geyser.extension.betterpack.util.LanguageManager;
import com.kremecn.geyser.extension.betterpack.util.LoggerUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PackManager {

    private final Map<String, ResourcePack> packs = new HashMap<>();
    private final Map<String, ResourcePackManifest> packManifests = new HashMap<>();
    private final Map<String, String> packFilenames = new HashMap<>();
    private final Path packsPath;

    public PackManager(Path dataFolder) {
        this.packsPath = dataFolder.resolve("packs");
        loadPacks();
    }

    public void loadPacks() {
        packs.clear();
        packManifests.clear();
        packFilenames.clear();

        if (!packsPath.toFile().exists()) {
            packsPath.toFile().mkdirs();
            return; // No packs to load
        }

        File[] files = packsPath.toFile().listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isFile() && (file.getName().endsWith(".zip") || file.getName().endsWith(".mcpack"))) {
                try {
                    ResourcePack pack = ResourcePack.create(PackCodec.path(file.toPath()));
                    String uuid = pack.manifest().header().uuid().toString();
                    packs.put(uuid, pack);
                    packManifests.put(uuid, pack.manifest());
                    packFilenames.put(uuid, file.getName());
                    String locale = "en_US";
                    try {
                        if (BetterPack.getInstance() != null) {
                            locale = BetterPack.getInstance().getConfigManager().getConfig().getDefaultLocale();
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                    String msg = LanguageManager.get(locale, "pack.loaded");
                    LoggerUtil.info(String.format(msg, pack.manifest().header().name(), uuid));
                } catch (Exception e) {
                    LoggerUtil.error("Failed to load pack: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    public ResourcePack getPack(String uuid) {
        return packs.get(uuid);
    }

    public ResourcePackManifest getManifest(String uuid) {
        return packManifests.get(uuid);
    }

    public String getPackFilename(String uuid) {
        return packFilenames.get(uuid);
    }

    public Map<String, ResourcePackManifest> getAvailablePacks() {
        return new HashMap<>(packManifests);
    }
}
