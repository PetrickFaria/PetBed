package com.petrick.petbed.client;

import com.petrick.petbed.menu.PetBedMenu;
import com.petrick.petbed.network.PetBedNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.UUID;
import java.util.function.Consumer;

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
            unlinkButton=addRenderableWidget(button("Desvincular pet",left+12,top+143,224,20,b -> PetBedNetwork.sendAction(menu.getPos(),1,null)));
        } else {
            int start=page*PAGE_SIZE, max=Math.min(menu.getCandidates().size()-start,PAGE_SIZE);
            for(int row=0;row<max;row++) {
                final int index=start+row;
                var c=menu.getCandidates().get(index);
                addRenderableWidget(button((index==selected?"▶ ":"")+c.name()+" — "+c.species(),left+12,top+53+row*14,224,14,b -> { selected=index; rebuildWidgets(); }));
            }
            int pages=Math.max(1,(menu.getCandidates().size()+PAGE_SIZE-1)/PAGE_SIZE);
            Button prev=addRenderableWidget(button("‹",left+12,top+112,30,18,b -> { page=Math.max(0,page-1); rebuildWidgets(); })); prev.active=page>0;
            Button next=addRenderableWidget(button("›",left+206,top+112,30,18,b -> { page=Math.min(pages-1,page+1); rebuildWidgets(); })); next.active=page<pages-1;
            linkButton=addRenderableWidget(button("Vincular Pet",left+12,top+143,108,20,b -> {
                if(selected>=0 && selected<menu.getCandidates().size()) PetBedNetwork.sendAction(menu.getPos(),0,menu.getCandidates().get(selected).id());
            }));
            linkButton.active=selected>=0;
        }
        addRenderableWidget(button("Fechar",left+128,top+143,108,20,b -> onClose()));
    }

    private Button button(String label,int x,int y,int w,int h,Button.OnPress action) { return new Button(x,y,w,h,Component.literal(label),action); }

    @Override protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        int left=(width-imageWidth)/2, top=(height-imageHeight)/2;
        GuiComponent.fill(poseStack,left,top,left+imageWidth,top+imageHeight,0xFF34271D);
        GuiComponent.fill(poseStack,left+2,top+2,left+imageWidth-2,top+imageHeight-2,0xFFE8D9C6);
        GuiComponent.fill(poseStack,left+2,top+2,left+imageWidth-2,top+22,0xFF865329);
        font.draw(poseStack,Component.literal("Pet vinculado: "+(menu.isLinked()?menu.getPetName():"Nenhum")),left+12,top+29,0xFF241A14);
        if(menu.isLinked()) {
            font.draw(poseStack,Component.literal("Espécie: "+menu.getSpecies()),left+12,top+45,0xFF44372B);
            font.draw(poseStack,Component.literal("Estado: "+menu.getState()),left+12,top+61,0xFF44372B);
            font.draw(poseStack,Component.literal("Distância: "+menu.getDistance()),left+12,top+77,0xFF44372B);
        } else {
            font.draw(poseStack,Component.literal("Pets domesticados próximos:"),left+12,top+39,0xFF44372B);
            if(menu.getCandidates().isEmpty()) font.draw(poseStack,Component.literal("Nenhum pet elegível carregado"),left+12,top+60,0xFF66584C);
            else font.draw(poseStack,Component.literal((page+1)+" / "+Math.max(1,(menu.getCandidates().size()+PAGE_SIZE-1)/PAGE_SIZE)),left+108,top+117,0xFF66584C);
        }
        font.draw(poseStack,Component.literal("Caminha dos Pets"),left+12,top+8,0xFFFFFFFF);
    }

    public static void applyStatus(UUID id,String name,String species,String state,String distance) {
        if(current==null || current.minecraft==null || current.minecraft.screen!=current) return;
        current.menu.updateStatus(id,name,species,state,distance);
        current.rebuildWidgets();
    }

    @Override public void onClose() { if(current==this) current=null; super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}
