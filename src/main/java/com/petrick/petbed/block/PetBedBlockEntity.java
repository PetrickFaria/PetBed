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

    public PetBedBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PET_BED.get(), pos, state); }
    @Nullable public UUID getPetId() { return petId; }
    @Nullable public UUID getOwnerId() { return ownerId; }
    public void setLink(@Nullable UUID pet, @Nullable UUID owner) { petId = pet; ownerId = owner; setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (petId != null) tag.putUUID("PetId", petId);
        if (ownerId != null) tag.putUUID("OwnerId", ownerId);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        petId = tag.hasUUID("PetId") ? tag.getUUID("PetId") : null;
        ownerId = tag.hasUUID("OwnerId") ? tag.getUUID("OwnerId") : null;
    }
}
