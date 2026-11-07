package com.faust.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/** Inventory and skills tabs. Texture copied unchanged; see META-INF/NOTICE-faustcore.txt. */
public final class PanelTabs {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("faustcore","textures/gui/container/tabs.png");
    public static void draw(GuiGraphics g,int x,int y,boolean skills,int mouseX,int mouseY) {
        var mc=Minecraft.getInstance();
        int hovered=-1;
        for(int i=0;i<2;i++) {
            int tx=x+i*27,ty=y-28;boolean selected=(i==1)==skills;
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            g.blit(TEXTURE,tx,ty,i*26,selected?32:0,26,32);
            g.renderItem(new ItemStack(i==0?Items.PLAYER_HEAD:Items.BOOK),tx+5,ty+7);
            if(inside(mouseX,mouseY,tx,ty,26,28))hovered=i;
        }
        if(hovered>=0)tooltip(g,java.util.List.of(Component.translatable(hovered==0?"container.inventory":"core.panel")),mouseX,mouseY);
    }
    public static void tooltip(GuiGraphics g,java.util.List<Component> text,int mouseX,int mouseY) {
        g.pose().pushPose();g.pose().translate(0,0,400);
        var mc=Minecraft.getInstance();float scale=Math.abs(g.pose().last().pose().m00());
        int screenWidth=(int)(mc.getWindow().getGuiScaledWidth()/scale),screenHeight=(int)(mc.getWindow().getGuiScaledHeight()/scale);
        var lines=new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
        for(Component line:text)lines.addAll(mc.font.split(line,Math.min(240,screenWidth-16)));
        int w=lines.stream().mapToInt(mc.font::width).max().orElse(0),h=lines.size()*10;
        int x=Math.max(4,Math.min(mouseX+10,screenWidth-w-5)),y=Math.max(4,Math.min(mouseY+10,screenHeight-h-5));
        g.fill(x-3,y-3,x+w+3,y+h+3,0xFF130F1D);g.fill(x-2,y-2,x+w+2,y+h+2,0xF0181622);
        for(var line:lines){g.drawString(mc.font,line,x,y,0xFFFFFF,false);y+=10;}
        g.pose().popPose();
    }
    public static boolean click(double mx,double my,int button,int x,int y,boolean skills) {
        if(button!=0||Minecraft.getInstance().player==null)return false;
        if(!skills&&inside(mx,my,x+27,y-28,26,28)){Minecraft.getInstance().setScreen(new CoreScreen());return true;}
        if(skills&&inside(mx,my,x,y-28,26,28)){Minecraft.getInstance().setScreen(new InventoryScreen(Minecraft.getInstance().player));return true;}
        return false;
    }
    public static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
}
