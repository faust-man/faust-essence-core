package com.faust.core.client;
import com.faust.core.progression.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;
public final class CoreScreen extends Screen {
 private Skill selected=Skill.strength;
 private int page;
 private java.util.List<Balance.Ability> visible(){var all=CoreClient.balance.abilities.stream().filter(a->a.skill()==selected).toList();int size=Math.max(1,(height-176)/36);int first=Math.min(page*size,Math.max(0,all.size()-1));return all.subList(first,Math.min(all.size(),first+size));}
 public CoreScreen(){super(Component.literal("core"));}
 protected void init() {
  int i=0,cell=Math.min(100,(width-24)/6);
  for(Skill skill:Skill.values())addRenderableWidget(Button.builder(Component.translatable("core.skill."+skill),b->{selected=skill;page=0;rebuildWidgets();}).bounds(12+i++*cell,28,cell-2,20).build());
  if(CoreClient.balance==null)return;
  addRenderableWidget(Button.builder(Component.translatable("core.cap.buy"),b->command("core cap "+selected)).bounds(12,116,150,20).build());
  int y=166;
  for(var ability:visible()) {final var a=ability;
   addRenderableWidget(Button.builder(Component.translatable("core.ability."+a.id().split(":")[1]),b->command("core unlock "+a.id())).bounds(12,y,Math.min(210,width/2),20).build());y+=36;}
  addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.max(0,page-1);rebuildWidgets();}).bounds(12,height-28,30,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->{int size=Math.max(1,(height-176)/36);long count=CoreClient.balance.abilities.stream().filter(a->a.skill()==selected).count();if((page+1)*size<count)page++;rebuildWidgets();}).bounds(46,height-28,30,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width-92,height-28,80,20).build());
 }
 private void command(String command){if(minecraft!=null&&minecraft.getConnection()!=null)minecraft.getConnection().sendCommand(command);}
 public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
  g.fill(0,0,width,height,0xF0141C28);super.render(g,mouseX,mouseY,partialTick);
  g.drawString(font,title,12,10,0xF5D497);Progress p=CoreClient.progress;Balance b=CoreClient.balance;if(p==null||b==null)return;var t=p.track(selected);
  g.drawString(font,Component.translatable("core.progress",t.level,fmt(t.xp),fmt(p.needed(selected,b)),fmt(p.cap(selected,b)),fmt(p.multiplier(selected,b)*100)),12,60,0xFFFFFF);
  g.drawString(font,Component.translatable("core.economy",CoreClient.experience,p.price(b),p.purchases),12,78,0xD8BF8D);
  g.drawString(font,Component.translatable("core.load",fmt(CoreClient.weight),fmt(p.capacity(b))),12,96,0xB5D4DB);
  g.drawString(font,Component.translatable("core.tree"),12,148,0xFFFFFF);int y=166;
  for(var a:visible()){
   String status=p.unlocked.contains(a.id())?Component.translatable("core.unlocked").getString():Component.translatable("core.requirement",a.level(),fmt(p.counters.getOrDefault(a.counter(),0.0)),fmt(a.amount())).getString()+" "+Component.translatable("core.counter."+a.counter()).getString();
   g.drawString(font,status,Math.min(230,width/2+24),y+5,p.unlocked.contains(a.id())?0x6DDFB2:0xD4DCE5);
   String parent=a.parent().isEmpty()?"—":Component.translatable("core.ability."+a.parent().split(":")[1]).getString();
   g.drawString(font,Component.translatable("core.parent",parent),14,y+23,0xA9B3C2);y+=36;
  }
 }
 private static String fmt(double d){return String.format(Locale.ROOT,"%.1f",d);}
 public boolean isPauseScreen(){return false;}
}
