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
 public static java.util.function.Consumer<com.faust.core.runtime.SwimInput> sendInput=value->{};
 public static java.util.function.Consumer<com.faust.core.runtime.ConfigEdit> sendConfig=value->{};
 public static java.nio.file.Path configDirectory;
 public static boolean configAllowed;
 public static void configResult(com.faust.core.runtime.ConfigResult result){configAllowed=result.allowed();var mc=Minecraft.getInstance();if(mc.screen instanceof CoreConfigScreen screen)screen.result(result);}
 public static void receive(Snapshot snapshot) {progress=Progress.parse(snapshot.progress());if(!snapshot.balance().isEmpty())balance=Balance.JSON.fromJson(snapshot.balance(),Balance.class);experience=snapshot.experience();weight=snapshot.armorWeight();}
 private static boolean airDrawn,lastAirDrawn;private static int airTop,vehicleTop;
 public static void beginHud(){airDrawn=false;airTop=vehicleTop=Integer.MAX_VALUE;}
 public static void airSprite(net.minecraft.resources.ResourceLocation sprite,int y){if(HudClearance.vehicleHeart(sprite.getNamespace(),sprite.getPath()))vehicleTop=Math.min(vehicleTop,y);if(sprite.getPath().equals("hud/air")||sprite.getPath().equals("hud/air_bursting")){airDrawn=true;airTop=Math.min(airTop,y);}}
 public static void reset(){progress=null;balance=null;airDrawn=lastAirDrawn=false;}

 public static boolean canSprint(Object entity){var mc=Minecraft.getInstance();return entity!=mc.player||mc.player==null||mc.player.isCreative()||mc.player.isSpectator()||progress==null||balance==null||!progress.actionsBlocked(balance);}
 static {com.faust.core.runtime.ClientActions.sprintAllowed=CoreClient::canSprint;com.faust.core.runtime.ClientActions.jumpAllowed=()->{var mc=Minecraft.getInstance();if(mc.player==null||mc.player.isCreative()||mc.player.isSpectator()||balance==null||progress==null||balance.stamina.get("enabled")==0)return true;if(progress.actionsBlocked(balance)){mc.player.setSprinting(false);return true;}return !mc.player.isSprinting()||progress.stamina>=balance.stamina.get("sprintJumpCost")+weight*balance.stamina.get("jumpArmorCost");};}
 public static void tick() {
  Minecraft mc=Minecraft.getInstance();if(mc.player==null||balance==null)return;
  if(balance.stamina.get("enabled")>0&&progress!=null&&progress.actionsBlocked(balance))mc.player.setSprinting(false);
  if(mc.player.tickCount%5==0)sendInput.accept(new com.faust.core.runtime.SwimInput(mc.screen==null&&mc.player.input.forwardImpulse>0,lastAirDrawn));
  while(OPEN.consumeClick())if(mc.screen==null)mc.setScreen(new CoreScreen());
 }
 public static void hud(GuiGraphics g) {
  lastAirDrawn=airDrawn;var mc=Minecraft.getInstance();if(mc.player==null||mc.player.isCreative()||mc.player.isSpectator()||progress==null||balance==null||mc.options.hideGui||balance.client.get("hud")==0||balance.stamina.get("enabled")==0)return;
  int x=mc.getWindow().getGuiScaledWidth()/2+10+balance.client.get("hudX").intValue(),y=mc.getWindow().getGuiScaledHeight()-50+balance.client.get("hudY").intValue();
  y=HudClearance.staminaY(y,airDrawn?airTop:Integer.MAX_VALUE,vehicleTop);
  int total=(int)Math.ceil(progress.baseStamina(balance)),available=(int)Math.floor(progress.maxStamina(balance)),filled=(int)Math.floor(progress.stamina);
  var texture=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("faustcore","textures/gui/stamina_lightning.png");
  com.mojang.blaze3d.systems.RenderSystem.enableBlend();
  boolean exhausted=progress.actionsBlocked(balance);
  try {
   for(int unit=0;unit<total;unit++) {
    int icon=unit/2,half=unit%2,dx=x+(icon%10)*8+half*4,dy=y-(icon/10)*9;
    int state=StaminaSprites.state(unit,available,filled),color=StaminaSprites.color(state,exhausted);
    g.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1f);
    g.blit(texture,dx,dy,4,8,StaminaSprites.textureX(state,half),0,8,16,32,16);
   }
  } finally {
   g.setColor(1f,1f,1f,1f);
  }

 }
}
