package com.petrick.petbed.client;

import com.petrick.petbed.PetBed;
import com.petrick.petbed.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterMenuScreensEvent;

@Mod.EventBusSubscriber(modid = PetBed.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {
    private ClientInit() {}
    @SubscribeEvent public static void onRegisterScreens(RegisterMenuScreensEvent event) { event.register(ModMenus.PET_BED.get(), PetBedScreen::new); }
}
