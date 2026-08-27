package com.what_animals_eat.kubejs;

import com.what_animals_eat.WhatAnimalsEatKubeHooks;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public final class WhatAnimalsEatKubeJSPlugin implements KubeJSPlugin {
    public static final EventGroup EVENTS = EventGroup.of("WhatAnimalsEatEvents");
    public static final EventHandler BEFORE_FED = EVENTS
            .server("beforeAnimalFed", () -> BeforeAnimalFedEventJS.class).hasResult();
    public static final EventHandler AFTER_FED = EVENTS
            .server("afterAnimalFed", () -> AfterAnimalFedEventJS.class);

    @Override
    public void init() {
        WhatAnimalsEatKubeHooks.install((player, animal, item) -> {
            if (!BEFORE_FED.hasListeners()) {
                return true;
            }
            EventResult result = BEFORE_FED.post(new BeforeAnimalFedEventJS(player, animal, item));
            return !result.interruptFalse();
        }, (player, animal, item) -> {
            if (AFTER_FED.hasListeners()) {
                AFTER_FED.post(new AfterAnimalFedEventJS(player, animal, item));
            }
        });
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(EVENTS);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("WhatAnimalsEat", KubeJsBreedingApi.INSTANCE);
    }
}
