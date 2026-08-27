package com.what_animals_eat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public final class BreedingFoodHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ALL_ANIMALS = "*";
    private volatile List<String> lastConfig = List.of();
    private volatile ParsedRules parsedRules = ParsedRules.EMPTY;

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

    private void handleEvent(Entity target, Player player, ItemStack stack, net.minecraft.world.level.Level level,
                             java.util.function.Consumer<InteractionResult> resultSetter,
                             java.util.function.Consumer<Boolean> cancelSetter) {
        InteractionResult result = tryHandle(target, player, stack, level);
        if (result != null) {
            resultSetter.accept(result);
            cancelSetter.accept(true);
        }
    }

    private InteractionResult tryHandle(Entity target, Player player, ItemStack stack,
                                       net.minecraft.world.level.Level level) {
        if (!(target instanceof Animal animal) || stack.isEmpty()) {
            return null;
        }

        FoodRule rule = rules().forAnimal(animal);
        if (rule == null) {
            return null;
        }

        boolean configuredFood = rule.matches(stack);
        boolean vanillaFood = animal.isFood(stack);
        if (!configuredFood && !vanillaFood) {
            return null;
        }

        if (configuredFood && !level.isClientSide && animal.getAge() == 0 && animal.canFallInLove()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            animal.setInLove(player);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private ParsedRules rules() {
        List<String> configured = WhatAnimalsEatConfig.BREEDING_RULES.get().stream().map(String::valueOf).toList();
        if (!configured.equals(lastConfig)) {
            synchronized (this) {
                if (!configured.equals(lastConfig)) {
                    parsedRules = ParsedRules.parse(configured);
                    lastConfig = configured;
                }
            }
        }
        return parsedRules;
    }

    private static final class ParsedRules {
        private static final ParsedRules EMPTY = new ParsedRules(Map.of(), null);
        private final Map<ResourceLocation, FoodRule> byAnimal;
        private final FoodRule allAnimals;

        private ParsedRules(Map<ResourceLocation, FoodRule> byAnimal, FoodRule allAnimals) {
            this.byAnimal = byAnimal;
            this.allAnimals = allAnimals;
        }

        private FoodRule forAnimal(Animal animal) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());
            FoodRule exact = byAnimal.get(id);
            return exact != null ? exact : allAnimals;
        }

        private static ParsedRules parse(List<String> entries) {
            Map<ResourceLocation, FoodRule> byAnimal = new HashMap<>();
            FoodRule allAnimals = null;

            for (String entry : entries) {
                int separator = entry.indexOf('=');
                if (separator <= 0 || separator == entry.length() - 1) {
                    LOGGER.warn("Ignoring invalid breeding rule '{}'. Expected entity_id=food_id.", entry);
                    continue;
                }

                String animalText = entry.substring(0, separator).trim();
                FoodRule rule = new FoodRule();
                for (String foodText : entry.substring(separator + 1).split("\\|")) {
                    addMatcher(rule, foodText.trim(), entry);
                }
                if (rule.isEmpty()) {
                    continue;
                }

                if (ALL_ANIMALS.equals(animalText)) {
                    if (allAnimals == null) {
                        allAnimals = new FoodRule();
                    }
                    allAnimals.merge(rule);
                    continue;
                }

                ResourceLocation animalId = ResourceLocation.tryParse(animalText);
                if (animalId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(animalId)) {
                    LOGGER.warn("Ignoring breeding rule with unknown entity type '{}'.", animalText);
                    continue;
                }
                byAnimal.computeIfAbsent(animalId, ignored -> new FoodRule()).merge(rule);
            }

            return new ParsedRules(byAnimal, allAnimals);
        }

        private static void addMatcher(FoodRule rule, String foodText, String originalEntry) {
            if (foodText.isEmpty()) {
                return;
            }
            if (foodText.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(foodText.substring(1));
                if (tagId == null) {
                    LOGGER.warn("Ignoring invalid item tag in breeding rule '{}'.", originalEntry);
                    return;
                }
                rule.tags.add(TagKey.create(Registries.ITEM, tagId));
                return;
            }

            ResourceLocation itemId = ResourceLocation.tryParse(foodText);
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                LOGGER.warn("Ignoring unknown item '{}' in breeding rule '{}'.", foodText, originalEntry);
                return;
            }
            rule.items.add(BuiltInRegistries.ITEM.get(itemId));
        }
    }

    private static final class FoodRule {
        private final Set<Item> items = new HashSet<>();
        private final Set<TagKey<Item>> tags = new HashSet<>();

        private boolean matches(ItemStack stack) {
            if (items.contains(stack.getItem())) {
                return true;
            }
            for (TagKey<Item> tag : tags) {
                if (stack.is(tag)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isEmpty() {
            return items.isEmpty() && tags.isEmpty();
        }

        private void merge(FoodRule other) {
            items.addAll(other.items);
            tags.addAll(other.tags);
        }
    }
}
