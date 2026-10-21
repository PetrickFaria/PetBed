package com.petrick.petbed;

import com.petrick.petbed.block.PetBedBlockEntity;
import com.petrick.petbed.block.PetLinkService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import com.petrick.petbed.api.PetBedPets;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PetBehaviorEvents {
    @SubscribeEvent public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob pet) || !PetBedPets.supports(pet)) return;
        // Goals are initialized once when this entity instance enters the server world.
        pet.goalSelector.addGoal(1, new PetRestGoal(pet));
    }

    @SubscribeEvent public void onPetDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity.level instanceof ServerLevel level) || !entity.getPersistentData().contains(PetLinkService.DATA_KEY)) return;
        var marker = entity.getPersistentData().getCompound(PetLinkService.DATA_KEY);
        BlockPos pos = BlockPos.of(marker.getLong("Pos"));
        if (marker.getString("Dimension").equals(level.dimension().location().toString()) && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof PetBedBlockEntity bed && entity.getUUID().equals(bed.getPetId())) {
            bed.setLink(null, null);
        }
        // Death ends the relationship; do not restore a sit command on an entity being removed.
        entity.getPersistentData().remove(PetLinkService.DATA_KEY);
    }
}
