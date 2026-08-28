package com.what_animals_eat;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

final class AttractantGoal extends Goal {
    private static final TargetingConditions TARGETING = TargetingConditions.forNonCombat()
            .range(10.0D)
            .ignoreLineOfSight();

    private final Animal animal;
    private final double speedModifier;
    private final TargetingConditions targetingConditions;
    private Player player;
    private int calmDown;

    AttractantGoal(Animal animal, double speedModifier) {
        this.animal = animal;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.targetingConditions = TARGETING.copy().selector(this::shouldFollow);
    }

    @Override
    public boolean canUse() {
        if (calmDown > 0) {
            calmDown--;
            return false;
        }
        player = animal.level().getNearestPlayer(targetingConditions, animal);
        return player != null;
    }

    private boolean shouldFollow(LivingEntity entity) {
        AttractantRules.FoodRule rule = AttractantRules.forAnimal(animal);
        return rule != null
                && (rule.matches(entity.getMainHandItem()) || rule.matches(entity.getOffhandItem()));
    }

    @Override
    public boolean canContinueToUse() {
        return player != null && canUse();
    }

    @Override
    public void start() {
        calmDown = 0;
    }

    @Override
    public void stop() {
        player = null;
        animal.getNavigation().stop();
        calmDown = reducedTickDelay(100);
    }

    @Override
    public void tick() {
        animal.getLookControl().setLookAt(player, animal.getMaxHeadYRot() + 20.0F, animal.getMaxHeadXRot());
        if (animal.distanceToSqr(player) < 6.25D) {
            animal.getNavigation().stop();
        } else {
            animal.getNavigation().moveTo(player, speedModifier);
        }
    }
}
