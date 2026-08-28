package com.what_animals_eat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public final class AttractantRules {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ALL_ANIMALS = "*";
    private static final Map<String, Set<String>> RUNTIME_RULES = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile List<String> lastConfig = List.of();
    private static volatile ParsedRules configRules = ParsedRules.EMPTY;
    private static final Field TEMPT_ITEMS = findTemptItemsField();

    private AttractantRules() {
    }

    public static FoodRule forAnimal(Animal animal) {
        ParsedRules config = configRules();
        Set<String> entityRule = EntityRuleData.get(animal, EntityRuleData.ATTRACTANT_KEY);
        if (entityRule != null) {
            return FoodRule.parse(entityRule);
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

    public static boolean setRuntimeRule(String animalId, Collection<String> itemIds) {
        String key = normalizeAnimalId(animalId);
        if (key == null) {
            return false;
        }
        Set<String> normalized = normalizeItemIds(itemIds, "runtime attractant rule for " + key);
        if (itemIds != null && !itemIds.isEmpty() && normalized.isEmpty()) {
            return false;
        }
        RUNTIME_RULES.put(key, Set.copyOf(normalized));
        return true;
    }

    public static boolean setRuntimeRule(Animal animal, Collection<String> itemIds) {
        if (animal == null) {
            return false;
        }
        String key = entityTypeId(animal);
        Set<String> normalized = normalizeItemIds(itemIds, "runtime attractant rule for " + key);
        if (itemIds != null && !itemIds.isEmpty() && normalized.isEmpty()) {
            return false;
        }
        EntityRuleData.put(animal, EntityRuleData.ATTRACTANT_KEY, normalized);
        return true;
    }

    public static boolean addRuntimeFood(String animalId, String itemId) {
        String key = normalizeAnimalId(animalId);
        if (key == null || itemId == null) {
            return false;
        }
        Set<String> food = normalizeItemIds(List.of(itemId), "runtime attractant rule for " + key);
        if (food.isEmpty()) {
            return false;
        }
        RUNTIME_RULES.compute(key, (ignored, old) -> merge(old, food));
        return true;
    }

    public static boolean addRuntimeFood(Animal animal, String itemId) {
        if (animal == null || itemId == null) {
            return false;
        }
        String key = entityTypeId(animal);
        Set<String> food = normalizeItemIds(List.of(itemId), "runtime attractant rule for " + key);
        if (food.isEmpty()) {
            return false;
        }
        Set<String> current = EntityRuleData.get(animal, EntityRuleData.ATTRACTANT_KEY);
        EntityRuleData.put(animal, EntityRuleData.ATTRACTANT_KEY, merge(current, food));
        return true;
    }

    public static boolean removeRuntimeFood(String animalId, String itemId) {
        String key = normalizeAnimalId(animalId);
        if (key == null || itemId == null) {
            return false;
        }
        return removeFromMap(key, itemId);
    }

    public static boolean removeRuntimeFood(Animal animal, String itemId) {
        if (animal == null || itemId == null) {
            return false;
        }
        String key = entityTypeId(animal);
        Set<String> item = normalizeItemIds(List.of(itemId), "runtime attractant rule for " + key);
        Set<String> current = EntityRuleData.get(animal, EntityRuleData.ATTRACTANT_KEY);
        if (item.isEmpty() || current == null) {
            return false;
        }
        Set<String> updated = new LinkedHashSet<>(current);
        boolean changed = updated.removeAll(item);
        if (changed) {
            EntityRuleData.put(animal, EntityRuleData.ATTRACTANT_KEY, updated);
        }
        return changed;
    }

    public static boolean clearRuntimeRule(String animalId) {
        String key = normalizeAnimalId(animalId);
        return key != null && RUNTIME_RULES.remove(key) != null;
    }

    public static boolean clearRuntimeRule(Animal animal) {
        return animal != null && EntityRuleData.remove(animal, EntityRuleData.ATTRACTANT_KEY);
    }

    public static List<String> getEffectiveAttractants(String animalId) {
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

    public static List<String> getEffectiveAttractants(Animal animal) {
        if (animal == null) {
            return List.of();
        }
        Set<String> entityRule = EntityRuleData.get(animal, EntityRuleData.ATTRACTANT_KEY);
        if (entityRule != null) {
            return List.copyOf(entityRule);
        }
        return getEffectiveAttractants(entityTypeId(animal));
    }

    /** Scans the registered TemptGoal predicates from vanilla and other loaded mods. */
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
                List<String> attractants = discoverAttractants(animal, items);
                if (!attractants.isEmpty()) {
                    result.add(entityId + "=" + String.join("|", attractants));
                }
            } catch (RuntimeException exception) {
                LOGGER.debug("Unable to inspect attractants for entity type {}", entityId, exception);
            } finally {
                if (created != null) {
                    created.discard();
                }
            }
        }
        result.sort(String::compareTo);
        return List.copyOf(result);
    }

    private static List<String> discoverAttractants(Animal animal, List<Item> items) {
        Set<String> result = new LinkedHashSet<>();
        for (var wrappedGoal : animal.goalSelector.getAvailableGoals()) {
            if (!(wrappedGoal.getGoal() instanceof TemptGoal goal)) {
                continue;
            }
            if (TEMPT_ITEMS == null) {
                for (Item item : items) {
                    ItemStack stack = item.getDefaultInstance();
                    if (!stack.isEmpty() && animal.isFood(stack)) {
                        result.add(idOf(BuiltInRegistries.ITEM.getKey(item)));
                    }
                }
                break;
            }
            try {
                Object predicate = TEMPT_ITEMS.get(goal);
                if (predicate instanceof Predicate<?> rawPredicate) {
                    @SuppressWarnings("unchecked")
                    Predicate<ItemStack> itemPredicate = (Predicate<ItemStack>) rawPredicate;
                    for (Item item : items) {
                        ItemStack stack = item.getDefaultInstance();
                        if (!stack.isEmpty() && itemPredicate.test(stack)) {
                            result.add(idOf(BuiltInRegistries.ITEM.getKey(item)));
                        }
                    }
                }
            } catch (IllegalAccessException exception) {
                LOGGER.debug("Unable to read vanilla attractant predicate", exception);
            }
        }
        return result.stream().sorted().toList();
    }

    private static Field findTemptItemsField() {
        try {
            Field field = TemptGoal.class.getDeclaredField("items");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Unable to inspect vanilla attractant predicates; using breeding foods as fallback", exception);
            return null;
        }
    }

    private static Set<String> merge(Set<String> old, Collection<String> additions) {
        Set<String> result = new LinkedHashSet<>(old == null ? Set.of() : old);
        result.addAll(additions);
        return Set.copyOf(result);
    }

    private static boolean removeFromMap(String key, String itemId) {
        Set<String> item = normalizeItemIds(List.of(itemId), "runtime attractant rule for " + key);
        Set<String> current = RUNTIME_RULES.get(key);
        if (item.isEmpty() || current == null) {
            return false;
        }
        Set<String> updated = new LinkedHashSet<>(current);
        boolean changed = updated.removeAll(item);
        if (changed) {
            RUNTIME_RULES.put(key, Set.copyOf(updated));
        }
        return changed;
    }

    private static ParsedRules configRules() {
        List<String> configured = WhatAnimalsEatConfig.getAttractantConfiguredEntries();
        if (!configured.equals(lastConfig)) {
            synchronized (AttractantRules.class) {
                if (!configured.equals(lastConfig)) {
                    configRules = ParsedRules.parse(configured);
                    lastConfig = configured;
                }
            }
        }
        return configRules;
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
            LOGGER.warn("Unknown entity type '{}' in attractant API", raw);
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

    private static Set<String> normalizeItemIds(Collection<String> itemIds, String context) {
        Set<String> normalized = new LinkedHashSet<>();
        if (itemIds == null) {
            return normalized;
        }
        for (String raw : itemIds) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            String value = raw.trim();
            if (value.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(value.substring(1));
                if (tagId == null) {
                    LOGGER.warn("Ignoring invalid item tag '{}' in {}", value, context);
                } else {
                    normalized.add("#" + tagId);
                }
            } else {
                ResourceLocation itemId = ResourceLocation.tryParse(value);
                if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                    LOGGER.warn("Ignoring unknown item '{}' in {}", value, context);
                } else {
                    normalized.add(itemId.toString());
                }
            }
        }
        return normalized;
    }

    private static String entityTypeId(Animal animal) {
        return idOf(BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()));
    }

    private static String idOf(ResourceLocation id) {
        return id == null ? "" : id.toString();
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
            Map<ResourceLocation, Set<String>> builders = new HashMap<>();
            Set<String> all = null;
            for (String raw : entries) {
                int separator = raw.indexOf('=');
                if (separator <= 0 || separator == raw.length() - 1) {
                    LOGGER.warn("Ignoring invalid attractant rule '{}'. Expected entity_id=item_id.", raw);
                    continue;
                }
                String animalText = raw.substring(0, separator).trim();
                String animalKey = ALL_ANIMALS.equals(animalText) ? ALL_ANIMALS : normalizeAnimalId(animalText);
                if (animalKey == null) {
                    continue;
                }
                Set<String> foods;
                if (ALL_ANIMALS.equals(animalKey)) {
                    if (all == null) {
                        all = new LinkedHashSet<>();
                    }
                    foods = all;
                } else {
                    ResourceLocation id = ResourceLocation.tryParse(animalKey);
                    foods = builders.computeIfAbsent(id, ignored -> new LinkedHashSet<>());
                }
                for (String food : raw.substring(separator + 1).split("\\|")) {
                    foods.addAll(normalizeItemIds(List.of(food.trim()), raw));
                }
            }

            Map<ResourceLocation, FoodRule> parsed = new HashMap<>();
            builders.forEach((id, values) -> parsed.put(id, FoodRule.parse(values)));
            return new ParsedRules(parsed, all == null ? null : FoodRule.parse(all));
        }
    }

    public static final class FoodRule {
        private final Set<Item> items;
        private final Set<net.minecraft.tags.TagKey<Item>> tags;
        private final List<String> identifiers;

        private FoodRule(Set<Item> items, Set<net.minecraft.tags.TagKey<Item>> tags, List<String> identifiers) {
            this.items = Set.copyOf(items);
            this.tags = Set.copyOf(tags);
            this.identifiers = List.copyOf(identifiers);
        }

        private static FoodRule parse(Collection<String> ids) {
            Set<Item> items = new java.util.HashSet<>();
            Set<net.minecraft.tags.TagKey<Item>> tags = new java.util.HashSet<>();
            List<String> identifiers = new ArrayList<>();
            for (String value : ids) {
                if (value.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(value.substring(1));
                    if (tagId != null) {
                        tags.add(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId));
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
            for (net.minecraft.tags.TagKey<Item> tag : tags) {
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
}
