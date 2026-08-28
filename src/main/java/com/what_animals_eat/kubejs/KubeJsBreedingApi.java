package com.what_animals_eat.kubejs;

import java.util.ArrayList;
import java.util.List;

import com.what_animals_eat.BreedingFoodRules;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.util.ListJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;

public final class KubeJsBreedingApi {
    public static final KubeJsBreedingApi INSTANCE = new KubeJsBreedingApi();

    private KubeJsBreedingApi() {
    }

    public boolean set(Object animal, Object foods) {
        List<String> ids = parseFoods(foods);
        return animal instanceof Animal fedEntity
                ? BreedingFoodRules.setRuntimeRule(fedEntity, ids)
                : BreedingFoodRules.setRuntimeRule(toAnimalId(animal), ids);
    }

    public boolean setBreedingFoods(Object animal, Object foods) {
        return set(animal, foods);
    }

    public boolean add(Object animal, Object food) {
        List<String> ids = parseFoods(food);
        if (ids.size() != 1) {
            return false;
        }
        return animal instanceof Animal fedEntity
                ? BreedingFoodRules.addRuntimeFood(fedEntity, ids.get(0))
                : BreedingFoodRules.addRuntimeFood(toAnimalId(animal), ids.get(0));
    }

    public boolean addBreedingFood(Object animal, Object food) {
        return add(animal, food);
    }

    public boolean remove(Object animal, Object food) {
        List<String> ids = parseFoods(food);
        if (ids.size() != 1) {
            return false;
        }
        return animal instanceof Animal fedEntity
                ? BreedingFoodRules.removeRuntimeFood(fedEntity, ids.get(0))
                : BreedingFoodRules.removeRuntimeFood(toAnimalId(animal), ids.get(0));
    }

    public boolean removeBreedingFood(Object animal, Object food) {
        return remove(animal, food);
    }

    public boolean reset(Object animal) {
        return animal instanceof Animal fedEntity
                ? BreedingFoodRules.clearRuntimeRule(fedEntity)
                : BreedingFoodRules.clearRuntimeRule(toAnimalId(animal));
    }

    public boolean resetBreedingFoods(Object animal) {
        return reset(animal);
    }

    public List<String> get(Object animal) {
        return animal instanceof Animal fedEntity
                ? BreedingFoodRules.getEffectiveFoods(fedEntity)
                : BreedingFoodRules.getEffectiveFoods(toAnimalId(animal));
    }

    public List<String> getBreedingFoods(Object animal) {
        return get(animal);
    }

    private static String toAnimalId(Object animal) {
        if (animal instanceof Entity entity) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return id == null ? null : id.toString();
        }
        return animal == null ? null : animal.toString();
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
