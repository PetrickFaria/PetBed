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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.network.NetworkHooks;
import com.petrick.petbed.menu.PetControlMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleMenuProvider;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import com.petrick.petbed.block.PetBedMode;

public final class PetBehaviorEvents {
    @SubscribeEvent public void onPetInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getTarget() instanceof Mob pet) || !PetLinkService.hasLinkMarker(pet)) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.getUUID().equals(PetBedPets.getOwnerId(pet))) return;
        event.setCanceled(true);
        var bed = PetLinkService.getBedPos(pet);
        int area=PetLinkService.getAreaSize(pet);
        String mode=PetLinkService.getMode(pet).id();
        String status=com.petrick.petbed.menu.PetBedMenu.status(pet,pet.level,bed);
        String distance=com.petrick.petbed.menu.PetBedMenu.distance(pet,bed);
        NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inv,p)->new PetControlMenu(id,pet.getId(),pet.getUUID(),pet.getDisplayName().getString(),pet.getType().getDescription().getString(),com.petrick.petbed.menu.PetBedMenu.posture(pet),mode,status,distance),Component.literal("PetBed")),buf->{
            buf.writeVarInt(pet.getId()); buf.writeUUID(pet.getUUID()); buf.writeUtf(pet.getDisplayName().getString(),128); buf.writeUtf(pet.getType().getDescription().getString(),128);
            buf.writeUtf(com.petrick.petbed.menu.PetBedMenu.posture(pet),32); buf.writeUtf(mode,32); buf.writeUtf(status,64); buf.writeUtf(distance,64);
        });
    }
    @SubscribeEvent public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob pet) || !PetBedPets.supports(pet)) return;
        if (!hasGoal(pet, PetRestGoal.class)) pet.goalSelector.addGoal(0, new PetRestGoal(pet));
        if (!hasGoal(pet, PetFollowGoal.class)) pet.goalSelector.addGoal(2, new PetFollowGoal(pet));
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

    private static boolean hasGoal(Mob pet, Class<?> goalType) {
        return pet.goalSelector.getAvailableGoals().stream().anyMatch(wrapped -> goalType.isInstance(wrapped.getGoal()));
    }
}
