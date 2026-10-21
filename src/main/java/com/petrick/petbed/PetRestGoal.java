package com.petrick.petbed;

import com.petrick.petbed.block.PetBedBlockEntity;
import com.petrick.petbed.block.PetBedBlock;
import com.petrick.petbed.block.PetBedArea;
import com.petrick.petbed.block.PetBedMode;
import com.petrick.petbed.block.PetLinkService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.Mob;
import com.petrick.petbed.api.PetBedPets;
import net.minecraft.nbt.CompoundTag;
import java.util.EnumSet;

/** Server-side, low-speed return/rest goal. It never loads chunks and never teleports a pet. */
public final class PetRestGoal extends Goal {
    private final Mob pet;
    private BlockPos bedPos;
    private int retryDelay;
    private boolean arrived;

    public PetRestGoal(Mob pet) {
        this.pet = pet;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override public boolean canUse() {
        if (!(pet.level instanceof ServerLevel level) || !pet.isAlive() || !PetBedPets.isTamedPet(pet)) return false;
        if (retryDelay > 0) { retryDelay--; return false; }
        bedPos = readBedPos(level);
        if (bedPos == null) return false;
        boolean night = isNight(level);
        if (night && PetLinkService.getMode(pet) == PetBedMode.SIT && PetBedPets.isOrderedToSit(pet)) PetBedPets.setOrderedToSit(pet, false);
        if (!night && PetLinkService.getMode(pet) == PetBedMode.SIT) return false;
        return night || !insideArea();
    }

    @Override public boolean canContinueToUse() {
        if (!(pet.level instanceof ServerLevel level) || !pet.isAlive() || bedPos == null) return false;
        if (!PetLinkService.isLinkValid(pet, level, bedPos)) return false;
        boolean night = isNight(level);
        return night || (PetLinkService.getMode(pet) != PetBedMode.SIT && !insideArea());
    }

    @Override public void start() {
        arrived = false;
        if (atBed()) rest(); else if (!navigate()) retryDelay = 100;
    }

    @Override public void tick() {
        if (!(pet.level instanceof ServerLevel level) || bedPos == null) return;
        if (retryDelay > 0) { retryDelay--; return; }
        boolean night = isNight(level);
        if (night && PetLinkService.getMode(pet) == PetBedMode.SIT && !atBed() && PetBedPets.isOrderedToSit(pet)) PetBedPets.setOrderedToSit(pet, false);
        if (atBed()) { rest(); return; }
        pet.getPersistentData().putBoolean("PetBedResting", false);
        if (pet.getNavigation().isDone()) {
            if (night || !insideArea()) {
                if (!navigate()) retryDelay = 100;
            }
        }
    }

    @Override public void stop() {
        pet.getPersistentData().remove("PetBedResting");
        if (pet.getNavigation().isInProgress()) pet.getNavigation().stop();
        bedPos = null;
        arrived = false;
    }

    private boolean navigate() {
        if (!(pet.level instanceof ServerLevel level) || bedPos == null || !level.hasChunkAt(bedPos) || !PetLinkService.isLinkValid(pet, level, bedPos)) return false;
        // Speed modifier 1.0 uses the species' normal walking pace.
        return pet.getNavigation().moveTo(bedPos.getX() + .5, bedPos.getY() + 4.0 / 16.0, bedPos.getZ() + .5, 1.0);
    }

    private boolean atBed() {
        if (bedPos == null) return false;
        double dx = pet.getX() - (bedPos.getX() + .5), dz = pet.getZ() - (bedPos.getZ() + .5);
        return dx * dx + dz * dz < .42 && Math.abs(pet.getY() - (bedPos.getY() + 4.0 / 16.0)) < 1.1;
    }

    private void rest() {
        if (!arrived) {
            pet.getNavigation().stop();
            // One arrival alignment and facing adjustment; no recurring position correction.
            pet.setPos(bedPos.getX() + .5, bedPos.getY() + 4.0 / 16.0, bedPos.getZ() + .5);
            if (pet.level.getBlockState(bedPos).hasProperty(PetBedBlock.FACING)) {
                float yaw = pet.level.getBlockState(bedPos).getValue(PetBedBlock.FACING).toYRot();
                pet.setYRot(yaw); pet.yRotO = yaw; pet.yHeadRot = yaw; pet.yBodyRot = yaw;
            }
            arrived = true;
        }
        pet.setDeltaMovement(0, 0, 0);
        pet.getPersistentData().putBoolean("PetBedResting", true);
        if (PetLinkService.getMode(pet) == PetBedMode.SIT) PetBedPets.setOrderedToSit(pet, true);
    }

    private boolean insideArea() { return bedPos != null && PetBedArea.contains(bedPos, pet.getX(), pet.getZ(), PetLinkService.getAreaSize(pet)); }
    private static boolean isNight(ServerLevel level) { long time = level.getDayTime() % 24000L; return time >= 13000L && time < 23000L; }

    private BlockPos readBedPos(ServerLevel level) {
        CompoundTag data = pet.getPersistentData();
        if (!data.contains(PetLinkService.DATA_KEY)) return null;
        CompoundTag marker = data.getCompound(PetLinkService.DATA_KEY);
        if (marker.hasUUID("Owner") && !marker.getUUID("Owner").equals(PetBedPets.getOwnerId(pet))) {
            BlockPos oldPos = BlockPos.of(marker.getLong("Pos"));
            if (level.hasChunkAt(oldPos) && level.getBlockEntity(oldPos) instanceof PetBedBlockEntity oldBed && pet.getUUID().equals(oldBed.getPetId())) oldBed.setLink(null, null);
            PetLinkService.removeLinkMarker(pet);
            return null;
        }
        if (!marker.getString("Dimension").equals(level.dimension().location().toString())) {
            PetLinkService.removeLinkMarker(pet);
            return null;
        }
        BlockPos pos = BlockPos.of(marker.getLong("Pos"));
        if (level.hasChunkAt(pos) && !PetLinkService.isLinkValid(pet, level, pos)) {
            PetLinkService.removeLinkMarker(pet);
            return null;
        }
        return pos;
    }
}
