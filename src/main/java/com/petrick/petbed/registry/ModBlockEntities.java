package com.petrick.petbed.registry;

import com.petrick.petbed.PetBed;
import com.petrick.petbed.block.PetBedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, PetBed.MOD_ID);
    public static final RegistryObject<BlockEntityType<PetBedBlockEntity>> PET_BED = TYPES.register("pet_bed", () -> BlockEntityType.Builder.of(PetBedBlockEntity::new, ModBlocks.PET_BED.get()).build(null));
}
