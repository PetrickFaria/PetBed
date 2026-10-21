package com.petrick.petbed.block;

import com.petrick.petbed.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class PetBedBlockEntity extends BlockEntity {
    @Nullable private UUID petId;
    @Nullable private UUID ownerId;
    private int areaSize = PetBedArea.DEFAULT_SIZE;
    private PetBedMode mode = PetBedMode.FREE;

    public PetBedBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PET_BED.get(), pos, state); }
    @Nullable public UUID getPetId() { return petId; }
    @Nullable public UUID getOwnerId() { return ownerId; }
    public int getAreaSize() { return PetBedArea.isValid(areaSize) ? areaSize : PetBedArea.DEFAULT_SIZE; }
    public PetBedMode getMode() { return mode == null ? PetBedMode.FREE : mode; }
    public void setAreaSize(int size) { if (PetBedArea.isValid(size)) { areaSize = size; sync(); } }
    public void setMode(PetBedMode mode) { this.mode = mode == null ? PetBedMode.FREE : mode; sync(); }
    public void setLink(@Nullable UUID pet, @Nullable UUID owner) {
        petId = pet; ownerId = owner;
        if (pet == null) mode = PetBedMode.FREE;
        sync();
    }

    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (petId != null) tag.putUUID("PetId", petId);
        if (ownerId != null) tag.putUUID("OwnerId", ownerId);
        tag.putInt("AreaSize", getAreaSize());
        tag.putString("PetMode", getMode().id());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        petId = tag.hasUUID("PetId") ? tag.getUUID("PetId") : null;
        ownerId = tag.hasUUID("OwnerId") ? tag.getUUID("OwnerId") : null;
        areaSize = tag.contains("AreaSize") ? tag.getInt("AreaSize") : PetBedArea.DEFAULT_SIZE;
        if (!PetBedArea.isValid(areaSize)) areaSize = PetBedArea.DEFAULT_SIZE;
        mode = PetBedMode.fromId(tag.getString("PetMode"));
    }
}
