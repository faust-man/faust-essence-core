package com.faust.core.client;
import com.faust.core.progression.*;
import com.faust.core.runtime.Snapshot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
public final class CoreClient {
 public static Progress progress; public static Balance balance; public static int experience; public static double weight;
 public static final net.minecraft.client.KeyMapping OPEN = new net.minecraft.client.KeyMapping("key.faustcore.skills",InputConstants.Type.KEYSYM,75,"key.categories.faustcore");
 public static java.util.function.Consumer<Boolean> sendInput=value->{};
 public static java.util.function.Consumer<com.faust.core.runtime.ConfigEdit> sendConfig=value->{};
 public static java.nio.file.Path configDirectory;
 public static boolean configAllowed;
 public static void configResult(com.faust.core.runtime.ConfigResult result){configAllowed=result.allowed();var mc=Minecraft.getInstance();if(mc.screen instanceof CoreConfigScreen screen)screen.result(result);}
 public static void receive(Snapshot snapshot) {progress=Progress.parse(snapshot.progress());if(!snapshot.balance().isEmpty())balance=Balance.JSON.fromJson(snapshot.balance(),Balance.class);experience=snapshot.experience();weight=snapshot.armorWeight();}
 public static void reset(){progress=null;balance=null;}
 public static void tick() {
  Minecraft mc=Minecraft.getInstance();if(mc.player==null||balance==null)return;
  if(balance.stamina.get("enabled")>0&&progress!=null&&progress.exhausted)mc.player.setSprinting(false);
  if(mc.player.tickCount%5==0)sendInput.accept(mc.screen==null&&mc.player.input.forwardImpulse>0);
  while(OPEN.consumeClick())if(mc.screen==null)mc.setScreen(new CoreScreen());
 }
 private static String units(double value){return Long.toString((long)Math.floor(value));}
 public static void hud(GuiGraphics g) {
  Minecraft mc=Minecraft.getInstance();if(mc.player==null||progress==null||balance==null||mc.options.hideGui||balance.client.get("hud")==0||balance.stamina.get("enabled")==0)return;
  int x=mc.getWindow().getGuiScaledWidth()/2+10+balance.client.get("hudX").intValue(),y=mc.getWindow().getGuiScaledHeight()-52+balance.client.get("hudY").intValue(),w=balance.client.get("hudWidth").intValue();
  g.fill(x,y,x+w,y+12,0xCC18202A);
  g.fill(x,y,x+(int)(w*progress.maxStamina(balance)/progress.baseStamina(balance)),y+12,0xFF303030);
  g.fill(x,y,x+(int)(w*progress.stamina/progress.baseStamina(balance)),y+12,0xFF39A895);
  int boundary=x+(int)Math.round(w*progress.maxStamina(balance)/progress.baseStamina(balance));
  g.fill(boundary,y,x+w,y+12,0xFF000000);
  g.drawString(mc.font,Component.translatable("core.stamina",units(progress.stamina),units(progress.baseStamina(balance))),x+3,y+2,0xFFFFFF,false);
 }
}
