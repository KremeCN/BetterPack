package com.kremecn.geyser.extension.betterpack.commands;

import com.kremecn.geyser.extension.betterpack.BetterPack;
import com.kremecn.geyser.extension.betterpack.util.LanguageManager;
import org.geysermc.geyser.api.connection.GeyserConnection;

public class PushAllCommand extends BaseCommand {

    public PushAllCommand(BetterPack extension) {
        super(extension);
    }

    @Override
    public String name() {
        return "pushall";
    }

    @Override
    public String description() {
        return "Push default packs to ALL players";
    }

    @Override
    public String permission() {
        return "betterpack.admin";
    }

    @Override
    public void execute(GeyserConnection connection, String[] args) {
        extension.getUserPackData().pushToAll();
        connection.sendMessage(LanguageManager.get(connection.locale(), "command.pushall.success"));
    }
}
