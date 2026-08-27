package com.what_animals_eat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

public final class WhatAnimalsEatConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("what_animals_eat-common.toml");
    private static final boolean CONFIG_EXISTED_AT_LOAD = Files.exists(CONFIG_PATH);
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BREEDING_RULES = BUILDER
            .comment(
                    "One rule per line: entity_id=item_id, entity_id=#item_tag, or *=item_id.",
                    "Separate multiple foods for one animal with |.",
                    "Configured animals replace their vanilla breeding foods with these rules.",
                    "The first file is generated with all detected breeding animals and their vanilla foods.")
            .defineListAllowEmpty("breedingRules", List.<String>of(), value -> value instanceof String);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private WhatAnimalsEatConfig() {
    }

    public static void createDefaultsIfNeeded(ServerLevel level) {
        if (CONFIG_EXISTED_AT_LOAD) {
            BreedingFoodRules.reloadConfig();
            return;
        }

        List<String> defaults = BreedingFoodRules.discoverDefaults(level);
        BREEDING_RULES.set(defaults);
        SPEC.save();
        BreedingFoodRules.reloadConfig();
        WhatAnimalsEat.LOGGER.info("Generated {} breeding food rules at {}", defaults.size(), CONFIG_PATH);
    }
}
