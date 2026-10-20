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
    private PetLinkService() {}

    public static boolean link(Player player, ServerLevel level, PetBedBlockEntity bed, Mob pet) {
        if (!pet.isAlive() || !PetBedPets.isTamedPet(pet) || !player.getUUID().equals(PetBedPets.getOwnerId(pet))) return false;
        if (!pet.level.dimension().equals(level.dimension()) || pet.distanceToSqr(player) > 64.0D || bed.getPetId() != null) return false;
        CompoundTag data = pet.getPersistentData();
        if (data.contains(DATA_KEY)) {
            CompoundTag old = data.getCompound(DATA_KEY);
            if (!old.getString("Dimension").equals(level.dimension().location().toString())) return false;
            BlockPos oldPos = BlockPos.of(old.getLong("Pos"));
            if (!level.hasChunkAt(oldPos)) return false;
            BlockEntity oldEntity = level.getBlockEntity(oldPos);
            if (oldEntity instanceof PetBedBlockEntity oldBed && pet.getUUID().equals(oldBed.getPetId())) return false;
            data.remove(DATA_KEY); // stale marker left by a removed bed
        }
        bed.setLink(pet.getUUID(), player.getUUID());
        CompoundTag marker = new CompoundTag();
        marker.putLong("Pos", bed.getBlockPos().asLong());
        marker.putString("Dimension", level.dimension().location().toString());
        marker.putUUID("Owner", player.getUUID());
        data.put(DATA_KEY, marker);
        return true;
    }

    public static boolean unlink(Player player, ServerLevel level, PetBedBlockEntity bed) {
        if (bed.getPetId() == null) return false;
        Entity entity = level.getEntity(bed.getPetId());
        boolean originalOwner = bed.getOwnerId() != null && bed.getOwnerId().equals(player.getUUID());
        boolean currentPetOwner = entity != null && player.getUUID().equals(PetBedPets.getOwnerId(entity));
        if (entity == null ? !originalOwner : !currentPetOwner) return false;
        if (entity != null) entity.getPersistentData().remove(DATA_KEY);
        bed.setLink(null, null);
        return true;
    }

    public static void clearMarker(Entity entity, BlockPos pos, ServerLevel level) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(DATA_KEY)) return;
        CompoundTag marker = data.getCompound(DATA_KEY);
        if (marker.getLong("Pos") == pos.asLong() && marker.getString("Dimension").equals(level.dimension().location().toString())) data.remove(DATA_KEY);
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
