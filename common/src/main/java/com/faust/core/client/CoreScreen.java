package com.faust.core.client;

import com.faust.core.progression.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Two-column overview and per-skill ability tree, using attributed JustLeveling sprites. */
public final class CoreScreen extends Screen {
    private static final ResourceLocation PANEL=ResourceLocation.fromNamespaceAndPath("faustcore","textures/gui/container/skill_page_1.png");
    private Skill selected;
    private int page;
    public CoreScreen(){super(Component.translatable("core.panel"));}
    private int panelWidth(){return selected==null?220:300;}
    private int panelHeight(){return 200;}
    private float scale(){return Math.min(1f,Math.min((width-12f)/panelWidth(),(height-12f)/(panelHeight()+28)));}
    private int x(){return Math.round((width/scale()-panelWidth())/2);}
    private int y(){return Math.round((height/scale()-panelHeight()+28)/2);}
    private List<Balance.Ability> abilities(){return CoreClient.balance.abilities.stream().filter(a->a.skill()==selected).toList();}
    private List<Balance.Ability> visible(){var list=abilities();int start=Math.min(page*2,Math.max(0,list.size()-1));return list.subList(start,Math.min(list.size(),start+2));}
    private static String number(double value){return String.format(Locale.ROOT,"%.1f",value);}
    private static Component skillName(Skill skill){return Component.translatableWithFallback("core.skill."+skill,skill.name());}
    private static Component abilityName(Balance.Ability ability){return Component.translatableWithFallback("core.ability."+ability.id().split(":")[1],ability.id());}
    private void command(String text){if(minecraft!=null&&minecraft.getConnection()!=null)minecraft.getConnection().sendCommand(text);}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){}
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderTransparentBackground(g);
        float scale=scale();g.pose().pushPose();g.pose().scale(scale,scale,1);mx=(int)(mx/scale);my=(int)(my/scale);
        int x=x(),y=y(),w=panelWidth(),h=panelHeight();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        g.blit(PANEL,x,y,w,h,0,0,176,166,256,256);
        g.fill(x+3,y+3,x+w-3,y+h-3,0xFFC6C6C6);
        Progress p=CoreClient.progress;Balance b=CoreClient.balance;
        if(p==null||b==null){center(g,Component.translatable("core.syncing"),x+w/2,y+70,0x555555);PanelTabs.draw(g,x,y,true,mx,my);g.pose().popPose();return;}
        if(selected==null) {
            center(g,minecraft.player==null?title:minecraft.player.getName(),x+w/2,y+8,0x505050);
            center(g,Component.translatable("core.levels",CoreClient.experience),x+w/2,y+21,0x505050);
            g.fill(x+10,y+37,x+w-10,y+41,0xFF303030);
            if(minecraft.player!=null)g.fill(x+10,y+37,x+10+(int)((w-20)*minecraft.player.experienceProgress),y+41,0xFF80B938);
            int i=0;
            for(Skill skill:Skill.values()) {
                if(i>=6)break;
                int cx=x+9+(i%2)*102,cy=y+49+(i/2)*45;i++;
                g.blit(PANEL,cx,cy,100,42,12,50,76,26,256,256);
                ResourceLocation icon=ResourceLocation.fromNamespaceAndPath("faustcore","textures/skill/"+skill+"/locked_"+SkillEffects.icon(p.track(skill).level,b)+".png");
                g.blit(icon,cx+4,cy+4,18,18,0,0,16,16,16,16);
                g.drawString(font,Component.translatable("core.short."+skill),cx+25,cy+4,0xFFFFFF,false);
                g.drawString(font,p.track(skill).level+" / "+(int)p.cap(skill,b),cx+25,cy+15,0xC6C6C6,false);
                g.drawWordWrap(font,Component.translatable("core.brief."+skill),cx+4,cy+28,94,0xD8D8D8);
                if(PanelTabs.inside(mx,my,cx,cy,100,42))PanelTabs.tooltip(g,List.of(skillName(skill)),mx,my);
            }
            center(g,Component.translatable("core.select_skill"),x+w/2,y+187,0x505050);
        } else {
            center(g,skillName(selected),x+w/2,y+7,0x505050);
            g.drawWordWrap(font,Component.translatable("core.description."+selected),x+10,y+22,w-20,0x505050);
            g.drawString(font,Component.translatable("core.skill_progress",p.track(selected).level,number(100*p.track(selected).xp/p.needed(selected,b))),x+10,y+47,0x404040,false);
            g.drawString(font,Component.translatable("core.cap_status",(int)p.cap(selected,b),(int)Math.round(p.multiplier(selected,b)*100)),x+10,y+61,0x404040,false);
            long cost=p.price(selected,b);boolean full=p.cap(selected,b)>=b.progression.get("maximumCap");
            box(g,x+w-124,y+76,114,18,full?Component.translatable("core.cap.maximum"):Component.translatable("core.cap.price",cost),!full&&CoreClient.experience>=cost);
            int row=0;
            for(var a:visible()) {
                int ay=y+100+row++*37;boolean unlocked=p.unlocked.contains(a.id());
                box(g,x+10,ay,w-20,33,abilityName(a),p.canUnlock(a)||unlocked);
                g.drawString(font,unlocked?Component.translatable("core.unlocked"):Component.translatable("core.node_condition",a.level(),number(p.counters.getOrDefault(a.counter(),0.0)),number(a.amount()),Component.translatable("core.counter."+a.counter())),x+16,ay+18,0xCFCFCF,false);
                if(PanelTabs.inside(mx,my,x+10,ay,w-20,33)) {
                    var lines=new ArrayList<Component>();lines.add(Component.translatable("core.effect."+a.effect(),number(a.value()*(Set.of("damage","speed","breaking","shield_durability","shield_recovery").contains(a.effect())?100:1))));
                    if(!a.parent().isEmpty())lines.add(Component.translatable("core.parent",a.parent()));
                    PanelTabs.tooltip(g,lines,mx,my);
                }
            }
            box(g,x+10,y+177,70,16,Component.translatable("core.back"),true);
            box(g,x+w-62,y+177,22,16,Component.literal("<"),page>0);
            box(g,x+w-36,y+177,22,16,Component.literal(">"),(page+1)*2<abilities().size());
        }
        PanelTabs.draw(g,x,y,true,mx,my);g.pose().popPose();
    }
    private void center(GuiGraphics g,Component text,int x,int y,int color){g.drawString(font,text,x-font.width(text)/2,y,color,false);}
    private void box(GuiGraphics g,int x,int y,int w,int h,Component text,boolean active) {
        g.fill(x,y,x+w,y+h,0xFF707070);g.fill(x+1,y+1,x+w-1,y+h-1,active?0xFF373737:0xFF555555);
        center(g,text,x+w/2,y+4,active?0xFFFFFF:0xAAAAAA);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        float scale=scale();mx/=scale;my/=scale;
        int x=x(),y=y(),w=panelWidth();
        if(PanelTabs.click(mx,my,button,x,y,true))return true;
        if(button!=0||CoreClient.progress==null||CoreClient.balance==null)return super.mouseClicked(mx,my,button);
        if(selected==null){int i=0;for(Skill skill:Skill.values()){if(i>=6)break;int cx=x+9+(i%2)*102,cy=y+49+(i/2)*45;i++;if(PanelTabs.inside(mx,my,cx,cy,100,42)){selected=skill;page=0;return true;}}}
        else {
            if(PanelTabs.inside(mx,my,x+10,y+177,70,16)){selected=null;page=0;return true;}
            if(PanelTabs.inside(mx,my,x+w-62,y+177,22,16)){page=Math.max(0,page-1);return true;}
            if(PanelTabs.inside(mx,my,x+w-36,y+177,22,16)){if((page+1)*2<abilities().size())page++;return true;}
            if(PanelTabs.inside(mx,my,x+w-124,y+76,114,18)){command("core cap "+selected);return true;}
            int row=0;for(var a:visible()){int ay=y+100+row++*37;if(PanelTabs.inside(mx,my,x+10,ay,w-20,33)){command("core unlock "+a.id());return true;}}
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(CoreClient.OPEN.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public boolean isPauseScreen(){return false;}
}
