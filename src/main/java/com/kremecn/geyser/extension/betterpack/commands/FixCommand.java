package com.kremecn.geyser.extension.betterpack.commands;

import com.kremecn.geyser.extension.betterpack.BetterPack;
import com.kremecn.geyser.extension.betterpack.util.LanguageManager;
import com.kremecn.geyser.extension.betterpack.util.PathFixer;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FixCommand extends BaseCommand {

    public FixCommand(BetterPack extension) {
        super(extension);
    }

    @Override
    public String name() {
        return "fix";
    }

    @Override
    public String description() {
        return "Auto-fix long paths in a resource pack";
    }

    @Override
    public String permission() {
        return "betterpack.admin";
    }

    @Override
    public void execute(GeyserConnection connection, String[] args) {
        if (args.length < 1) {
            connection.sendMessage(LanguageManager.get(connection.locale(), "command.fix.usage"));
            return;
        }

        // Parse arguments
        boolean fastMode = false;
        List<String> cleanArgs = new ArrayList<>();
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-fast")) {
                fastMode = true;
            } else {
                cleanArgs.add(arg);
            }
        }

        if (cleanArgs.isEmpty()) {
            connection.sendMessage(LanguageManager.get(connection.locale(), "command.fix.usage"));
            return;
        }

        int threshold = extension.getConfigManager().getConfig().getFixThreshold();
        String packName;

        // Check if the last argument is a number (threshold)
        String lastArg = cleanArgs.get(cleanArgs.size() - 1);
        boolean lastIsNumber = false;
        try {
            threshold = Integer.parseInt(lastArg);
            lastIsNumber = true;
        } catch (NumberFormatException e) {
            // Last argument is part of the name
        }

        if (lastIsNumber && cleanArgs.size() > 1) {
            // Join all args except the last one
            packName = String.join(" ", cleanArgs.subList(0, cleanArgs.size() - 1)).toLowerCase();
        } else {
            // Join all args
            packName = String.join(" ", cleanArgs).toLowerCase();
            if (!lastIsNumber)
                threshold = extension.getConfigManager().getConfig().getFixThreshold();
        }

        File packsDir = extension.dataFolder().resolve("packs").toFile();
        File[] files = packsDir.listFiles();

        final int finalThreshold = threshold;
        final boolean finalFastMode = fastMode;
        final String finalPackName = packName;

        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase().contains(finalPackName)) {
                    connection.sendMessage(String.format(LanguageManager.get(connection.locale(), "command.fix.found"),
                            file.getName()));

                    // Check if loaded
                    boolean isLoaded = false;
                    try {
                        ResourcePack pack = ResourcePack.create(PackCodec.path(file.toPath()));
                        String uuid = pack.manifest().header().uuid().toString();
                        if (extension.getPackManager().getAvailablePacks().containsKey(uuid)) {
                            isLoaded = true;
                        }
                    } catch (Exception e) {
                        // Failed to read manifest
                    }

                    if (!isLoaded) {
                        connection.sendMessage(
                                LanguageManager.get(connection.locale(), "command.fix.warning.not_loaded"));
                    } else {
                        connection.sendMessage(LanguageManager.get(connection.locale(), "command.fix.loaded"));
                    }

                    connection.sendMessage(String.format(LanguageManager.get(connection.locale(), "command.fix.start"),
                            finalThreshold, finalFastMode));
                    connection.sendMessage(LanguageManager.get(connection.locale(), "command.fix.wait"));

                    // Run async
                    new Thread(() -> {
                        try {
                            boolean fixed = new PathFixer(
                                    file.toPath(), finalThreshold, finalFastMode,
                                    (key, msgArgs) -> {
                                        String msg = LanguageManager.get(connection.locale(), key);
                                        if (msgArgs != null && msgArgs.length > 0) {
                                            try {
                                                msg = String.format(msg, msgArgs);
                                            } catch (Exception e) {
                                                // Ignore
                                            }
                                        }
                                        connection.sendMessage(msg);
                                    }).fix();

                            if (fixed) {
                                connection.sendMessage(LanguageManager.get(connection.locale(), "command.fix.success"));
                                extension.getPackManager().loadPacks();
                                connection.sendMessage(
                                        LanguageManager.get(connection.locale(), "command.fix.reloaded"));
                            } else {
                                connection.sendMessage(String.format(
                                        LanguageManager.get(connection.locale(), "command.fix.no_changes"),
                                        finalThreshold));
                            }
                        } catch (Exception e) {
                            connection.sendMessage(String.format(
                                    LanguageManager.get(connection.locale(), "command.fix.error"), e.getMessage()));
                            e.printStackTrace();
                        }
                    }).start();

                    return;
                }
            }
        }
        connection.sendMessage(
                String.format(LanguageManager.get(connection.locale(), "command.fix.not_found"), finalPackName));
    }
}
