package com.petrick.petbed.menu;

import com.petrick.petbed.block.PetBedBlockEntity;
import com.petrick.petbed.block.PetLinkService;
import com.petrick.petbed.api.PetBedPets;
import com.petrick.petbed.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PetBedMenu extends AbstractContainerMenu {
    public record Candidate(UUID id, String name, String species) {}
    private final BlockPos pos;
    private final List<Candidate> candidates;
    private String petName, species, state, distance;
    private UUID petId;

    public PetBedMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
        boolean linked = buf.readBoolean();
        if (linked) { petId = buf.readUUID(); petName = buf.readUtf(128); species = buf.readUtf(128); state = buf.readUtf(64); distance = buf.readUtf(64); }
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) candidates.add(new Candidate(buf.readUUID(), buf.readUtf(128), buf.readUtf(128)));
    }

    public PetBedMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.PET_BED.get(), id);
        this.pos = pos;
        this.candidates = new ArrayList<>();
        this.petName = "Nenhum"; this.species = "—"; this.state = "Livre"; this.distance = "—";
    }

    public static void writeOpenData(FriendlyByteBuf buf, ServerPlayer player, PetBedBlockEntity bed) {
        buf.writeBlockPos(bed.getBlockPos());
        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level;
        Mob pet = bed.getPetId() == null ? null : level.getEntity(bed.getPetId()) instanceof Mob m ? m : null;
        buf.writeBoolean(bed.getPetId() != null);
        if (bed.getPetId() != null) {
            buf.writeUUID(bed.getPetId());
            buf.writeUtf(pet == null ? "Pet indisponível" : pet.getDisplayName().getString(), 128);
            buf.writeUtf(pet == null ? "—" : pet.getType().getDescription().getString(), 128);
            buf.writeUtf(status(pet, level, bed.getBlockPos()), 64);
            buf.writeUtf(distance(pet, bed.getBlockPos()), 64);
        }
        List<Mob> eligible = level.getEntitiesOfClass(Mob.class,
                new AABB(bed.getBlockPos()).inflate(8), candidate -> candidate.isAlive() && PetBedPets.isTamedPet(candidate)
                        && player.getUUID().equals(PetBedPets.getOwnerId(candidate)) && candidate.getPersistentData().getCompound(PetLinkService.DATA_KEY).isEmpty());
        buf.writeVarInt(eligible.size());
        for (Mob candidate : eligible) {
            buf.writeUUID(candidate.getUUID()); buf.writeUtf(candidate.getDisplayName().getString(), 128);
            buf.writeUtf(candidate.getType().getDescription().getString(), 128);
        }
    }

    public static String status(@Nullable Mob pet, Level level, BlockPos pos) {
        if (pet == null || !pet.isAlive()) return "Indisponível";
        if (pet.getPersistentData().getBoolean("PetBedResting")) return "Descansando";
        long time = level.getDayTime() % 24000L;
        double distance = Math.sqrt(pet.distanceToSqr(pos.getX() + .5, pos.getY(), pos.getZ() + .5));
        return time >= 13000L && time < 23000L || distance >= 90 ? "Retornando" : "Livre";
    }
    public static String distance(@Nullable Mob pet, BlockPos pos) {
        return pet == null || !pet.isAlive() ? "—" : String.format(java.util.Locale.ROOT, "%.1f blocos", Math.sqrt(pet.distanceToSqr(pos.getX()+.5, pos.getY(), pos.getZ()+.5)));
    }

    @Override public boolean stillValid(Player player) { return stillValid(ContainerLevelAccess.create(player.level, pos), player, com.petrick.petbed.registry.ModBlocks.PET_BED.get()); }
    public BlockPos getPos() { return pos; }
    public List<Candidate> getCandidates() { return candidates; }
    public boolean isLinked() { return petId != null; }
    public UUID getPetId() { return petId; }
    public String getPetName() { return petName; }
    public String getSpecies() { return species; }
    public String getState() { return state; }
    public String getDistance() { return distance; }
    public void updateStatus(@Nullable UUID id, String name, String kind, String status, String range) { petId = id; petName = name; species = kind; state = status; distance = range; }
    @Override public boolean canTakeItemForPickAll(net.minecraft.world.item.ItemStack stack, net.minecraft.world.inventory.Slot slot) { return false; }
    @Override public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) { return net.minecraft.world.item.ItemStack.EMPTY; }

    public static class Provider implements MenuProvider {
        private final PetBedBlockEntity bed;
        public Provider(PetBedBlockEntity bed) { this.bed = bed; }
        @Override public net.minecraft.network.chat.Component getDisplayName() { return net.minecraft.network.chat.Component.translatable("block.petbed.pet_bed"); }
        @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new PetBedMenu(id, inventory, bed.getBlockPos()); }
    }
}
