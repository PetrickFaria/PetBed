package com.petrick.petbed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.petrick.petbed.menu.PetControlMenu;
import com.petrick.petbed.network.PetBedNetwork;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PetControlScreen extends AbstractContainerScreen<PetControlMenu> {
    public PetControlScreen(PetControlMenu menu, Inventory inventory, Component title){super(menu,inventory,title); imageWidth=240; imageHeight=150;}
    @Override protected void init(){super.init(); int x=(width-imageWidth)/2, y=(height-imageHeight)/2;
        addRenderableWidget(new Button(x+10,y+112,68,20,Component.literal("Livre"),b->PetBedNetwork.sendControl(menu.getPetEntityId(),0)));
        addRenderableWidget(new Button(x+86,y+112,68,20,Component.literal("Sentar"),b->PetBedNetwork.sendControl(menu.getPetEntityId(),1)));
        addRenderableWidget(new Button(x+162,y+112,68,20,Component.literal("Seguir"),b->PetBedNetwork.sendControl(menu.getPetEntityId(),2)));
        addRenderableWidget(new Button(x+84,y+135,72,16,Component.literal("Fechar"),b->onClose()));
    }
    @Override protected void renderBg(PoseStack p,float partial,int mx,int my){int x=(width-imageWidth)/2,y=(height-imageHeight)/2;
        GuiComponent.fill(p,x,y,x+imageWidth,y+imageHeight,0xFFE8D9C6); GuiComponent.fill(p,x+2,y+2,x+imageWidth-2,y+24,0xFF865329);
        font.draw(p,Component.literal("PetBed — "+menu.getName()),x+10,y+8,0xFFFFFFFF);
        font.draw(p,Component.literal("Espécie: "+menu.getSpecies()),x+10,y+34,0xFF241A14);
        font.draw(p,Component.literal("Estado: "+menu.getState()+"   "+menu.getDistance()),x+10,y+50,0xFF44372B);
        font.draw(p,Component.literal("Postura: "+menu.getPosture()+"   Modo: "+menu.getMode()),x+10,y+67,0xFF44372B);
        font.draw(p,Component.literal("Escolha o comportamento do pet:"),x+10,y+91,0xFF44372B);
    }
    @Override public void render(PoseStack p,int mx,int my,float partial){renderBackground(p);super.render(p,mx,my,partial);renderTooltip(p,mx,my);}
    @Override public boolean isPauseScreen(){return false;}
}
