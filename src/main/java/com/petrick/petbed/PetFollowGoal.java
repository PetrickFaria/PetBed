package com.petrick.petbed;

import com.petrick.petbed.block.PetBedArea;
import com.petrick.petbed.block.PetBedMode;
import com.petrick.petbed.block.PetLinkService;
import com.petrick.petbed.api.PetBedPets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import java.util.EnumSet;
import java.util.UUID;

/** Owner follow for PetBed's FOLLOW mode; uses normal navigation and never teleports. */
public final class PetFollowGoal extends Goal {
    private final Mob pet;
    private Player owner;
    private int recalcDelay;

    public PetFollowGoal(Mob pet) {
        this.pet = pet;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        owner = findOwner();
        return owner != null && PetLinkService.hasLinkMarker(pet)
                && PetLinkService.getMode(pet) == PetBedMode.FOLLOW
                && !isNight() && !PetBedPets.isOrderedToSit(pet)
                && targetDistanceSquared() > 9.0D;
    }

    @Override public boolean canContinueToUse() {
        if (owner == null || !owner.isAlive() || !PetLinkService.hasLinkMarker(pet)
                || PetLinkService.getMode(pet) != PetBedMode.FOLLOW || isNight()
                || PetBedPets.isOrderedToSit(pet)) return false;
        return targetDistanceSquared() > 6.25D;
    }

    @Override public void start() { recalcDelay = 0; }

    @Override public void tick() {
        owner = findOwner();
        if (owner == null) return;
        pet.getLookControl().setLookAt(owner, 10.0F, pet.getMaxHeadXRot());
        if (recalcDelay-- > 0) return;
        recalcDelay = 10;
        double x = PetBedArea.clampX(PetLinkService.getBedPos(pet), owner.getX(), PetLinkService.getAreaSize(pet));
        double z = PetBedArea.clampZ(PetLinkService.getBedPos(pet), owner.getZ(), PetLinkService.getAreaSize(pet));
        if (targetDistanceSquared() > 9.0D) pet.getNavigation().moveTo(x, owner.getY(), z, 1.0D);
        else pet.getNavigation().stop();
    }

    @Override public void stop() {
        owner = null;
        if (pet.getNavigation().isInProgress()) pet.getNavigation().stop();
    }

    private Player findOwner() {
        if (!(pet.level instanceof ServerLevel level)) return null;
        UUID ownerId = PetBedPets.getOwnerId(pet);
        return ownerId == null ? null : level.getPlayerByUUID(ownerId);
    }

    private double targetDistanceSquared() {
        if (owner == null) return 0;
        var bed = PetLinkService.getBedPos(pet);
        int size = PetLinkService.getAreaSize(pet);
        double x = PetBedArea.clampX(bed, owner.getX(), size);
        double z = PetBedArea.clampZ(bed, owner.getZ(), size);
        double dx = pet.getX() - x, dz = pet.getZ() - z;
        return dx * dx + dz * dz;
    }

    private boolean isNight() {
        if (!(pet.level instanceof ServerLevel level)) return false;
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time < 23000L;
    }
}
