package com.kremecn.geyser.extension.betterpack.commands;

import com.kremecn.geyser.extension.betterpack.BetterPack;
import org.geysermc.geyser.api.connection.GeyserConnection;
import java.util.List;

public class MenuCommand extends BaseCommand {

    public MenuCommand(BetterPack extension) {
        super(extension);
    }

    @Override
    public String name() {
        return "menu";
    }

    @Override
    public String description() {
        return "Open the BetterPack menu";
    }

    @Override
    public String permission() {
        return "betterpack.menu";
    }

    @Override
    public List<String> aliases() {
        return List.of("m", "ui", "open");
    }

    @Override
    public void execute(GeyserConnection connection, String[] args) {
        extension.getFormHandler().sendMainMenu(connection);
    }
}
