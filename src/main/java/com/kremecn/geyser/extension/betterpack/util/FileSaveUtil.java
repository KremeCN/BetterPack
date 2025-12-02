package com.kremecn.geyser.extension.betterpack.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileSaveUtil {

    public static void makeDir(Path path, String debugName) {
        if (!path.toFile().exists()) {
            if (path.toFile().mkdirs()) {
                System.out.println("Created " + debugName);
            } else {
                System.err.println("Failed to create " + debugName);
            }
        }
    }

    public static void save(List<String> data, Path path) {
        try {
            Files.write(path, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> load(Path path) {
        if (!path.toFile().exists()) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
