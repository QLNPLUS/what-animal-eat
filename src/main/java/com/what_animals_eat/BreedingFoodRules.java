package com.what_animals_eat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class BreedingFoodRules {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ALL_ANIMALS = "*";
    private static final Map<String, Set<String>> RUNTIME_RULES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> RUNTIME_ENTITY_RULES = new ConcurrentHashMap<>();
    private static volatile List<String> lastConfig = List.of();
    private static volatile ParsedRules configRules = ParsedRules.EMPTY;

    private BreedingFoodRules() {
    }

    public static void reloadConfig() {
        List<String> entries = configEntries();
        configRules = ParsedRules.parse(entries);
        lastConfig = entries;
    }

    public static FoodRule forAnimal(Animal animal) {
        ParsedRules config = configRules();
        Set<String> runtimeEntity = RUNTIME_ENTITY_RULES.get(animal.getUUID());
        if (runtimeEntity != null) {
            return FoodRule.parse(runtimeEntity);
        }

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());
        String key = id == null ? "" : id.toString();

        Set<String> runtimeExact = RUNTIME_RULES.get(key);
        if (runtimeExact != null) {
            return FoodRule.parse(runtimeExact);
        }
        Set<String> runtimeAll = RUNTIME_RULES.get(ALL_ANIMALS);
        if (runtimeAll != null) {
            return FoodRule.parse(runtimeAll);
        }
        return config.forAnimal(id);
    }

    public static boolean setRuntimeRule(String animalId, Collection<String> foodIds) {
        String key = normalizeAnimalId(animalId);
        if (key == null) {
            return false;
        }
        Set<String> normalized = normalizeFoodIds(foodIds, "runtime rule for " + key);
        if (foodIds != null && !foodIds.isEmpty() && normalized.isEmpty()) {
            return false;
        }
        RUNTIME_RULES.put(key, Set.copyOf(normalized));
        return true;
    }

    public static boolean setRuntimeRule(Animal animal, Collection<String> foodIds) {
        if (animal == null) {
            return false;
        }
        String key = entityTypeId(animal);
        Set<String> normalized = normalizeFoodIds(foodIds, "runtime rule for " + key);
        if (foodIds != null && !foodIds.isEmpty() && normalized.isEmpty()) {
            return false;
        }
        RUNTIME_ENTITY_RULES.put(animal.getUUID(), Set.copyOf(normalized));
        return true;
    }

    public static boolean addRuntimeFood(String animalId, String foodId) {
        String key = normalizeAnimalId(animalId);
        if (key == null || foodId == null) {
            return false;
        }
        Set<String> food = normalizeFoodIds(List.of(foodId), "runtime rule for " + key);
        if (food.isEmpty()) {
            return false;
        }
        RUNTIME_RULES.compute(key, (ignored, old) -> {
            Set<String> result = old == null ? new LinkedHashSet<>() : new LinkedHashSet<>(old);
            result.addAll(food);
            return Set.copyOf(result);
        });
        return true;
    }

    public static boolean addRuntimeFood(Animal animal, String foodId) {
        if (animal == null || foodId == null) {
            return false;
        }
        String key = entityTypeId(animal);
        Set<String> food = normalizeFoodIds(List.of(foodId), "runtime rule for " + key);
        if (food.isEmpty()) {
            return false;
        }
        RUNTIME_ENTITY_RULES.compute(animal.getUUID(), (ignored, old) -> {
            Set<String> result = old == null ? new LinkedHashSet<>() : new LinkedHashSet<>(old);
            result.addAll(food);
            return Set.copyOf(result);
        });
        return true;
    }

    public static boolean removeRuntimeFood(String animalId, String foodId) {
        String key = normalizeAnimalId(animalId);
        if (key == null || foodId == null) {
            return false;
        }
        Set<String> food = normalizeFoodIds(List.of(foodId), "runtime rule for " + key);
        Set<String> current = RUNTIME_RULES.get(key);
        if (food.isEmpty() || current == null) {
            return false;
        }
        Set<String> updated = new LinkedHashSet<>(current);
        boolean changed = updated.removeAll(food);
        if (changed) {
            RUNTIME_RULES.put(key, Set.copyOf(updated));
        }
        return changed;
    }

    public static boolean removeRuntimeFood(Animal animal, String foodId) {
        if (animal == null || foodId == null) {
            return false;
        }
        String key = entityTypeId(animal);
        Set<String> food = normalizeFoodIds(List.of(foodId), "runtime rule for " + key);
        Set<String> current = RUNTIME_ENTITY_RULES.get(animal.getUUID());
        if (food.isEmpty() || current == null) {
            return false;
        }
        Set<String> updated = new LinkedHashSet<>(current);
        boolean changed = updated.removeAll(food);
        if (changed) {
            RUNTIME_ENTITY_RULES.put(animal.getUUID(), Set.copyOf(updated));
        }
        return changed;
    }

    public static boolean clearRuntimeRule(String animalId) {
        String key = normalizeAnimalId(animalId);
        return key != null && RUNTIME_RULES.remove(key) != null;
    }

    public static boolean clearRuntimeRule(Animal animal) {
        return animal != null && RUNTIME_ENTITY_RULES.remove(animal.getUUID()) != null;
    }

    public static List<String> getEffectiveFoods(String animalId) {
        String key = normalizeAnimalIdForRead(animalId);
        if (key == null) {
            return List.of();
        }
        Set<String> runtimeExact = RUNTIME_RULES.get(key);
        if (runtimeExact != null) {
            return List.copyOf(runtimeExact);
        }
        Set<String> runtimeAll = RUNTIME_RULES.get(ALL_ANIMALS);
        if (runtimeAll != null) {
            return List.copyOf(runtimeAll);
        }

        ParsedRules config = configRules();
        ResourceLocation id = ResourceLocation.tryParse(key);
        FoodRule rule = id == null ? null : config.byAnimal.get(id);
        if (rule == null) {
            rule = config.allAnimals;
        }
        return rule == null ? List.of() : rule.identifiers();
    }

    public static List<String> getEffectiveFoods(Animal animal) {
        if (animal == null) {
            return List.of();
        }
        Set<String> runtimeEntity = RUNTIME_ENTITY_RULES.get(animal.getUUID());
        if (runtimeEntity != null) {
            return List.copyOf(runtimeEntity);
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());
        return id == null ? List.of() : getEffectiveFoods(id.toString());
    }

    /** Scans every registered entity type and item after all mods have registered. */
    public static List<String> discoverDefaults(Level level) {
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            items.add(item);
        }
        items.sort(Comparator.comparing(item -> idOf(BuiltInRegistries.ITEM.getKey(item))));

        List<String> result = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (entityId == null) {
                continue;
            }
            Entity created = null;
            try {
                created = type.create(level);
                if (!(created instanceof Animal animal)) {
                    continue;
                }
                List<String> foods = new ArrayList<>();
                for (Item item : items) {
                    ItemStack stack = item.getDefaultInstance();
                    if (!stack.isEmpty() && animal.isFood(stack)) {
                        foods.add(idOf(BuiltInRegistries.ITEM.getKey(item)));
                    }
                }
                if (!foods.isEmpty()) {
                    result.add(entityId + "=" + String.join("|", foods));
                }
            } catch (RuntimeException exception) {
                LOGGER.debug("Unable to inspect breeding food for entity type {}", entityId, exception);
            } finally {
                if (created != null) {
                    created.discard();
                }
            }
        }
        result.sort(String::compareTo);
        return List.copyOf(result);
    }

    private static ParsedRules configRules() {
        List<String> configured = configEntries();
        if (!configured.equals(lastConfig)) {
            synchronized (BreedingFoodRules.class) {
                if (!configured.equals(lastConfig)) {
                    configRules = ParsedRules.parse(configured);
                    lastConfig = configured;
                }
            }
        }
        return configRules;
    }

    private static List<String> configEntries() {
        return WhatAnimalsEatConfig.getConfiguredEntries();
    }

    private static String normalizeAnimalId(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (ALL_ANIMALS.equals(value)) {
            return value;
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            LOGGER.warn("Unknown entity type '{}' in breeding food API", raw);
            return null;
        }
        return id.toString();
    }

    private static String normalizeAnimalIdForRead(String raw) {
        if (ALL_ANIMALS.equals(raw)) {
            return raw;
        }
        ResourceLocation id = ResourceLocation.tryParse(raw == null ? "" : raw.trim());
        return id == null ? null : id.toString();
    }

    private static Set<String> normalizeFoodIds(Collection<String> foodIds, String context) {
        Set<String> normalized = new LinkedHashSet<>();
        if (foodIds == null) {
            return normalized;
        }
        for (String raw : foodIds) {
            if (raw == null) {
                continue;
            }
            String value = raw.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (value.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(value.substring(1));
                if (tagId == null) {
                    LOGGER.warn("Ignoring invalid item tag '{}' in {}", value, context);
                } else {
                    normalized.add("#" + tagId);
                }
                continue;
            }
            ResourceLocation itemId = ResourceLocation.tryParse(value);
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                LOGGER.warn("Ignoring unknown item '{}' in {}", value, context);
            } else {
                normalized.add(itemId.toString());
            }
        }
        return normalized;
    }

    private static String idOf(ResourceLocation id) {
        return id == null ? "" : id.toString();
    }

    private static String entityTypeId(Animal animal) {
        return idOf(BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()));
    }

    public static final class FoodRule {
        private final Set<Item> items;
        private final Set<TagKey<Item>> tags;
        private final List<String> identifiers;

        private FoodRule(Set<Item> items, Set<TagKey<Item>> tags, List<String> identifiers) {
            this.items = Set.copyOf(items);
            this.tags = Set.copyOf(tags);
            this.identifiers = List.copyOf(identifiers);
        }

        private static FoodRule parse(Collection<String> ids) {
            Set<Item> items = new HashSet<>();
            Set<TagKey<Item>> tags = new HashSet<>();
            List<String> identifiers = new ArrayList<>();
            for (String value : ids) {
                if (value.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(value.substring(1));
                    if (tagId != null) {
                        tags.add(TagKey.create(Registries.ITEM, tagId));
                        identifiers.add("#" + tagId);
                    }
                } else {
                    ResourceLocation itemId = ResourceLocation.tryParse(value);
                    if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
                        items.add(BuiltInRegistries.ITEM.get(itemId));
                        identifiers.add(itemId.toString());
                    }
                }
            }
            return new FoodRule(items, tags, identifiers);
        }

        public boolean matches(ItemStack stack) {
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

        public List<String> identifiers() {
            return identifiers;
        }
    }

    private static final class ParsedRules {
        private static final ParsedRules EMPTY = new ParsedRules(Map.of(), null);
        private final Map<ResourceLocation, FoodRule> byAnimal;
        private final FoodRule allAnimals;

        private ParsedRules(Map<ResourceLocation, FoodRule> byAnimal, FoodRule allAnimals) {
            this.byAnimal = byAnimal;
            this.allAnimals = allAnimals;
        }

        private FoodRule forAnimal(ResourceLocation id) {
            FoodRule exact = id == null ? null : byAnimal.get(id);
            return exact != null ? exact : allAnimals;
        }

        private static ParsedRules parse(List<String> entries) {
            Map<ResourceLocation, FoodRuleBuilder> builders = new HashMap<>();
            FoodRuleBuilder all = null;
            for (String raw : entries) {
                int separator = raw.indexOf('=');
                if (separator <= 0 || separator == raw.length() - 1) {
                    LOGGER.warn("Ignoring invalid breeding rule '{}'. Expected entity_id=food_id.", raw);
                    continue;
                }
                String animalText = raw.substring(0, separator).trim();
                String animalKey = ALL_ANIMALS.equals(animalText) ? ALL_ANIMALS : normalizeAnimalId(animalText);
                if (animalKey == null) {
                    continue;
                }

                FoodRuleBuilder builder;
                if (ALL_ANIMALS.equals(animalKey)) {
                    if (all == null) {
                        all = new FoodRuleBuilder();
                    }
                    builder = all;
                } else {
                    ResourceLocation id = ResourceLocation.tryParse(animalKey);
                    builder = builders.computeIfAbsent(id, ignored -> new FoodRuleBuilder());
                }
                for (String food : raw.substring(separator + 1).split("\\|")) {
                    builder.add(food.trim(), raw);
                }
            }

            Map<ResourceLocation, FoodRule> parsed = new HashMap<>();
            builders.forEach((id, builder) -> parsed.put(id, builder.build()));
            return new ParsedRules(parsed, all == null ? null : all.build());
        }
    }

    private static final class FoodRuleBuilder {
        private final Set<String> ids = new LinkedHashSet<>();

        private void add(String raw, String context) {
            ids.addAll(normalizeFoodIds(List.of(raw), context));
        }

        private FoodRule build() {
            return FoodRule.parse(ids);
        }
    }
}
