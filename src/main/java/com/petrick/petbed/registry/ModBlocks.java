package com.petrick.petbed.registry;

import com.petrick.petbed.PetBed;
import com.petrick.petbed.block.PetBedBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PetBed.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PetBed.MOD_ID);
    public static final RegistryObject<Block> PET_BED = BLOCKS.register("pet_bed", () -> new PetBedBlock(Block.Properties.of(Material.WOOD).strength(1.2F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Item> PET_BED_ITEM = ITEMS.register("pet_bed", () -> new BlockItem(PET_BED.get(), new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
}
