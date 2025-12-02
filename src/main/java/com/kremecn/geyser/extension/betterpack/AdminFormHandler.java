package com.kremecn.geyser.extension.betterpack;

import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.pack.ResourcePackManifest;
import com.kremecn.geyser.extension.betterpack.util.LanguageManager;

import java.util.List;
import java.util.Map;

public class AdminFormHandler {

    private final PackManager packManager;
    private final UserPackData userPackData;

    public AdminFormHandler(PackManager packManager, UserPackData userPackData) {
        this.packManager = packManager;
        this.userPackData = userPackData;
    }

    public void sendAdminMenu(GeyserConnection connection) {
        String locale = connection.locale();
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(LanguageManager.get(locale, "admin.title"))
                .content(LanguageManager.get(locale, "admin.content"));

        builder.button(LanguageManager.get(locale, "admin.edit_defaults"));
        builder.button(LanguageManager.get(locale, "admin.push_player"));
        builder.button(LanguageManager.get(locale, "admin.push_all"));
        builder.button(LanguageManager.get(locale, "menu.back"));

        builder.validResultHandler((form, response) -> {
            switch (response.clickedButtonId()) {
                case 0 -> sendEditDefaultsMenu(connection);
                case 1 -> sendPushToPlayerMenu(connection);
                case 2 -> sendPushConfirmMenu(connection);
                case 3 -> {
                    // Close
                }
            }
        });

        connection.sendForm(builder.build());
    }

    public void sendPushToPlayerMenu(GeyserConnection connection) {
        String locale = connection.locale();
        List<GeyserConnection> players = new java.util.ArrayList<>(
                org.geysermc.geyser.api.GeyserApi.api().onlineConnections());

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(LanguageManager.get(locale, "admin.push_player.title"))
                .content(LanguageManager.get(locale, "admin.push_player.content"));

        for (GeyserConnection player : players) {
            builder.button(player.name());
        }
        builder.button(LanguageManager.get(locale, "menu.back"));

        builder.validResultHandler((form, response) -> {
            int clickedId = response.clickedButtonId();
            if (clickedId < players.size()) {
                GeyserConnection target = players.get(clickedId);
                sendPushToPlayerConfirmMenu(connection, target);
            } else {
                sendAdminMenu(connection);
            }
        });

        connection.sendForm(builder.build());
    }

    public void sendPushToPlayerConfirmMenu(GeyserConnection connection, GeyserConnection target) {
        String locale = connection.locale();
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(LanguageManager.get(locale, "admin.push_player.confirm.title"))
                .content(
                        String.format(LanguageManager.get(locale, "admin.push_player.confirm.content"), target.name()));

        builder.button(LanguageManager.get(locale, "admin.push_player.confirm.yes"));
        builder.button(LanguageManager.get(locale, "admin.push_player.confirm.no"));

        builder.validResultHandler((form, response) -> {
            switch (response.clickedButtonId()) {
                case 0 -> {
                    userPackData.resetUser(target.xuid());
                    String successMsg = String.format(LanguageManager.get(locale, "admin.push_player.success"),
                            target.name());
                    // connection.sendMessage("§a" + successMsg); // Optional
                    sendAdminMenu(connection);
                }
                case 1 -> sendPushToPlayerMenu(connection);
            }
        });

        connection.sendForm(builder.build());
    }

    public void sendPushConfirmMenu(GeyserConnection connection) {
        String locale = connection.locale();
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(LanguageManager.get(locale, "admin.push.confirm.title"))
                .content(LanguageManager.get(locale, "admin.push.confirm.content"));

        builder.button(LanguageManager.get(locale, "admin.push.confirm.yes"));
        builder.button(LanguageManager.get(locale, "admin.push.confirm.no"));

        builder.validResultHandler((form, response) -> {
            switch (response.clickedButtonId()) {
                case 0 -> {
                    userPackData.pushToAll();
                    sendAdminMenu(connection);
                }
                case 1 -> sendAdminMenu(connection);
            }
        });

        connection.sendForm(builder.build());
    }

    public void sendEditDefaultsMenu(GeyserConnection connection) {
        String locale = connection.locale();
        List<String> defaultPacks = userPackData.getDefaultPacks();

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(LanguageManager.get(locale, "admin.defaults.title"))
                .content(LanguageManager.get(locale, "admin.defaults.content"));

        builder.button(LanguageManager.get(locale, "admin.defaults.add"));
        for (String packId : defaultPacks) {
            ResourcePackManifest manifest = packManager.getManifest(packId);
            String name = (manifest != null) ? manifest.header().name()
                    : LanguageManager.get(locale, "pack.unknown") + " (" + packId + ")";
            builder.button(name);
        }
        builder.button(LanguageManager.get(locale, "menu.back"));

        builder.validResultHandler((form, response) -> {
            int clickedId = response.clickedButtonId();
            if (clickedId == 0) {
                sendAddDefaultPackMenu(connection);
            } else if (clickedId <= defaultPacks.size()) {
                sendDefaultPackActionMenu(connection, defaultPacks.get(clickedId - 1));
            } else {
                sendAdminMenu(connection);
            }
        });

        connection.sendForm(builder.build());
    }

    public void sendAddDefaultPackMenu(GeyserConnection connection) {
        String locale = connection.locale();
        List<String> defaultPacks = userPackData.getDefaultPacks();
        Map<String, ResourcePackManifest> availablePacks = packManager.getAvailablePacks();
        List<String> availablePackIds = new java.util.ArrayList<>();

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(LanguageManager.get(locale, "admin.defaults.add.title"))
                .content(LanguageManager.get(locale, "admin.defaults.add.content"));

        for (Map.Entry<String, ResourcePackManifest> entry : availablePacks.entrySet()) {
            if (!defaultPacks.contains(entry.getKey())) {
                builder.button(entry.getValue().header().name());
                availablePackIds.add(entry.getKey());
            }
        }
        builder.button(LanguageManager.get(locale, "menu.back"));

        builder.validResultHandler((form, response) -> {
            int clickedId = response.clickedButtonId();
            if (clickedId < availablePackIds.size()) {
                defaultPacks.add(availablePackIds.get(clickedId));
                userPackData.setDefaultPacks(defaultPacks);
                sendEditDefaultsMenu(connection);
            } else {
                sendEditDefaultsMenu(connection);
            }
        });

        connection.sendForm(builder.build());
    }

    public void sendDefaultPackActionMenu(GeyserConnection connection, String packId) {
        String locale = connection.locale();
        List<String> defaultPacks = userPackData.getDefaultPacks();
        ResourcePackManifest manifest = packManager.getManifest(packId);
        String name = (manifest != null) ? manifest.header().name() : LanguageManager.get(locale, "pack.unknown");

        SimpleForm.Builder builder = SimpleForm.builder()
                .title(name)
                .content(LanguageManager.get(locale, "admin.defaults.action.content"));

        builder.button(LanguageManager.get(locale, "pack.move_up"));
        builder.button(LanguageManager.get(locale, "pack.move_down"));
        builder.button(LanguageManager.get(locale, "pack.remove"));
        builder.button(LanguageManager.get(locale, "menu.back"));

        builder.validResultHandler((form, response) -> {
            int index = defaultPacks.indexOf(packId);
            switch (response.clickedButtonId()) {
                case 0 -> {
                    if (index > 0) {
                        defaultPacks.remove(index);
                        defaultPacks.add(index - 1, packId);
                        userPackData.setDefaultPacks(defaultPacks);
                    }
                    sendEditDefaultsMenu(connection);
                }
                case 1 -> {
                    if (index != -1 && index < defaultPacks.size() - 1) {
                        defaultPacks.remove(index);
                        defaultPacks.add(index + 1, packId);
                        userPackData.setDefaultPacks(defaultPacks);
                    }
                    sendEditDefaultsMenu(connection);
                }
                case 2 -> {
                    defaultPacks.remove(packId);
                    userPackData.setDefaultPacks(defaultPacks);
                    sendEditDefaultsMenu(connection);
                }
                case 3 -> sendEditDefaultsMenu(connection);
            }
        });

        connection.sendForm(builder.build());
    }
}
