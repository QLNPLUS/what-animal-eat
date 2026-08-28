package com.what_animals_eat;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.animal.Animal;

final class EntityRuleData {
    static final String ROOT_KEY = "what_animals_eat";
    static final String FOOD_KEY = "food";
    static final String ATTRACTANT_KEY = "attractant";

    private EntityRuleData() {
    }

    static Set<String> get(Animal animal, String key) {
        CompoundTag data = animal.getPersistentData();
        if (!data.contains(ROOT_KEY, 10)) {
            return null;
        }

        CompoundTag rules = data.getCompound(ROOT_KEY);
        if (!rules.contains(key, 9)) {
            return null;
        }

        ListTag values = rules.getList(key, 8);
        Set<String> result = new LinkedHashSet<>();
        for (int index = 0; index < values.size(); index++) {
            result.add(values.getString(index));
        }
        return Set.copyOf(result);
    }

    static void put(Animal animal, String key, Set<String> values) {
        CompoundTag data = animal.getPersistentData();
        CompoundTag rules = data.getCompound(ROOT_KEY);
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        rules.put(key, list);
        data.put(ROOT_KEY, rules);
    }

    static boolean remove(Animal animal, String key) {
        CompoundTag data = animal.getPersistentData();
        if (!data.contains(ROOT_KEY, 10)) {
            return false;
        }

        CompoundTag rules = data.getCompound(ROOT_KEY);
        if (!rules.contains(key)) {
            return false;
        }
        rules.remove(key);
        if (rules.isEmpty()) {
            data.remove(ROOT_KEY);
        } else {
            data.put(ROOT_KEY, rules);
        }
        return true;
    }
}
