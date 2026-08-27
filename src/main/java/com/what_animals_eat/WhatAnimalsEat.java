package com.what_animals_eat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

@Mod(WhatAnimalsEat.MOD_ID)
public final class WhatAnimalsEat {
    public static final String MOD_ID = "what_animals_eat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WhatAnimalsEat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WhatAnimalsEatConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new BreedingFoodHandler());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        WhatAnimalsEatConfig.createDefaultsIfNeeded(event.getServer().overworld());
    }
}
