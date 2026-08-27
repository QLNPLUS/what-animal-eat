package com.what_animals_eat;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(WhatAnimalsEat.MOD_ID)
public final class WhatAnimalsEat {
    public static final String MOD_ID = "what_animals_eat";

    public WhatAnimalsEat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WhatAnimalsEatConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new BreedingFoodHandler());
    }
}
