package com.petrick.petbed.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import com.petrick.petbed.api.PetBedPets;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public final class PetLinkService {
    public static final String DATA_KEY = "PetBedLink";
    private static final String WAS_ORDERED_TO_SIT_KEY = "WasOrderedToSit";
    private PetLinkService() {}

    public static boolean link(Player player, ServerLevel level, PetBedBlockEntity bed, Mob pet) {
        if (!pet.isAlive() || !PetBedPets.isTamedPet(pet) || !player.getUUID().equals(PetBedPets.getOwnerId(pet))) return false;
        if (!pet.level.dimension().equals(level.dimension()) || bed.getPetId() != null) return false;
        CompoundTag data = pet.getPersistentData();
        if (data.contains(DATA_KEY)) {
            CompoundTag old = data.getCompound(DATA_KEY);
            if (!old.getString("Dimension").equals(level.dimension().location().toString())) return false;
            BlockPos oldPos = BlockPos.of(old.getLong("Pos"));
            if (!level.hasChunkAt(oldPos)) return false;
            BlockEntity oldEntity = level.getBlockEntity(oldPos);
            if (oldEntity instanceof PetBedBlockEntity oldBed && pet.getUUID().equals(oldBed.getPetId())) return false;
            removeLinkMarker(pet); // stale marker left by a removed bed
        }
        boolean wasOrderedToSit = PetBedPets.isOrderedToSit(pet);
        PetBedMode initialMode = PetBedMode.FREE;
        bed.setLink(pet.getUUID(), player.getUUID());
        bed.setMode(initialMode);
        CompoundTag marker = new CompoundTag();
        marker.putLong("Pos", bed.getBlockPos().asLong());
        marker.putString("Dimension", level.dimension().location().toString());
        marker.putUUID("Owner", player.getUUID());
        marker.putBoolean(WAS_ORDERED_TO_SIT_KEY, wasOrderedToSit);
        marker.putString("Mode", initialMode.id());
        marker.putInt("AreaSize", bed.getAreaSize());
        data.put(DATA_KEY, marker);
        // Release the Vanilla sit order while linked, but restore it from the snapshot on unlink.
        PetBedPets.setOrderedToSit(pet, false);
        return true;
    }

    public static boolean setMode(Player player, ServerLevel level, Entity entity, PetBedMode mode) {
        if (!(entity instanceof Mob pet) || mode == null || !isOwnedLinkedPet(player, level, pet)) return false;
        CompoundTag marker = pet.getPersistentData().getCompound(DATA_KEY);
        marker.putString("Mode", mode.id());
        pet.getPersistentData().put(DATA_KEY, marker);
        if (mode == PetBedMode.SIT) PetBedPets.setOrderedToSit(pet, true);
        else PetBedPets.setOrderedToSit(pet, false);
        BlockPos pos = BlockPos.of(marker.getLong("Pos"));
        if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof PetBedBlockEntity bed && pet.getUUID().equals(bed.getPetId())) bed.setMode(mode);
        pet.getNavigation().stop();
        pet.getPersistentData().putBoolean("PetBedResting", false);
        return true;
    }

    public static boolean setAreaSize(Player player, ServerLevel level, PetBedBlockEntity bed, int size) {
        if (!PetBedArea.isValid(size) || bed.getPetId() == null || !player.getUUID().equals(bed.getOwnerId())) return false;
        bed.setAreaSize(size);
        Entity entity = level.getEntity(bed.getPetId());
        if (entity != null && entity.getPersistentData().contains(DATA_KEY)) {
            CompoundTag marker = entity.getPersistentData().getCompound(DATA_KEY);
            if (marker.getLong("Pos") == bed.getBlockPos().asLong() && marker.getString("Dimension").equals(level.dimension().location().toString())) {
                marker.putInt("AreaSize", size);
                entity.getPersistentData().put(DATA_KEY, marker);
            }
        }
        return true;
    }

    public static PetBedMode getMode(Entity entity) {
        return entity.getPersistentData().contains(DATA_KEY) ? PetBedMode.fromId(entity.getPersistentData().getCompound(DATA_KEY).getString("Mode")) : PetBedMode.FREE;
    }

    public static int getAreaSize(Entity entity) {
        if (!entity.getPersistentData().contains(DATA_KEY)) return PetBedArea.DEFAULT_SIZE;
        int size = entity.getPersistentData().getCompound(DATA_KEY).getInt("AreaSize");
        return PetBedArea.isValid(size) ? size : PetBedArea.DEFAULT_SIZE;
    }

    public static BlockPos getBedPos(Entity entity) {
        return entity.getPersistentData().contains(DATA_KEY)
                ? BlockPos.of(entity.getPersistentData().getCompound(DATA_KEY).getLong("Pos")) : entity.blockPosition();
    }

    private static boolean isOwnedLinkedPet(Player player, ServerLevel level, Mob pet) {
        if (!pet.isAlive() || !PetBedPets.isTamedPet(pet) || !player.getUUID().equals(PetBedPets.getOwnerId(pet)) || pet.level != level || !hasLinkMarker(pet)) return false;
        CompoundTag marker = pet.getPersistentData().getCompound(DATA_KEY);
        BlockPos pos = BlockPos.of(marker.getLong("Pos"));
        return isLinkValid(pet, level, pos);
    }

    public static boolean unlink(Player player, ServerLevel level, PetBedBlockEntity bed) {
        if (bed.getPetId() == null) return false;
        Entity entity = level.getEntity(bed.getPetId());
        boolean originalOwner = bed.getOwnerId() != null && bed.getOwnerId().equals(player.getUUID());
        boolean currentPetOwner = entity != null && player.getUUID().equals(PetBedPets.getOwnerId(entity));
        if (entity == null ? !originalOwner : !currentPetOwner) return false;
        if (entity != null) removeLinkMarker(entity);
        bed.setLink(null, null);
        return true;
    }

    public static void clearMarker(Entity entity, BlockPos pos, ServerLevel level) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(DATA_KEY)) return;
        CompoundTag marker = data.getCompound(DATA_KEY);
        if (marker.getLong("Pos") == pos.asLong() && marker.getString("Dimension").equals(level.dimension().location().toString())) removeLinkMarker(entity);
    }

    /** Lightweight per-pet guard: block/chunk resolution stays in the return goal, not this tick hook. */
    public static boolean hasLinkMarker(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(DATA_KEY) || !PetBedPets.isTamedPet(entity)) return false;
        CompoundTag marker = data.getCompound(DATA_KEY);
        if (marker.hasUUID("Owner") && !marker.getUUID("Owner").equals(PetBedPets.getOwnerId(entity))) return false;
        if (!(entity.level instanceof ServerLevel level) || !marker.getString("Dimension").equals(level.dimension().location().toString())) return false;
        return true;
    }

    /** Remove a link and restore the original sit order once, without replacing the entity or its AI. */
    public static void removeLinkMarker(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(DATA_KEY)) return;
        CompoundTag marker = data.getCompound(DATA_KEY);
        boolean restoreSitting = marker.getBoolean(WAS_ORDERED_TO_SIT_KEY);
        data.remove(DATA_KEY);
        // Restore the captured Vanilla sit order even if the linked mode changed it.
        PetBedPets.setOrderedToSit(entity, restoreSitting);
    }

    public static boolean isLinkValid(Entity entity, ServerLevel level, BlockPos pos) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(DATA_KEY)) return false;
        CompoundTag marker = data.getCompound(DATA_KEY);
        if (!marker.getString("Dimension").equals(level.dimension().location().toString()) || marker.getLong("Pos") != pos.asLong()) return false;
        if (!level.hasChunkAt(pos)) return true;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof PetBedBlockEntity bed && entity.getUUID().equals(bed.getPetId());
    }
}
