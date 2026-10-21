package com.petrick.petbed.client;

import com.petrick.petbed.menu.PetBedMenu;
import com.petrick.petbed.network.PetBedNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import com.petrick.petbed.block.PetBedArea;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.UUID;

public class PetBedScreen extends AbstractContainerScreen<PetBedMenu> {
    private int selected = -1;
    private int page = 0;
    private static final int PAGE_SIZE = 5;
    private Button linkButton, unlinkButton;
    private static PetBedScreen current;

    public PetBedScreen(PetBedMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth=280; imageHeight=220; inventoryLabelY=1000; current=this;
    }

    @Override protected void init() {
        super.init();
        int left=(width-imageWidth)/2, top=(height-imageHeight)/2;
        if (menu.isLinked()) {
            int buttonWidth=48;
            for(int i=0;i<PetBedArea.OPTIONS.length;i++) {
                final int size=PetBedArea.OPTIONS[i];
                String label=(menu.getAreaSize()==size?"✓ ":"")+size+"×"+size;
                addRenderableWidget(button(label,left+8+i*52,top+135,buttonWidth,18,b -> PetBedNetwork.sendArea(menu.getPos(),size)));
            }
            unlinkButton=addRenderableWidget(button("Desvincular pet",left+8,top+178,126,20,b -> PetBedNetwork.sendAction(menu.getPos(),1,null)));
            addRenderableWidget(button("Fechar",left+146,top+178,126,20,b -> onClose()));
        } else {
            int start=page*PAGE_SIZE, max=Math.min(menu.getCandidates().size()-start,PAGE_SIZE);
            for(int row=0;row<max;row++) {
                final int index=start+row;
                var c=menu.getCandidates().get(index);
                addRenderableWidget(button((index==selected?"▶ ":"")+c.name()+" — "+c.species(),left+10,top+57+row*16,260,16,b -> { selected=index; rebuildWidgets(); }));
            }
            int pages=Math.max(1,(menu.getCandidates().size()+PAGE_SIZE-1)/PAGE_SIZE);
            Button prev=addRenderableWidget(button("‹",left+12,top+143,30,18,b -> { page=Math.max(0,page-1); rebuildWidgets(); })); prev.active=page>0;
            Button next=addRenderableWidget(button("›",left+238,top+143,30,18,b -> { page=Math.min(pages-1,page+1); rebuildWidgets(); })); next.active=page<pages-1;
            linkButton=addRenderableWidget(button("Vincular Pet",left+10,top+178,126,20,b -> {
                if(selected>=0 && selected<menu.getCandidates().size()) PetBedNetwork.sendAction(menu.getPos(),0,menu.getCandidates().get(selected).id());
            }));
            linkButton.active=selected>=0;
            addRenderableWidget(button("Fechar",left+146,top+178,126,20,b -> onClose()));
        }
    }

    private Button button(String label,int x,int y,int w,int h,Button.OnPress action) { return new Button(x,y,w,h,Component.literal(label),action); }

    @Override protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        int left=(width-imageWidth)/2, top=(height-imageHeight)/2;
        GuiComponent.fill(poseStack,left,top,left+imageWidth,top+imageHeight,0xFF34271D);
        GuiComponent.fill(poseStack,left+2,top+2,left+imageWidth-2,top+imageHeight-2,0xFFE8D9C6);
        GuiComponent.fill(poseStack,left+2,top+2,left+imageWidth-2,top+22,0xFF865329);
        font.draw(poseStack,Component.literal("Pet vinculado: "+(menu.isLinked()?menu.getPetName():"Nenhum")),left+10,top+30,0xFF241A14);
        if(menu.isLinked()) {
            font.draw(poseStack,Component.literal("Espécie: "+menu.getSpecies()),left+10,top+47,0xFF44372B);
            font.draw(poseStack,Component.literal("Estado: "+menu.getState()+"   Postura: "+menu.getPosture()),left+10,top+63,0xFF44372B);
            font.draw(poseStack,Component.literal("Modo: "+modeName(menu.getMode())+"   Distância: "+menu.getDistance()),left+10,top+79,0xFF44372B);
            font.draw(poseStack,Component.literal("Área de movimentação: "+menu.getAreaSize()+"×"+menu.getAreaSize()),left+10,top+113,0xFF44372B);
            font.draw(poseStack,Component.literal("Caminha vinculada: frente "+facingName(menu.getPos())),left+10,top+96,0xFF44372B);
        } else {
            font.draw(poseStack,Component.literal("Pets carregados do jogador (qualquer distância):"),left+10,top+39,0xFF44372B);
            if(menu.getCandidates().isEmpty()) font.draw(poseStack,Component.literal("Nenhum elegível carregado nesta dimensão"),left+10,top+65,0xFF66584C);
            else font.draw(poseStack,Component.literal((page+1)+" / "+Math.max(1,(menu.getCandidates().size()+PAGE_SIZE-1)/PAGE_SIZE)),left+120,top+146,0xFF66584C);
        }
        font.draw(poseStack,Component.literal("Caminha dos Pets"),left+12,top+8,0xFFFFFFFF);
        if(menu.isLinked()) font.draw(poseStack,Component.literal("Selecione o tamanho da área quadrada:"),left+10,top+122,0xFF66584C);
    }

    public static void applyStatus(UUID id,String name,String species,String state,String distance) {
        if(current==null || current.minecraft==null || current.minecraft.screen!=current) return;
        current.menu.updateStatus(id,name,species,state,distance,"Em pé",current.menu.getMode(),current.menu.getAreaSize());
        current.rebuildWidgets();
    }

    public static void applyStatus(UUID id,String name,String species,String state,String distance,String posture,String mode,int size) {
        if(current==null || current.minecraft==null || current.minecraft.screen!=current) return;
        current.menu.updateStatus(id,name,species,state,distance,posture,mode,size);
        current.rebuildWidgets();
    }

    private static String modeName(String mode) { return switch(mode) { case "sit" -> "Sentar"; case "follow" -> "Seguir"; default -> "Livre"; }; }
    private static String facingName(net.minecraft.core.BlockPos pos) {
        var player=net.minecraft.client.Minecraft.getInstance().player;
        if(player==null || player.level.getBlockEntity(pos)==null) return "—";
        var state=player.level.getBlockState(pos);
        return state.hasProperty(com.petrick.petbed.block.PetBedBlock.FACING) ? state.getValue(com.petrick.petbed.block.PetBedBlock.FACING).getName() : "—";
    }

    @Override public void onClose() { if(current==this) current=null; super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}
