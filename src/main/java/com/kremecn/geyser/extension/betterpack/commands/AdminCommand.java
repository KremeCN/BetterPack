package com.kremecn.geyser.extension.betterpack.commands;

import com.kremecn.geyser.extension.betterpack.BetterPack;
import org.geysermc.geyser.api.connection.GeyserConnection;

public class AdminCommand extends BaseCommand {

    public AdminCommand(BetterPack extension) {
        super(extension);
    }

    @Override
    public String name() {
        return "admin";
    }

    @Override
    public String description() {
        return "BetterPack Admin Panel";
    }

    @Override
    public String permission() {
        return "betterpack.admin";
    }

    @Override
    public void execute(GeyserConnection connection, String[] args) {
        extension.getAdminFormHandler().sendAdminMenu(connection);
    }
}
