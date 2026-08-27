package com.what_animals_eat;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(WhatAnimalsEat.MOD_ID)
public final class WhatAnimalsEat {
    public static final String MOD_ID = "what_animals_eat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WhatAnimalsEat(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(new BreedingFoodHandler());
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        WhatAnimalsEatConfig.loadConfigIfPresent();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        WhatAnimalsEatConfig.createDefaultsIfNeeded(event.getServer());
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(WhatAnimalsEatConfig.RESOURCE_RELOAD_LISTENER);
    }
}
