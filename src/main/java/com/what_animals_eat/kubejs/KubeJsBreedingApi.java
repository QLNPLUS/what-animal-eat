package com.what_animals_eat.kubejs;

import java.util.ArrayList;
import java.util.List;

import com.what_animals_eat.BreedingFoodRules;
import dev.latvian.mods.kubejs.plugin.builtin.wrapper.ItemWrapper;
import dev.latvian.mods.kubejs.util.ListJS;
import dev.latvian.mods.rhino.Context;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class KubeJsBreedingApi {
    public static final KubeJsBreedingApi INSTANCE = new KubeJsBreedingApi();

    private KubeJsBreedingApi() {
    }

    public boolean set(Context cx, String animalId, Object foods) {
        return BreedingFoodRules.setRuntimeRule(animalId, parseFoods(cx, foods));
    }

    public boolean setBreedingFoods(Context cx, String animalId, Object foods) {
        return set(cx, animalId, foods);
    }

    public boolean add(Context cx, String animalId, Object food) {
        List<String> ids = parseFoods(cx, food);
        return ids.size() == 1 && BreedingFoodRules.addRuntimeFood(animalId, ids.get(0));
    }

    public boolean addBreedingFood(Context cx, String animalId, Object food) {
        return add(cx, animalId, food);
    }

    public boolean remove(Context cx, String animalId, Object food) {
        List<String> ids = parseFoods(cx, food);
        return ids.size() == 1 && BreedingFoodRules.removeRuntimeFood(animalId, ids.get(0));
    }

    public boolean removeBreedingFood(Context cx, String animalId, Object food) {
        return remove(cx, animalId, food);
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

    private static List<String> parseFoods(Context cx, Object value) {
        List<String> ids = new ArrayList<>();
        for (Object entry : ListJS.orSelf(value)) {
            if (entry instanceof String string) {
                ids.add(string);
                continue;
            }
            ItemStack stack = ItemWrapper.wrap(cx, entry);
            if (!stack.isEmpty()) {
                ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
        }
        return ids;
    }
}
