package com.what_animals_eat.kubejs;

import java.util.ArrayList;
import java.util.List;

import com.what_animals_eat.BreedingFoodRules;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.util.ListJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class KubeJsBreedingApi {
    public static final KubeJsBreedingApi INSTANCE = new KubeJsBreedingApi();

    private KubeJsBreedingApi() {
    }

    public boolean set(String animalId, Object foods) {
        return BreedingFoodRules.setRuntimeRule(animalId, parseFoods(foods));
    }

    public boolean setBreedingFoods(String animalId, Object foods) {
        return set(animalId, foods);
    }

    public boolean add(String animalId, Object food) {
        List<String> ids = parseFoods(food);
        return ids.size() == 1 && BreedingFoodRules.addRuntimeFood(animalId, ids.get(0));
    }

    public boolean addBreedingFood(String animalId, Object food) {
        return add(animalId, food);
    }

    public boolean remove(String animalId, Object food) {
        List<String> ids = parseFoods(food);
        return ids.size() == 1 && BreedingFoodRules.removeRuntimeFood(animalId, ids.get(0));
    }

    public boolean removeBreedingFood(String animalId, Object food) {
        return remove(animalId, food);
    }

    public boolean reset(String animalId) {
        return BreedingFoodRules.clearRuntimeRule(animalId);
    }

    public boolean resetBreedingFoods(String animalId) {
        return reset(animalId);
    }

    public List<String> get(String animalId) {
        return BreedingFoodRules.getEffectiveFoods(animalId);
    }

    public List<String> getBreedingFoods(String animalId) {
        return get(animalId);
    }

    private static List<String> parseFoods(Object value) {
        List<String> ids = new ArrayList<>();
        for (Object entry : ListJS.orSelf(value)) {
            if (entry instanceof String string) {
                ids.add(string);
                continue;
            }
            ItemStack stack = ItemStackJS.of(entry);
            if (!stack.isEmpty()) {
                ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
        }
        return ids;
    }
}
