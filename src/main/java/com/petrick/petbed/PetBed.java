package com.petrick.petbed;

import com.petrick.petbed.network.PetBedNetwork;
import com.petrick.petbed.registry.ModBlockEntities;
import com.petrick.petbed.registry.ModBlocks;
import com.petrick.petbed.registry.ModMenus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PetBed.MOD_ID)
public class PetBed {
    public static final String MOD_ID = "petbed";

    public PetBed() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        ModBlockEntities.TYPES.register(bus);
        ModMenus.TYPES.register(bus);
        PetBedNetwork.register();
        MinecraftForge.EVENT_BUS.register(new PetBehaviorEvents());
    }
}
