package com.what_animals_eat.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class AfterAnimalFedEventJS extends EventJS {
    private final Player player;
    private final Animal animal;
    private final ItemStack item;

    public AfterAnimalFedEventJS(Player player, Animal animal, ItemStack item) {
        this.player = player;
        this.animal = animal;
        this.item = item.copy();
    }

    public Player getPlayer() {
        return player;
    }

    public Player getFeeder() {
        return player;
    }

    public Animal getAnimal() {
        return animal;
    }

    public Animal getFedEntity() {
        return animal;
    }

    public ItemStack getItem() {
        return item;
    }
}
