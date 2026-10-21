package com.petrick.petbed.menu;

import com.petrick.petbed.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import java.util.UUID;

public class PetControlMenu extends AbstractContainerMenu {
    private final int petEntityId;
    private final UUID petId;
    private String name, species, posture, mode, state, distance;
    public PetControlMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenus.PET_CONTROL.get(), id);
        petEntityId=buf.readVarInt(); petId=buf.readUUID(); name=buf.readUtf(128); species=buf.readUtf(128);
        posture=buf.readUtf(32); mode=buf.readUtf(32); state=buf.readUtf(64); distance=buf.readUtf(64);
    }
    public PetControlMenu(int id, int entityId, UUID uuid, String name, String species, String posture, String mode, String state, String distance) {
        super(ModMenus.PET_CONTROL.get(), id); this.petEntityId=entityId; this.petId=uuid; this.name=name; this.species=species;
        this.posture=posture; this.mode=mode; this.state=state; this.distance=distance;
    }
    public int getPetEntityId(){return petEntityId;} public UUID getPetId(){return petId;} public String getName(){return name;}
    public String getSpecies(){return species;} public String getPosture(){return posture;} public String getMode(){return mode;}
    public String getState(){return state;} public String getDistance(){return distance;}
    public void updateStatus(String posture,String mode,String state,String distance){this.posture=posture;this.mode=mode;this.state=state;this.distance=distance;}
    @Override public boolean stillValid(Player player){return true;}
    @Override public net.minecraft.world.item.ItemStack quickMoveStack(Player player,int index){return net.minecraft.world.item.ItemStack.EMPTY;}
}
