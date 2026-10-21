package com.petrick.petbed.registry;

import com.petrick.petbed.PetBed;
import com.petrick.petbed.menu.PetBedMenu;
import com.petrick.petbed.menu.PetControlMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, PetBed.MOD_ID);
    public static final RegistryObject<MenuType<PetBedMenu>> PET_BED = TYPES.register("pet_bed", () -> IForgeMenuType.create(PetBedMenu::new));
    public static final RegistryObject<MenuType<PetControlMenu>> PET_CONTROL = TYPES.register("pet_control", () -> IForgeMenuType.create(PetControlMenu::new));
}
