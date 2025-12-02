package com.kremecn.geyser.extension.betterpack.commands;

import com.kremecn.geyser.extension.betterpack.BetterPack;
import com.kremecn.geyser.extension.betterpack.util.LanguageManager;
import org.geysermc.geyser.api.connection.GeyserConnection;

public class PushCommand extends BaseCommand {

    public PushCommand(BetterPack extension) {
        super(extension);
    }

    @Override
    public String name() {
        return "push";
    }

    @Override
    public String description() {
        return "Push default packs to a specific player";
    }

    @Override
    public String permission() {
        return "betterpack.admin";
    }

    @Override
    public void execute(GeyserConnection connection, String[] args) {
        if (args.length < 1) {
            connection.sendMessage(LanguageManager.get(connection.locale(), "command.push.usage"));
            return;
        }
        String playerName = args[0];
        GeyserConnection target = null;
        for (GeyserConnection session : org.geysermc.geyser.api.GeyserApi.api().onlineConnections()) {
            if (session.name().equalsIgnoreCase(playerName)) {
                target = session;
                break;
            }
        }

        if (target != null) {
            extension.getUserPackData().resetUser(target.xuid());
            connection.sendMessage(String.format(LanguageManager.get(connection.locale(), "command.push.success"),
                    target.name()));
        } else {
            connection.sendMessage(String.format(LanguageManager.get(connection.locale(), "command.push.not_found"),
                    playerName));
        }
    }
}
