package com.kremecn.geyser.extension.betterpack.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PathFixerTest {

    @TempDir
    Path tempDir;

    @Test
    void testZipSlipProtection() throws IOException {
        Path maliciousZip = tempDir.resolve("malicious.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(maliciousZip))) {
            ZipEntry entry = new ZipEntry("../evil.txt");
            zos.putNextEntry(entry);
            zos.write("evil content".getBytes());
            zos.closeEntry();
        }

        PathFixer fixer = new PathFixer(maliciousZip, 80, true);

        // The fix method catches exceptions and logs them, returning false
        assertFalse(fixer.fix(), "Fix should fail for malicious zip");
    }

    @Test
    void testPathShortening() throws IOException {
        Path packZip = tempDir.resolve("test_pack.zip");
        String longPath = "assets/minecraft/textures/very/long/path/that/exceeds/the/limit/of/eighty/characters/texture.png";

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(packZip))) {
            // Add manifest
            ZipEntry manifest = new ZipEntry("manifest.json");
            zos.putNextEntry(manifest);
            zos.write("{}".getBytes());
            zos.closeEntry();

            // Add long path file
            ZipEntry entry = new ZipEntry(longPath);
            zos.putNextEntry(entry);
            zos.write("image data".getBytes());
            zos.closeEntry();
        }

        PathFixer fixer = new PathFixer(packZip, 50, true); // Low threshold to force move
        assertTrue(fixer.fix(), "Fix should succeed");

        Path fixedPack = tempDir.resolve("test_pack_fixed.mcpack");
        assertTrue(Files.exists(fixedPack), "Fixed pack should exist");
    }
}
