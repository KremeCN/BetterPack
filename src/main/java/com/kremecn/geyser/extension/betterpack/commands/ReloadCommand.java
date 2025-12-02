package com.kremecn.geyser.extension.betterpack.commands;

import com.kremecn.geyser.extension.betterpack.BetterPack;
import com.kremecn.geyser.extension.betterpack.util.LanguageManager;
import org.geysermc.geyser.api.connection.GeyserConnection;

public class ReloadCommand extends BaseCommand {

    public ReloadCommand(BetterPack extension) {
        super(extension);
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String description() {
        return "Reload resource packs from disk";
    }

    @Override
    public String permission() {
        return "betterpack.admin";
    }

    @Override
    public void execute(GeyserConnection connection, String[] args) {
        extension.getPackManager().loadPacks();
        connection.sendMessage(LanguageManager.get(connection.locale(), "command.reload.success"));
    }
}
