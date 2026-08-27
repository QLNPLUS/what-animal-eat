package com.what_animals_eat;

import java.util.function.Consumer;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BreedingFoodHandler {
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handleEvent(event.getTarget(), event.getEntity(), event.getItemStack(), event.getLevel(),
                event::setCancellationResult, event::setCanceled);
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleEvent(event.getTarget(), event.getEntity(), event.getItemStack(), event.getLevel(),
                event::setCancellationResult, event::setCanceled);
    }

    private void handleEvent(Entity target, Player player, ItemStack stack, Level level,
                             Consumer<InteractionResult> resultSetter, Consumer<Boolean> cancelSetter) {
        InteractionResult result = tryHandle(target, player, stack, level);
        if (result != null) {
            resultSetter.accept(result);
            cancelSetter.accept(true);
        }
    }

    private InteractionResult tryHandle(Entity target, Player player, ItemStack stack, Level level) {
        if (!(target instanceof Animal animal) || stack.isEmpty()) {
            return null;
        }

        BreedingFoodRules.FoodRule rule = BreedingFoodRules.forAnimal(animal);
        if (rule == null) {
            return null;
        }

        boolean configuredFood = rule.matches(stack);
        boolean vanillaFood = animal.isFood(stack);
        if (!configuredFood && !vanillaFood) {
            return null;
        }

        if (configuredFood && !level.isClientSide && animal.getAge() == 0 && animal.canFallInLove()) {
            ItemStack fedItem = stack.copyWithCount(1);
            if (!WhatAnimalsEatKubeHooks.before(player, animal, fedItem)) {
                return InteractionResult.sidedSuccess(false);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            animal.setInLove(player);
            WhatAnimalsEatKubeHooks.after(player, animal, fedItem);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
