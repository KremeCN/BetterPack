package com.kremecn.geyser.extension.betterpack.util;

import org.geysermc.geyser.api.extension.ExtensionLogger;

public class LoggerUtil {

    private static final String PREFIX = "\u001B[34m[BetterPack]\u001B[0m ";

    public static void info(String message) {
        System.out.println(PREFIX + message);
    }

    public static void warn(String message) {
        System.out.println(PREFIX + "\u001B[33mWARN: " + message + "\u001B[0m");
    }

    public static void error(String message) {
        System.err.println(PREFIX + "\u001B[31mERROR: " + message + "\u001B[0m");
    }

    public static void log(ExtensionLogger logger, String message) {
        // If we want to use the extension logger but with our prefix,
        // we might get double prefixes like [pack] [BetterPack].
        // The user wants to replace [pack] with [BetterPack].
        // Since we can't easily change the [pack] prefix (it's the extension ID),
        // we will stick to System.out for the "Blue [BetterPack]" look the user
        // requested.
        info(message);
    }
}
