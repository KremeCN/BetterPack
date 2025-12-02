package com.kremecn.geyser.extension.betterpack.commands;

import org.geysermc.geyser.api.connection.GeyserConnection;
import com.kremecn.geyser.extension.betterpack.BetterPack;

import java.util.Collections;
import java.util.List;

public abstract class BaseCommand {
    protected final BetterPack extension;

    public BaseCommand(BetterPack extension) {
        this.extension = extension;
    }

    public abstract String name();

    public abstract String description();

    public abstract String permission();

    public List<String> aliases() {
        return Collections.emptyList();
    }

    public abstract void execute(GeyserConnection connection, String[] args);
}
