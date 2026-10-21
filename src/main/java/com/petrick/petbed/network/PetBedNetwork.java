package com.petrick.petbed.network;

import com.petrick.petbed.block.PetBedBlockEntity;
import com.petrick.petbed.block.PetLinkService;
import com.petrick.petbed.api.PetBedPets;
import com.petrick.petbed.menu.PetBedMenu;
import com.petrick.petbed.menu.PetControlMenu;
import com.petrick.petbed.block.PetBedMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.UUID;
import java.util.function.Supplier;

public final class PetBedNetwork {
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new net.minecraft.resources.ResourceLocation("petbed", "main"), () -> VERSION, VERSION::equals, VERSION::equals);
    private static boolean registered;
    private PetBedNetwork() {}
    public static void register() {
        if (registered) return; registered = true;
        CHANNEL.messageBuilder(ActionMessage.class, 0).encoder(ActionMessage::encode).decoder(ActionMessage::decode).consumerMainThread(ActionMessage::handle).add();
        CHANNEL.messageBuilder(StatusMessage.class, 1).encoder(StatusMessage::encode).decoder(StatusMessage::decode).consumerMainThread(StatusMessage::handle).add();
        CHANNEL.messageBuilder(ControlMessage.class, 2).encoder(ControlMessage::encode).decoder(ControlMessage::decode).consumerMainThread(ControlMessage::handle).add();
    }

    public static void sendAction(BlockPos pos, int action, UUID petId) { CHANNEL.sendToServer(new ActionMessage(pos, action, petId, 0)); }
    public static void sendArea(BlockPos pos,int size){ CHANNEL.sendToServer(new ActionMessage(pos,2,null,size)); }
    public static void sendControl(int entityId,int mode){ CHANNEL.sendToServer(new ControlMessage(entityId,mode)); }

    public record ActionMessage(BlockPos pos, int action, UUID petId, int value) {
        static void encode(ActionMessage m, FriendlyByteBuf b) { b.writeBlockPos(m.pos); b.writeVarInt(m.action); b.writeBoolean(m.petId != null); if (m.petId != null) b.writeUUID(m.petId); b.writeVarInt(m.value); }
        static ActionMessage decode(FriendlyByteBuf b) { BlockPos pos=b.readBlockPos(); int action=b.readVarInt(); return new ActionMessage(pos,action,b.readBoolean()?b.readUUID():null,b.readVarInt()); }
        static void handle(ActionMessage m, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx=context.get(); ServerPlayer player=ctx.getSender();
            if (player != null) {
                BlockPos pos=m.pos;
                net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level;
                if (player.containerMenu instanceof PetBedMenu menu && menu.getPos().equals(pos) && player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5) <= 64.0D
                        && level.getBlockEntity(pos) instanceof PetBedBlockEntity bed) {
                    boolean ok=false;
                    if (m.action == 0 && m.petId != null && bed.getPetId() == null) {
                        Entity entity=level.getEntity(m.petId);
                        if (entity instanceof Mob pet) ok=PetLinkService.link(player, level, bed, pet);
                    } else if (m.action == 1) ok=PetLinkService.unlink(player, level, bed);
                    else if (m.action == 2) ok=PetLinkService.setAreaSize(player,level,bed,m.value);
                    sendStatus(player, bed);
                    if (!ok) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.petbed.action_failed"), true);
                }
            }
            ctx.setPacketHandled(true);
        }
    }

    private static void sendStatus(ServerPlayer player, PetBedBlockEntity bed) {
        net.minecraft.server.level.ServerLevel level=(net.minecraft.server.level.ServerLevel) player.level;
        Entity entity=bed.getPetId()==null?null:level.getEntity(bed.getPetId());
        Mob pet=entity instanceof Mob m && PetBedPets.isTamedPet(m)?m:null;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StatusMessage(bed.getPetId(), pet==null?"Pet indisponível":pet.getDisplayName().getString(), pet==null?"—":pet.getType().getDescription().getString(), PetBedMenu.status(pet,level,bed.getBlockPos()), PetBedMenu.distance(pet,bed.getBlockPos()),PetBedMenu.posture(pet),bed.getMode().id(),bed.getAreaSize()));
    }

    public record StatusMessage(UUID petId, String name, String species, String state, String distance,String posture,String mode,int areaSize) {
        static void encode(StatusMessage m, FriendlyByteBuf b) { b.writeBoolean(m.petId != null); if(m.petId!=null)b.writeUUID(m.petId); b.writeUtf(m.name,128);b.writeUtf(m.species,128);b.writeUtf(m.state,64);b.writeUtf(m.distance,64); b.writeUtf(m.posture,32); b.writeUtf(m.mode,32); b.writeVarInt(m.areaSize); }
        static StatusMessage decode(FriendlyByteBuf b) { UUID id=b.readBoolean()?b.readUUID():null;return new StatusMessage(id,b.readUtf(128),b.readUtf(128),b.readUtf(64),b.readUtf(64),b.readUtf(32),b.readUtf(32),b.readVarInt()); }
        static void handle(StatusMessage m, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx=context.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.petrick.petbed.client.PetBedScreen.applyStatus(m.petId,m.name,m.species,m.state,m.distance,m.posture,m.mode,m.areaSize));
            ctx.setPacketHandled(true);
        }
    }

    public record ControlMessage(int entityId,int mode) {
        static void encode(ControlMessage m,FriendlyByteBuf b){b.writeVarInt(m.entityId);b.writeVarInt(m.mode);}
        static ControlMessage decode(FriendlyByteBuf b){return new ControlMessage(b.readVarInt(),b.readVarInt());}
        static void handle(ControlMessage m,Supplier<NetworkEvent.Context> context){
            NetworkEvent.Context ctx=context.get(); ServerPlayer player=ctx.getSender();
            if(player!=null && player.containerMenu instanceof PetControlMenu menu && menu.getPetEntityId()==m.entityId && m.mode>=0 && m.mode<=2 && player.level instanceof net.minecraft.server.level.ServerLevel level){
                Entity entity=level.getEntity(m.entityId);
                PetBedMode mode= m.mode==1?PetBedMode.SIT:m.mode==2?PetBedMode.FOLLOW:PetBedMode.FREE;
                if(entity!=null && entity.getUUID().equals(menu.getPetId()) && PetLinkService.setMode(player,level,entity,mode)) player.displayClientMessage(net.minecraft.network.chat.Component.literal("Modo do pet: "+mode.id()),true);
            }
            ctx.setPacketHandled(true);
        }
    }
}
