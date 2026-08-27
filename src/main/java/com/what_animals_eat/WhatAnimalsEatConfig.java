package com.what_animals_eat;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

public final class WhatAnimalsEatConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BREEDING_RULES = BUILDER
            .comment(
                    "One rule per line: entity_id=item_id, entity_id=#item_tag, or *=item_id.",
                    "Separate multiple foods for one animal with |.",
                    "Configured animals replace their vanilla breeding foods with these rules.",
                    "Examples: minecraft:cow=minecraft:apple | minecraft:cow=#minecraft:flowers")
            .defineListAllowEmpty("breedingRules", List.of(), () -> "", value -> value instanceof String);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private WhatAnimalsEatConfig() {
    }
}
