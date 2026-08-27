package com.what_animals_eat;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WhatAnimalsEatKubeHooks {
    @FunctionalInterface
    public interface BeforeHook {
        boolean call(Player player, Animal animal, ItemStack item);
    }

    @FunctionalInterface
    public interface AfterHook {
        void call(Player player, Animal animal, ItemStack item);
    }

    private static volatile BeforeHook beforeHook;
    private static volatile AfterHook afterHook;

    private WhatAnimalsEatKubeHooks() {
    }

    public static void install(BeforeHook before, AfterHook after) {
        beforeHook = before;
        afterHook = after;
    }

    public static boolean before(Player player, Animal animal, ItemStack item) {
        BeforeHook hook = beforeHook;
        return hook == null || hook.call(player, animal, item);
    }

    public static void after(Player player, Animal animal, ItemStack item) {
        AfterHook hook = afterHook;
        if (hook != null) {
            hook.call(player, animal, item);
        }
    }
}
