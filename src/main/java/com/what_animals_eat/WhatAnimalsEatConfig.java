package com.what_animals_eat;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.loading.FMLPaths;

public final class WhatAnimalsEatConfig {
    private static final Path FOOD_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("what_animals_eat_food.json");
    private static final Path ATTRACTANT_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("what_animals_eat_attractant.json");
    private static final Path LEGACY_JSON_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("what_animals_eat.json");
    private static final Path LEGACY_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("what_animals_eat-common.toml");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Pattern LEGACY_ENTRY = Pattern.compile("\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
    private static volatile List<String> configuredFoodEntries = List.of();
    private static volatile List<String> configuredAttractantEntries = List.of();
    public static final SimplePreparableReloadListener<Void> RESOURCE_RELOAD_LISTENER =
            new SimplePreparableReloadListener<>() {
                @Override
                protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                    return null;
                }

                @Override
                protected void apply(Void ignored, ResourceManager resourceManager, ProfilerFiller profiler) {
                    reloadConfig();
                    WhatAnimalsEat.LOGGER.info("Reloaded {} breeding food rules from {}", configuredFoodEntries.size(), FOOD_CONFIG_PATH);
                    WhatAnimalsEat.LOGGER.info("Reloaded {} attractant rules from {}", configuredAttractantEntries.size(), ATTRACTANT_CONFIG_PATH);
                }
            };

    private WhatAnimalsEatConfig() {
    }

    public static List<String> getFoodConfiguredEntries() {
        return configuredFoodEntries;
    }

    public static List<String> getAttractantConfiguredEntries() {
        return configuredAttractantEntries;
    }

    public static void reloadConfig() {
        configuredFoodEntries = readJsonEntries(FOOD_CONFIG_PATH, "breedingRules");
        configuredAttractantEntries = readJsonEntries(ATTRACTANT_CONFIG_PATH, "attractants");
    }

    public static void loadConfigIfPresent() {
        if (!Files.exists(FOOD_CONFIG_PATH)) {
            if (Files.exists(LEGACY_JSON_CONFIG_PATH)) {
                List<String> legacyJsonEntries = readJsonEntries(LEGACY_JSON_CONFIG_PATH, "breedingRules");
                if (!legacyJsonEntries.isEmpty()) {
                    writeJson(FOOD_CONFIG_PATH, legacyJsonEntries);
                    WhatAnimalsEat.LOGGER.info("Migrated {} breeding food rules to {}", legacyJsonEntries.size(), FOOD_CONFIG_PATH);
                }
            } else {
                List<String> legacyEntries = readLegacyEntries();
                if (!legacyEntries.isEmpty()) {
                    writeJson(FOOD_CONFIG_PATH, legacyEntries);
                    WhatAnimalsEat.LOGGER.info("Migrated {} breeding food rules to {}", legacyEntries.size(), FOOD_CONFIG_PATH);
                }
            }
        }
        reloadConfig();
    }

    public static void createDefaultsIfNeeded(MinecraftServer server) {
        if (server.overworld() == null) {
            WhatAnimalsEat.LOGGER.error("Cannot generate breeding food defaults before the overworld is available");
            return;
        }

        if (!Files.exists(FOOD_CONFIG_PATH)) {
            List<String> defaults = BreedingFoodRules.discoverDefaults(server.overworld());
            writeJson(FOOD_CONFIG_PATH, defaults);
            WhatAnimalsEat.LOGGER.info("Generated {} breeding food rules at {}", defaults.size(), FOOD_CONFIG_PATH);
        }
        if (!Files.exists(ATTRACTANT_CONFIG_PATH)) {
            List<String> defaults = AttractantRules.discoverDefaults(server.overworld());
            writeJson(ATTRACTANT_CONFIG_PATH, defaults);
            WhatAnimalsEat.LOGGER.info("Generated {} attractant rules at {}", defaults.size(), ATTRACTANT_CONFIG_PATH);
        }
        reloadConfig();
    }

    private static List<String> readJsonEntries(Path path, String wrapperKey) {
        if (!Files.exists(path)) {
            return List.of();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                throw new JsonParseException("The root value must be a JSON object");
            }

            JsonObject rulesObject = root.getAsJsonObject();
            JsonElement wrappedRules = rulesObject.get(wrapperKey);
            if (wrappedRules != null && wrappedRules.isJsonObject()) {
                rulesObject = wrappedRules.getAsJsonObject();
            }

            List<String> entries = new ArrayList<>();
            for (Map.Entry<String, JsonElement> rule : rulesObject.entrySet()) {
                List<String> foods = new ArrayList<>();
                JsonElement value = rule.getValue();
                if (value.isJsonArray()) {
                    for (JsonElement food : value.getAsJsonArray()) {
                        if (food.isJsonPrimitive() && food.getAsJsonPrimitive().isString()) {
                            foods.add(food.getAsString());
                        }
                    }
                } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    foods.add(value.getAsString());
                }
                if (!foods.isEmpty()) {
                    entries.add(rule.getKey() + "=" + String.join("|", foods));
                }
            }
            return List.copyOf(entries);
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            WhatAnimalsEat.LOGGER.error("Unable to read JSON config {}", path, exception);
            return List.of();
        }
    }

    private static List<String> readLegacyEntries() {
        if (!Files.exists(LEGACY_CONFIG_PATH)) {
            return List.of();
        }

        try {
            String content = Files.readString(LEGACY_CONFIG_PATH, StandardCharsets.UTF_8);
            int keyStart = content.indexOf("breedingRules");
            int arrayStart = keyStart < 0 ? -1 : content.indexOf('[', keyStart);
            int arrayEnd = arrayStart < 0 ? -1 : content.indexOf(']', arrayStart);
            if (arrayStart < 0 || arrayEnd < 0) {
                return List.of();
            }

            Matcher matcher = LEGACY_ENTRY.matcher(content.substring(arrayStart + 1, arrayEnd));
            List<String> entries = new ArrayList<>();
            while (matcher.find()) {
                entries.add(matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
            }
            return List.copyOf(entries);
        } catch (IOException exception) {
            WhatAnimalsEat.LOGGER.error("Unable to read legacy breeding food config {}", LEGACY_CONFIG_PATH, exception);
            return List.of();
        }
    }

    static void writeJson(Path path, List<String> entries) {
        Map<String, Set<String>> grouped = new TreeMap<>();
        for (String raw : entries) {
            int separator = raw.indexOf('=');
            if (separator <= 0 || separator == raw.length() - 1) {
                continue;
            }
            String animal = raw.substring(0, separator).trim();
            Set<String> foods = grouped.computeIfAbsent(animal, ignored -> new LinkedHashSet<>());
            for (String food : raw.substring(separator + 1).split("\\|")) {
                String value = food.trim();
                if (!value.isEmpty()) {
                    foods.add(value);
                }
            }
        }

        JsonObject root = new JsonObject();
        for (Map.Entry<String, Set<String>> rule : grouped.entrySet()) {
            JsonArray foods = new JsonArray();
            rule.getValue().forEach(foods::add);
            root.add(rule.getKey(), foods);
        }

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            WhatAnimalsEat.LOGGER.error("Unable to write JSON config {}", path, exception);
        }
    }
}
