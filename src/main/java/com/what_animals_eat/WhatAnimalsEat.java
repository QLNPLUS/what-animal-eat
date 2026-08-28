package com.what_animals_eat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.entity.animal.Animal;
import org.slf4j.Logger;

@Mod(WhatAnimalsEat.MOD_ID)
public final class WhatAnimalsEat {
    public static final String MOD_ID = "what_animals_eat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WhatAnimalsEat() {
        MinecraftForge.EVENT_BUS.register(new BreedingFoodHandler());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        WhatAnimalsEatConfig.loadConfigIfPresent();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        WhatAnimalsEatConfig.createDefaultsIfNeeded(event.getServer());
        AttractantHandler.configureExisting(event.getServer());
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Animal animal) {
            AttractantHandler.configure(animal);
        }
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(WhatAnimalsEatConfig.RESOURCE_RELOAD_LISTENER);
    }
}
