package com.petrick.petbed.api;

import net.minecraft.world.entity.Mob;
import java.util.UUID;

/** Register an adapter for a modded Mob whose taming/ownership system is not based on TamableAnimal. */
public interface PetOwnershipAdapter<T extends Mob> {
    boolean isTamed(T pet);
    UUID getOwnerId(T pet);
    /** Preserve an owner's explicit sit command when the pet type exposes one. */
    default boolean isOrderedToSit(T pet) { return false; }
}
