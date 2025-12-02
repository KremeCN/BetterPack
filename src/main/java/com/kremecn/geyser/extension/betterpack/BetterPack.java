package com.kremecn.geyser.extension.betterpack;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.pack.ResourcePack;
import com.kremecn.geyser.extension.betterpack.util.LanguageManager;
import com.kremecn.geyser.extension.betterpack.util.LoggerUtil;
import com.kremecn.geyser.extension.betterpack.config.ConfigManager;

import com.kremecn.geyser.extension.betterpack.commands.*;
import java.util.List;
import java.util.Arrays;

public class BetterPack implements Extension {

    private static BetterPack instance;

    private PackManager packManager;
    private UserPackData userPackData;
    private FormHandler formHandler;
    private AdminFormHandler adminFormHandler;
    private ConfigManager configManager;

    public static BetterPack getInstance() {
        return instance;
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        instance = this;
        // Startup Banner
        LoggerUtil.info("\n" +
                " ____       _   _            ____            _    \n" +
                "| __ )  ___| |_| |_ ___ _ __|  _ \\ __ _  ___| | __\n" +
                "|  _ \\ / _ \\ __| __/ _ \\ '__| |_) / _` |/ __| |/ /\n" +
                "| |_) |  __/ |_| ||  __/ |  |  __/ (_| | (__|   < \n" +
                "|____/ \\___|\\__|\\__\\___|_|  |_|   \\__,_|\\___|_|\\_\\\n" +
                "                                                  \n" +
                " BetterPack v1.0.0 by KremeCN\n");

        LanguageManager.init();
        this.configManager = new ConfigManager(this.dataFolder());
        this.configManager.loadConfig();

        this.packManager = new PackManager(this.dataFolder());
        this.userPackData = new UserPackData(this.dataFolder(), packManager);
        this.formHandler = new FormHandler(packManager, userPackData, configManager.getConfig());
        this.adminFormHandler = new AdminFormHandler(packManager, userPackData);
        LoggerUtil.info("BetterPack initialized successfully!");
    }

    @Subscribe
    public void onSessionLoadResourcePacks(SessionLoadResourcePacksEvent event) {
        try {
            String xuid = event.connection().xuid();
            List<ResourcePack> packs = userPackData.getPacksForUser(xuid);
            for (ResourcePack pack : packs) {
                event.register(pack);
            }
        } catch (Exception e) {
            LoggerUtil.error("Failed to load packs for user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onDefineCommands(GeyserDefineCommandsEvent event) {
        List<BaseCommand> commands = Arrays.asList(
                new MenuCommand(this),
                new AdminCommand(this),
                new FixCommand(this),
                new ReloadCommand(this),
                new PushCommand(this),
                new PushAllCommand(this));

        for (BaseCommand cmd : commands) {
            event.register(Command.builder(this)
                    .name(cmd.name())
                    .aliases(cmd.aliases())
                    .description(cmd.description())
                    .permission(cmd.permission())
                    .bedrockOnly(true)
                    .playerOnly(true)
                    .source(GeyserConnection.class)
                    .executor((source, command, args) -> {
                        if (source instanceof GeyserConnection) {
                            cmd.execute((GeyserConnection) source, args);
                        }
                    })
                    .build());
        }
    }

    public PackManager getPackManager() {
        return packManager;
    }

    public UserPackData getUserPackData() {
        return userPackData;
    }

    public FormHandler getFormHandler() {
        return formHandler;
    }

    public AdminFormHandler getAdminFormHandler() {
        return adminFormHandler;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
