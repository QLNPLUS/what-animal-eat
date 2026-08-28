package com.what_animals_eat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.TemptGoal;

final class AttractantHandler {
    private AttractantHandler() {
    }

    static void configure(Animal animal) {
        if (animal.level().isClientSide) {
            return;
        }
        animal.goalSelector.removeAllGoals(goal -> goal instanceof TemptGoal || goal instanceof AttractantGoal);
        animal.goalSelector.addGoal(3, new AttractantGoal(animal, 1.25D));
    }

    static void configureExisting(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Animal animal) {
                    configure(animal);
                }
            }
        }
    }
}
