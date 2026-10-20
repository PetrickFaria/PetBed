package com.petrick.petbed.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Small integration point for pets added by other mods. Register during mod initialization. */
public final class PetBedPets {
    private static final Map<Class<? extends Mob>, PetOwnershipAdapter<?>> ADAPTERS = new LinkedHashMap<>();
    static {
        register(TamableAnimal.class, new PetOwnershipAdapter<TamableAnimal>() {
            @Override public boolean isTamed(TamableAnimal pet) { return pet.isTame(); }
            @Override public UUID getOwnerId(TamableAnimal pet) { return pet.getOwnerUUID(); }
            @Override public boolean isOrderedToSit(TamableAnimal pet) { return pet.isOrderedToSit(); }
        });
    }
    private PetBedPets() {}
    public static synchronized <T extends Mob> void register(Class<T> type, PetOwnershipAdapter<T> adapter) { ADAPTERS.put(type, adapter); }

    @SuppressWarnings("unchecked")
    private static synchronized PetOwnershipAdapter<Mob> adapter(Entity entity) {
        if (!(entity instanceof Mob mob)) return null;
        Class<?> best=null; PetOwnershipAdapter<Mob> selected=null;
        for (var entry : ADAPTERS.entrySet()) if (entry.getKey().isInstance(mob) && (best==null || best.isAssignableFrom(entry.getKey()))) {
            best=entry.getKey(); selected=(PetOwnershipAdapter<Mob>)entry.getValue();
        }
        return selected;
    }
    public static boolean supports(Entity entity) { return adapter(entity) != null; }
    public static boolean isTamedPet(Entity entity) { PetOwnershipAdapter<Mob> a=adapter(entity); return a!=null && a.isTamed((Mob)entity) && a.getOwnerId((Mob)entity)!=null; }
    public static UUID getOwnerId(Entity entity) { PetOwnershipAdapter<Mob> a=adapter(entity); return a==null?null:a.getOwnerId((Mob)entity); }
    public static boolean isOrderedToSit(Entity entity) { PetOwnershipAdapter<Mob> a=adapter(entity); return a!=null && a.isOrderedToSit((Mob)entity); }
}
