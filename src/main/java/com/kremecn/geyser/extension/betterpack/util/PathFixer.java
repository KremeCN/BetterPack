package com.kremecn.geyser.extension.betterpack.util;

import com.kremecn.geyser.extension.betterpack.BetterPack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.core.json.JsonReadFeature;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * PathFixer (Semantic Fixer v2)
 * <p>
 * This utility fixes "File path too long" errors in Minecraft Bedrock resource
 * packs by:
 * 1. analyzing the pack structure (Reverse Indexing),
 * 2. identifying files with long paths (Planning),
 * 3. moving them to a safe, short directory (Migration),
 * 4. and updating all references in JSON and text files (Global Update).
 * <p>
 * It supports a "Fast Mode" for high-performance execution and a throttled mode
 * to prevent server lag when running on the main thread or background tasks.
 */
public class PathFixer {

    private static final ObjectMapper jsonMapper = new ObjectMapper()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature())
            .enable(JsonReadFeature.ALLOW_YAML_COMMENTS.mappedFeature())
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature());

    private static final String SAFE_SHELTER = "textures/_s/";
    private static final Set<String> TEXT_EXTENSIONS = Set.of(".json", ".lang", ".properties", ".material", ".fragment",
            ".vertex", ".fsh", ".vsh");
    private static final Set<String> MOVABLE_EXTENSIONS = Set.of(".png", ".tga", ".jpg", ".jpeg", ".ogg", ".fsb",
            ".wav");

    // Files that must NEVER be moved (Lowercase for case-insensitive check)
    private static final Set<String> IGNORED_FILES = Set.of(
            "manifest.json", "pack_icon.png", "sounds.json", "splashes.json",
            "biomes_client.json", "blocks.json", "items.json",
            "music_definitions.json", "sound_definitions.json",
            "terrain_texture.json", "item_texture.json", "flipbook_textures.json",
            "resources.json");

    // Directories that must NEVER be touched (System/Engine paths)
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            "ui", "texts", "font", "scripts", "fogs", "materials", "dialogue", "loot_tables", "trading", "structures");

    // Instance Fields
    private final Path packPath;
    private final int threshold;
    private final boolean fastMode;
    private final BiConsumer<String, Object[]> progressCallback;

    // Global Reverse Index: Normalized Path -> List of Usages
    private final Map<String, List<AssetUsage>> reverseIndex = new HashMap<>();

    // Files to move: Original Path -> New Path
    private final Map<String, String> movePlan = new HashMap<>();

    public PathFixer(Path packPath, int threshold, boolean fastMode) {
        this(packPath, threshold, fastMode, null);
    }

    public PathFixer(Path packPath, int threshold, boolean fastMode, BiConsumer<String, Object[]> progressCallback) {
        this.packPath = packPath;
        this.threshold = threshold;
        this.fastMode = fastMode;
        this.progressCallback = progressCallback;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java PathFixer <pack_path> [threshold] [-fast]");
            return;
        }

        Path packPath = Paths.get(args[0]);
        int threshold = 80;
        boolean fastMode = false;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-fast")) {
                fastMode = true;
            } else {
                try {
                    threshold = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        if (!Files.exists(packPath)) {
            System.out.println("File not found: " + packPath.toAbsolutePath());
            return;
        }

        new PathFixer(packPath, threshold, fastMode).fix();
    }

    // Static Convenience Methods
    public static boolean fixPack(Path packPath, int threshold) {
        return new PathFixer(packPath, threshold, false).fix();
    }

    public static boolean fixPack(Path packPath) {
        return fixPack(packPath, 80);
    }

    /**
     * Executes the fix process.
     *
     * @return true if the pack was successfully fixed and saved.
     */
    public boolean fix() {
        log("command.fix.progress.start", packPath.getFileName(), threshold, fastMode);
        reverseIndex.clear();
        movePlan.clear();

        Path tempDir = null;
        try {
            // 1. Unzip to temp
            tempDir = Files.createTempDirectory("betterpack_fix_");
            unzip(packPath, tempDir);

            // 2. Phase 0: Reverse Indexing
            log("command.fix.progress.phase0");
            buildReverseIndex(tempDir);

            // 3. Phase 1 & 2: Strategy & Migration
            log("command.fix.progress.phase1");
            planMigration(tempDir, threshold);

            if (movePlan.isEmpty()) {
                log("command.fix.progress.clean");
                deleteDirectory(tempDir);
                return false;
            }

            log("command.fix.progress.moving", movePlan.size());

            // 4. Execute Move
            executeMove(tempDir);

            // 5. Phase 3: Global Update
            log("command.fix.progress.phase3");
            updateReferences(tempDir);

            // 5b. Update Manifest (New UUIDs)
            log("command.fix.progress.manifest");
            updateManifest(tempDir);

            // 6. Repack
            String originalName = packPath.getFileName().toString();
            String newName = originalName.substring(0, originalName.lastIndexOf('.')) + "_fixed.mcpack";
            Path newPackPath = packPath.getParent().resolve(newName);

            zip(tempDir, newPackPath);

            log("command.fix.progress.saved", newPackPath.getFileName());
            return true;

        } catch (Exception e) {
            LoggerUtil.error("Failed to fix pack: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Throttles execution if not in fast mode.
     * Sleeps for 2ms to yield CPU to the server main thread.
     */
    private void throttle() {
        if (!fastMode) {
            try {
                Thread.sleep(2); // Sleep 2ms to yield CPU
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateManifest(Path root) {
        Path manifestPath = root.resolve("manifest.json");
        if (!Files.exists(manifestPath))
            return;

        try {
            JsonNode rootNode = jsonMapper.readTree(manifestPath.toFile());
            if (rootNode.isObject()) {
                ObjectNode rootObj = (ObjectNode) rootNode;

                // Update Header
                if (rootObj.has("header")) {
                    JsonNode header = rootObj.get("header");
                    if (header.isObject()) {
                        ObjectNode headerObj = (ObjectNode) header;
                        headerObj.put("uuid", UUID.randomUUID().toString());
                        if (headerObj.has("name")) {
                            String currentName = headerObj.get("name").asText();
                            if ("pack.name".equals(currentName)) {
                                String filename = this.packPath.getFileName().toString();
                                int dotIndex = filename.lastIndexOf('.');
                                if (dotIndex > 0) {
                                    filename = filename.substring(0, dotIndex);
                                }
                                headerObj.put("name", filename + " (Fixed)");
                            } else {
                                headerObj.put("name", currentName + " (Fixed)");
                            }
                        }
                    }
                }

                // Update Modules
                if (rootObj.has("modules")) {
                    JsonNode modules = rootObj.get("modules");
                    if (modules.isArray()) {
                        for (JsonNode module : modules) {
                            if (module.isObject()) {
                                ((ObjectNode) module).put("uuid", UUID.randomUUID().toString());
                            }
                        }
                    }
                }

                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), rootNode);
            }
        } catch (Exception e) {
            LoggerUtil.error("Failed to update manifest: " + e.getMessage());
        }
    }

    // ================= Phase 0: Reverse Indexing =================

    /**
     * Phase 0: Reverse Indexing
     * Scans all JSON files to find every file path reference.
     * Builds a map of [Normalized Path -> List of Files referencing it].
     */
    private void buildReverseIndex(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted() // Deterministic order
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(jsonFile -> {
                        scanJson(root, jsonFile);
                        throttle();
                    });
        }
    }

    private void scanJson(Path root, Path jsonFile) {
        try {
            JsonNode rootNode = jsonMapper.readTree(jsonFile.toFile());
            scanNode(root, jsonFile, rootNode);
        } catch (Exception e) {
            // LoggerUtil.warn("Failed to parse JSON: " + jsonFile + " (" + e.getMessage() +
            // ")");
        }
    }

    private void scanNode(Path root, Path currentFile, JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                // Check Value
                if (field.getValue().isTextual()) {
                    String value = field.getValue().asText();
                    if (isPotentialPath(value)) {
                        registerUsage(root, currentFile, value, UsageType.VALUE);
                    }
                } else {
                    scanNode(root, currentFile, field.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                if (element.isTextual()) {
                    String value = element.asText();
                    if (isPotentialPath(value)) {
                        registerUsage(root, currentFile, value, UsageType.VALUE);
                    }
                } else {
                    scanNode(root, currentFile, element);
                }
            }
        }
    }

    private boolean isPotentialPath(String value) {
        return value.contains("/") || value.endsWith(".png") || value.endsWith(".tga") || value.endsWith(".json");
    }

    private void registerUsage(Path root, Path currentFile, String rawValue, UsageType type) {
        String normalized = normalizePath(rawValue);
        reverseIndex.computeIfAbsent(normalized, k -> new ArrayList<>())
                .add(new AssetUsage(root.relativize(currentFile).toString().replace("\\", "/"), rawValue, type));
    }

    // ================= Phase 1 & 2: Planning =================

    /**
     * Phase 1 & 2: Planning
     * Identifies files that exceed the path length threshold.
     * Also identifies "Family Members" (e.g., .json, .texture_set) that must move
     * with the main file.
     */
    private void planMigration(Path root, int threshold) throws IOException {
        // Find all files
        List<Path> allFiles;
        try (Stream<Path> stream = Files.walk(root)) {
            allFiles = stream.sorted().filter(Files::isRegularFile).collect(Collectors.toList()); // Deterministic
        }

        for (Path file : allFiles) {
            throttle();
            String relPath = root.relativize(file).toString().replace("\\", "/");
            String fileName = file.getFileName().toString();

            // Skip if already planned
            if (movePlan.containsKey(relPath))
                continue;

            // Skip Ignored Files (Critical Configs) - Case Insensitive
            if (IGNORED_FILES.contains(fileName.toLowerCase()))
                continue;

            // Skip Ignored Directories
            if (isInIgnoredDirectory(relPath))
                continue;

            // Check if file type is movable (Textures/Sounds)
            if (!isMovableFile(fileName))
                continue;

            boolean needsMove = false;

            // Criteria 1: Length > Threshold
            if (relPath.length() > threshold) {
                needsMove = true;
            }

            // Criteria 2: Referenced by a file that IS moving (Lazy Migration)
            // (This is hard to check proactively, so we rely on Family Scan)

            if (needsMove) {
                migrateFamily(root, file);
            }
        }
    }

    private boolean isInIgnoredDirectory(String relPath) {
        String[] parts = relPath.split("/");
        if (parts.length > 0) {
            return IGNORED_DIRECTORIES.contains(parts[0].toLowerCase());
        }
        return false;
    }

    private void migrateFamily(Path root, Path mainFile) {
        String relPath = root.relativize(mainFile).toString().replace("\\", "/");
        String fileName = mainFile.getFileName().toString();

        // Identify Base Name
        String baseName = fileName;
        if (baseName.contains(".")) {
            baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        }

        // Handle .texture_set (Double extension)
        if (baseName.endsWith(".texture_set")) {
            baseName = baseName.substring(0, baseName.length() - 12);
        }

        // Handle PBR suffixes
        if (baseName.endsWith("_mer"))
            baseName = baseName.substring(0, baseName.length() - 4);
        else if (baseName.endsWith("_normal"))
            baseName = baseName.substring(0, baseName.length() - 7);
        else if (baseName.endsWith("_heightmap"))
            baseName = baseName.substring(0, baseName.length() - 10);

        // Scan directory for family members
        Path parent = mainFile.getParent();
        if (parent == null)
            return;

        File[] siblings = parent.toFile().listFiles();
        if (siblings == null)
            return;

        // Generate Hash based on the BASE file (or the first one found)
        String familyId = root.relativize(parent).toString().replace("\\", "/") + "/" + baseName;
        String hash = hash(familyId);

        for (File sibling : siblings) {
            if (sibling.isDirectory())
                continue;
            String sName = sibling.getName();
            String sRelPath = root.relativize(sibling.toPath()).toString().replace("\\", "/");

            if (movePlan.containsKey(sRelPath))
                continue; // Already moved

            // Skip Ignored Files (Critical Configs) - Case Insensitive
            if (IGNORED_FILES.contains(sName.toLowerCase()))
                continue;

            // Skip Ignored Directories
            if (isInIgnoredDirectory(sRelPath))
                continue;

            // Check if it belongs to family
            if (sName.startsWith(baseName)) {
                String remainder = sName.substring(baseName.length()); // e.g. "_mer.png" or ".json"

                if (isValidFamilyMember(remainder)) {
                    String newName = hash + remainder;
                    String newPath = SAFE_SHELTER + newName;
                    movePlan.put(sRelPath, newPath);
                }
            }
        }
    }

    private boolean isValidFamilyMember(String remainder) {
        return remainder.startsWith(".") ||
                remainder.startsWith("_mer.") ||
                remainder.startsWith("_normal.") ||
                remainder.startsWith("_heightmap.");
    }

    // ================= Phase 3: Execution & Update =================

    /**
     * Phase 3: Execution
     * Moves the files to the safe shelter directory.
     */
    private void executeMove(Path root) throws IOException {
        Path shelter = root.resolve(SAFE_SHELTER);
        Files.createDirectories(shelter);

        for (Map.Entry<String, String> entry : movePlan.entrySet()) {
            throttle();
            Path src = root.resolve(entry.getKey());
            Path dest = root.resolve(entry.getValue());

            if (Files.exists(src)) {
                Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Phase 3: Global Update
     * Updates all references in JSON and text files to point to the new locations.
     * Uses the Reverse Index to efficiently locate files that need updating.
     */
    private void updateReferences(Path root) throws IOException {
        // 1. Prepare Replacement Map (Old -> New)
        // Keys are LOWERCASE for case-insensitive matching.
        Map<String, String> replacements = new HashMap<>();
        for (Map.Entry<String, String> entry : movePlan.entrySet()) {
            String oldP = entry.getKey(); // Original casing
            String newP = entry.getValue();

            replacements.put(oldP.toLowerCase(), newP);

            // Strip extension
            String oldNoExt = stripExtension(oldP);
            String newNoExt = stripExtension(newP);
            if (!oldNoExt.equals(oldP)) {
                replacements.put(oldNoExt.toLowerCase(), newNoExt);
            }
        }

        // Sort by length (descending), then alphabetically for stability
        List<String> sortedKeys = new ArrayList<>(replacements.keySet());
        sortedKeys.sort((a, b) -> {
            int len = Integer.compare(b.length(), a.length());
            return len != 0 ? len : a.compareTo(b);
        });

        // 2. Scan all text files
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted() // Deterministic order
                    .filter(Files::isRegularFile)
                    .filter(this::isTextFile)
                    .forEach(file -> {
                        throttle();
                        try {
                            if (file.toString().endsWith(".json")) {
                                updateJsonFile(root, file, replacements);
                            } else {
                                updateTextFile(file, replacements, sortedKeys);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private void updateJsonFile(Path root, Path file, Map<String, String> replacements) throws IOException {
        try {
            JsonNode jsonRoot = jsonMapper.readTree(file.toFile());
            boolean changed = traverseAndReplace(root, file, jsonRoot, replacements);
            if (changed) {
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), jsonRoot);
            }
        } catch (Exception e) {
            // Fallback to text replacement if JSON parse fails
        }
    }

    private boolean traverseAndReplace(Path root, Path currentFile, JsonNode node,
            Map<String, String> replacements) {
        boolean changed = false;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Map<String, String> updates = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode val = field.getValue();
                if (val.isTextual()) {
                    String oldVal = val.asText();

                    // 1. Direct Match (Normalized & Lowercase)
                    String normalized = normalizePath(oldVal).toLowerCase();
                    String replacement = null;

                    if (replacements.containsKey(normalized)) {
                        replacement = replacements.get(normalized);
                    } else {
                        // 2. Resolve Absolute Path
                        String absPath = resolveAbsolutePath(root, currentFile, oldVal).toLowerCase();
                        if (replacements.containsKey(absPath)) {
                            replacement = replacements.get(absPath);
                        }
                    }

                    if (replacement != null) {
                        // Smart Extension Handling
                        if (!oldVal.contains(".")) {
                            replacement = stripExtension(replacement);
                        }
                        updates.put(field.getKey(), replacement);
                    }
                } else {
                    if (traverseAndReplace(root, currentFile, val, replacements))
                        changed = true;
                }
            }

            // Apply updates
            for (Map.Entry<String, String> update : updates.entrySet()) {
                obj.put(update.getKey(), update.getValue());
                changed = true;
            }

        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode val = arr.get(i);
                if (val.isTextual()) {
                    String oldVal = val.asText();

                    String normalized = normalizePath(oldVal).toLowerCase();
                    String replacement = null;

                    if (replacements.containsKey(normalized)) {
                        replacement = replacements.get(normalized);
                    } else {
                        String absPath = resolveAbsolutePath(root, currentFile, oldVal).toLowerCase();
                        if (replacements.containsKey(absPath)) {
                            replacement = replacements.get(absPath);
                        }
                    }

                    if (replacement != null) {
                        if (!oldVal.contains(".")) {
                            replacement = stripExtension(replacement);
                        }
                        arr.set(i, new TextNode(replacement));
                        changed = true;
                    }
                } else {
                    if (traverseAndReplace(root, currentFile, val, replacements))
                        changed = true;
                }
            }
        }
        return changed;
    }

    private void updateTextFile(Path file, Map<String, String> replacements, List<String> sortedKeys)
            throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        String original = content;
        String lowerContent = content.toLowerCase();

        for (String key : sortedKeys) {
            if (lowerContent.contains(key)) {
                content = content.replaceAll("(?i)" + Pattern.quote(key), replacements.get(key));
                lowerContent = content.toLowerCase();
            }
        }

        if (!content.equals(original)) {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        }
    }

    // ================= Helpers =================

    private String resolveAbsolutePath(Path root, Path currentJsonFile, String jsonValue) {
        String val = jsonValue.replace("\\", "/");
        if (val.contains("/")) {
            return val;
        }

        Path parent = currentJsonFile.getParent();
        if (parent == null)
            return val;

        Path target = parent.resolve(val);
        return root.relativize(target).toString().replace("\\", "/");
    }

    private String normalizePath(String path) {
        return path.replace("\\", "/");
    }

    private String stripExtension(String path) {
        int i = path.lastIndexOf('.');
        if (i > path.lastIndexOf('/')) {
            return path.substring(0, i);
        }
        return path;
    }

    private boolean isTextFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        for (String ext : TEXT_EXTENSIONS) {
            if (name.endsWith(ext))
                return true;
        }
        return false;
    }

    private boolean isMovableFile(String fileName) {
        String lower = fileName.toLowerCase();
        for (String ext : MOVABLE_EXTENSIONS) {
            if (lower.endsWith(ext))
                return true;
        }
        return false;
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (int i = 0; i < 4; i++) {
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private void unzip(Path zipFilePath, Path destDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryDestination = destDir.resolve(entry.getName()).normalize();

                if (!entryDestination.startsWith(destDir)) {
                    throw new IOException("Zip entry is outside of the target dir: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryDestination);
                } else {
                    Files.createDirectories(entryDestination.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, entryDestination, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private void zip(Path sourceDir, Path zipFilePath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            // Walk and sort to ensure deterministic zip entry order
            List<Path> files;
            try (Stream<Path> stream = Files.walk(sourceDir)) {
                files = stream.filter(path -> !Files.isDirectory(path))
                        .sorted()
                        .collect(Collectors.toList());
            }

            for (Path path : files) {
                String relPath = sourceDir.relativize(path).toString().replace("\\", "/");
                ZipEntry zipEntry = new ZipEntry(relPath);
                try {
                    zos.putNextEntry(zipEntry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    // Inner class for Reverse Index
    private static class AssetUsage {
        String file;
        String rawValue;
        UsageType type;

        public AssetUsage(String file, String rawValue, UsageType type) {
            this.file = file;
            this.rawValue = rawValue;
            this.type = type;
        }
    }

    private enum UsageType {
        KEY, VALUE
    }

    private void log(String key, Object... args) {
        // Determine locale for console log
        String locale = "en_US";
        try {
            if (BetterPack.getInstance() != null) {
                locale = BetterPack.getInstance().getConfigManager().getConfig().getDefaultLocale();
            }
        } catch (Exception e) {
            // Fallback to en_US if instance or config is not available
        }

        // Log to console using determined locale
        String defaultMsg = LanguageManager.get(locale, key);
        if (args.length > 0) {
            try {
                defaultMsg = String.format(defaultMsg, args);
            } catch (Exception e) {
                // Ignore format errors
            }
        }
        LoggerUtil.info(defaultMsg);

        // Callback with key and args for localization (e.g. for player chat)
        if (progressCallback != null) {
            progressCallback.accept(key, args);
        }
    }
}
