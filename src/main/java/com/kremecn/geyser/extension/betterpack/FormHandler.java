package com.kremecn.geyser.extension.betterpack;

import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.pack.ResourcePackManifest;
import org.geysermc.geyser.session.GeyserSession;
import com.kremecn.geyser.extension.betterpack.util.LanguageManager;
import com.kremecn.geyser.extension.betterpack.config.BetterPackConfig;

import java.util.List;
import java.util.Map;

public class FormHandler {

    private final PackManager packManager;
    private final UserPackData userPackData;
    private final BetterPackConfig config;

    public FormHandler(PackManager packManager, UserPackData userPackData, BetterPackConfig config) {
        this.packManager = packManager;
        this.userPackData = userPackData;
        this.config = config;
    }

    public void sendMainMenu(GeyserConnection connection) {
        String locale = connection.locale();
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(LanguageManager.get(locale, "menu.title"))
                .content(LanguageManager.get(locale, "menu.content"));

        builder.button(LanguageManager.get(locale, "menu.my_packs"));
        builder.button(LanguageManager.get(locale, "menu.available_packs"));
        builder.button(LanguageManager.get(locale, "menu.apply"));

        builder.validResultHandler((form, response) -> {
            switch (response.clickedButtonId()) {
                case 0 -> sendMyPacksMenu(connection);
                case 1 -> sendAvailablePacksMenu(connection);
                case 2 -> {
                    // Re-transfer to apply changes
                    connection.transfer(config.getTransferIp(), config.getTransferPort());
                }
            }
        });

        connection.sendForm(builder.build());
    }

    public void sendMyPacksMenu(GeyserConnection connection) {
        String locale = connection.locale();
        String xuid = connection.xuid();
        List<String> userPacks = userPackData.getPackIdsForUser(xuid);

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(LanguageManager.get(locale, "my_packs.title"))
                .content(LanguageManager.get(locale, "my_packs.content"));

        if (userPacks.isEmpty()) {
            builder.content(LanguageManager.get(locale, "my_packs.empty"));
        }

        for (String packId : userPacks) {
            ResourcePackManifest manifest = packManager.getManifest(packId);
            String name;
            if (manifest != null) {
                name = manifest.header().name();
                if ("pack.name".equals(name)) {
                    name = packManager.getPackFilename(packId);
                }
            } else {
                name = LanguageManager.get(locale, "pack.unknown") + " (" + packId + ")";
            }
            builder.button(name);
        }

        builder.button(LanguageManager.get(locale, "menu.back"));

        builder.validResultHandler((form, response) -> {
            int clickedId = response.clickedButtonId();
            if (clickedId < userPacks.size()) {
                sendPackActionMenu(connection, userPacks.get(clickedId));
            } else {
                sendMainMenu(connection);
            }
        });

        connection.sendForm(builder.build());
    }

    public void sendAvailablePacksMenu(GeyserConnection connection) {
        String locale = connection.locale();
        String xuid = connection.xuid();
        List<String> userPacks = userPackData.getPackIdsForUser(xuid);
        Map<String, ResourcePackManifest> availablePacks = packManager.getAvailablePacks();
        List<String> availablePackIds = new java.util.ArrayList<>();

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(LanguageManager.get(locale, "available_packs.title"))
                .content(LanguageManager.get(locale, "available_packs.content"));

        for (Map.Entry<String, ResourcePackManifest> entry : availablePacks.entrySet()) {
            if (!userPacks.contains(entry.getKey())) {
                String name = entry.getValue().header().name();
                if ("pack.name".equals(name)) {
                    name = packManager.getPackFilename(entry.getKey());
                }
                builder.button(name);
                availablePackIds.add(entry.getKey());
            }
        }

        builder.button(LanguageManager.get(locale, "menu.back"));

        builder.validResultHandler((form, response) -> {
            int clickedId = response.clickedButtonId();
            if (clickedId < availablePackIds.size()) {
                userPackData.addPack(xuid, availablePackIds.get(clickedId));
                sendAvailablePacksMenu(connection); // Refresh
            } else {
                sendMainMenu(connection);
            }
        });

        connection.sendForm(builder.build());
    }

    public void sendPackActionMenu(GeyserConnection connection, String packId) {
        String locale = connection.locale();
        ResourcePackManifest manifest = packManager.getManifest(packId);
        String name = (manifest != null) ? manifest.header().name() : LanguageManager.get(locale, "pack.unknown");
        if ("pack.name".equals(name) && manifest != null) {
            name = packManager.getPackFilename(packId);
        }

        String description = (manifest != null) ? manifest.header().description() : "";
        if (description == null || description.isEmpty() || "pack.description".equals(description)) {
            description = LanguageManager.get(locale, "pack.no_description");
        }

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(name)
                .content("§7" + LanguageManager.get(locale, "pack.description_label") + "§r\n" +
                        description + "\n\n" +
                        LanguageManager.get(locale, "pack.action_content"));

        builder.button(LanguageManager.get(locale, "pack.move_up"));
        builder.button(LanguageManager.get(locale, "pack.move_down"));
        builder.button(LanguageManager.get(locale, "pack.remove"));
        builder.button(LanguageManager.get(locale, "menu.back"));

        builder.validResultHandler((form, response) -> {
            switch (response.clickedButtonId()) {
                case 0 -> {
                    userPackData.movePackUp(connection.xuid(), packId);
                    sendMyPacksMenu(connection);
                }
                case 1 -> {
                    userPackData.movePackDown(connection.xuid(), packId);
                    sendMyPacksMenu(connection);
                }
                case 2 -> {
                    userPackData.removePack(connection.xuid(), packId);
                    sendMyPacksMenu(connection);
                }
                case 3 -> sendMyPacksMenu(connection);
            }
        });

        connection.sendForm(builder.build());
    }
}
