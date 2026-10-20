package com.petrick.petbed.client;

import com.petrick.petbed.menu.PetBedMenu;
import com.petrick.petbed.network.PetBedNetwork;
import net.minecraft.client.gui.GuiGraphics;
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
        imageWidth=248; imageHeight=190; inventoryLabelY=1000; current=this;
    }

    @Override protected void init() {
        super.init();
        int left=(width-imageWidth)/2, top=(height-imageHeight)/2;
        if (menu.isLinked()) {
            unlinkButton=addRenderableWidget(Button.builder(Component.literal("Desvincular pet"), b -> PetBedNetwork.sendAction(menu.getPos(),1,null)).bounds(left+12,top+143,224,20).build());
        } else {
            int start=page*PAGE_SIZE, max=Math.min(menu.getCandidates().size()-start,PAGE_SIZE);
            for(int row=0;row<max;row++) {
                final int index=start+row;
                var c=menu.getCandidates().get(index);
                addRenderableWidget(Button.builder(Component.literal((index==selected?"▶ ":"")+c.name()+" — "+c.species()), b -> { selected=index; rebuildWidgets(); }).bounds(left+12,top+53+row*14,224,14).build());
            }
            int pages=Math.max(1,(menu.getCandidates().size()+PAGE_SIZE-1)/PAGE_SIZE);
            Button prev=addRenderableWidget(Button.builder(Component.literal("‹"), b -> { page=Math.max(0,page-1); rebuildWidgets(); }).bounds(left+12,top+112,30,18).build()); prev.active=page>0;
            Button next=addRenderableWidget(Button.builder(Component.literal("›"), b -> { page=Math.min(pages-1,page+1); rebuildWidgets(); }).bounds(left+206,top+112,30,18).build()); next.active=page<pages-1;
            linkButton=addRenderableWidget(Button.builder(Component.literal("Vincular Pet"), b -> {
                if(selected>=0 && selected<menu.getCandidates().size()) PetBedNetwork.sendAction(menu.getPos(),0,menu.getCandidates().get(selected).id());
            }).bounds(left+12,top+143,108,20).build());
            linkButton.active=selected>=0;
        }
        addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> onClose()).bounds(left+128,top+143,108,20).build());
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left=(width-imageWidth)/2, top=(height-imageHeight)/2;
        graphics.fill(left,top,left+imageWidth,top+imageHeight,0xFF34271D);
        graphics.fill(left+2,top+2,left+imageWidth-2,top+imageHeight-2,0xFFE8D9C6);
        graphics.fill(left+2,top+2,left+imageWidth-2,top+22,0xFF865329);
        graphics.drawString(font,Component.literal("Pet vinculado: "+(menu.isLinked()?menu.getPetName():"Nenhum")),left+12,top+29,0xFF241A14,false);
        if(menu.isLinked()) {
            graphics.drawString(font,Component.literal("Espécie: "+menu.getSpecies()),left+12,top+45,0xFF44372B,false);
            graphics.drawString(font,Component.literal("Estado: "+menu.getState()),left+12,top+61,0xFF44372B,false);
            graphics.drawString(font,Component.literal("Distância: "+menu.getDistance()),left+12,top+77,0xFF44372B,false);
        } else {
            graphics.drawString(font,Component.literal("Pets domesticados próximos:"),left+12,top+39,0xFF44372B,false);
            if(menu.getCandidates().isEmpty()) graphics.drawString(font,Component.literal("Nenhum pet elegível carregado"),left+12,top+60,0xFF66584C,false);
            else graphics.drawString(font,Component.literal((page+1)+" / "+Math.max(1,(menu.getCandidates().size()+PAGE_SIZE-1)/PAGE_SIZE)),left+108,top+117,0xFF66584C,false);
        }
        graphics.drawString(font,Component.literal("Caminha dos Pets"),left+12,top+8,0xFFFFFFFF,false);
    }

    public static void applyStatus(UUID id,String name,String species,String state,String distance) {
        if(current==null || current.minecraft==null || current.minecraft.screen!=current) return;
        current.menu.updateStatus(id,name,species,state,distance);
        current.rebuildWidgets();
    }

    @Override public void onClose() { if(current==this) current=null; super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}
