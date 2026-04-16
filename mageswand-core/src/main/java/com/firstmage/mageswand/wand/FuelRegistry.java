package com.firstmage.mageswand.wand;

import com.firstmage.mageswand.MagesWand;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FuelRegistry {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final MagesWand plugin;
    private final WandExecutorRegistry executorRegistry;
    private final Map<FuelSignature, RegisteredFuel> fuels = new LinkedHashMap<>();

    public FuelRegistry(MagesWand plugin, WandExecutorRegistry executorRegistry) {
        this.plugin = plugin;
        this.executorRegistry = executorRegistry;
    }

    public synchronized LoadReport load() {
        this.fuels.clear();
        List<String> issues = new ArrayList<>();

        Path path = this.plugin.getDataFolder().toPath().resolve("wand-fuels.json");
        try (Reader reader = Files.newBufferedReader(path)) {
            List<FuelMapping> mappings = parseMappings(reader);
            if (mappings == null) {
                issues.add("wand-fuels.json was empty or invalid. No fuels were loaded.");
                return new LoadReport(0, 1, List.copyOf(issues));
            }

            int loadedMappings = 0;
            int skippedMappings = 0;

            for (FuelMapping mapping : mappings) {
                if (mapping == null) {
                    skippedMappings++;
                    issues.add("Skipped a blank mapping entry.");
                    continue;
                }

                FuelSignature signature = FuelSignature.parseConfigKey(mapping.fuel);
                if (signature == null) {
                    skippedMappings++;
                    issues.add("Skipping invalid fuel key: " + mapping.fuel);
                    continue;
                }

                String executorClassName = firstNonBlank(mapping.executorClass, mapping.className);
                if (executorClassName.isBlank()) {
                    skippedMappings++;
                    issues.add("Skipping fuel " + mapping.fuel + " because executorClass was blank.");
                    continue;
                }

                WandExecutor executor;
                try {
                    executor = this.executorRegistry.createExecutor(executorClassName);
                } catch (RuntimeException exception) {
                    skippedMappings++;
                    issues.add("Skipping fuel " + mapping.fuel + " because executor " + executorClassName + " failed to instantiate: " + exception.getMessage());
                    continue;
                }

                if (executor == null) {
                    skippedMappings++;
                    issues.add("Skipping fuel " + mapping.fuel + " because executor class " + executorClassName + " could not be found.");
                    continue;
                }

                String spellName = firstNonBlank(mapping.spellName, simpleClassName(executorClassName));
                String spellDescription = firstNonBlank(mapping.description, "");
                boolean operatorOnly = Boolean.TRUE.equals(mapping.operatorOnly);
                String executorSource = this.executorRegistry.describeClassSource(executorClassName);

                RegisteredFuel registeredFuel = new RegisteredFuel(
                        signature,
                        spellName,
                        spellDescription,
                        operatorOnly,
                        executorClassName,
                        executorSource,
                        executor
                );

                RegisteredFuel previous = this.fuels.put(signature, registeredFuel);
                if (previous != null) {
                    issues.add("Duplicate fuel mapping for " + mapping.fuel + " replaced executor "
                            + previous.executorClass() + " with " + executorClassName + '.');
                }

                loadedMappings++;
            }

            return new LoadReport(loadedMappings, skippedMappings, List.copyOf(issues));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read wand-fuels.json", exception);
        }
    }

    public synchronized RegisteredFuel findMatchingFuel(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }

        FuelSignature exact = FuelSignature.fromItem(stack);
        RegisteredFuel direct = this.fuels.get(exact);
        if (direct != null) {
            return direct;
        }

        return this.fuels.get(new FuelSignature(stack.getType(), ""));
    }

private List<FuelMapping> parseMappings(Reader reader) throws IOException {
    JsonElement root = this.gson.fromJson(reader, JsonElement.class);
    if (root == null || root.isJsonNull()) {
        return null;
    }

    if (root.isJsonArray()) {
        return this.gson.fromJson(root, new TypeToken<List<FuelMapping>>() {}.getType());
    }

    if (root.isJsonObject()) {
        FuelFile fuelFile = this.gson.fromJson(root, FuelFile.class);
        return fuelFile == null ? null : fuelFile.mappings;
    }

    throw new JsonParseException("wand-fuels.json must be a JSON object with a mappings array or a top-level array of mappings.");
}

    public synchronized List<RegisteredFuel> getRegisteredFuels() {
        return this.fuels.values().stream()
                .sorted(Comparator
                        .comparing((RegisteredFuel fuel) -> fuel.signature().material().name())
                        .thenComparing(RegisteredFuel::spellName))
                .toList();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private String simpleClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }

    public record LoadReport(int loadedMappings, int skippedMappings, List<String> issues) {
    }

    private static final class FuelFile {
        private List<FuelMapping> mappings = new ArrayList<>();
    }

    private static final class FuelMapping {
        private String fuel = "";
        private String executorClass = "";
        private String className = "";
        private String spellName = "";
        private String description = "";
        private Boolean operatorOnly;
    }
}
