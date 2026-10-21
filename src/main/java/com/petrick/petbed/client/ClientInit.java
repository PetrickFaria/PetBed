package com.petrick.petbed.client;

import com.petrick.petbed.PetBed;
import com.petrick.petbed.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientInit {
    private ClientInit() {}
    public static void onClientSetup(FMLClientSetupEvent event) { event.enqueueWork(() -> { MenuScreens.register(ModMenus.PET_BED.get(), PetBedScreen::new); MenuScreens.register(ModMenus.PET_CONTROL.get(), PetControlScreen::new); }); }
}
