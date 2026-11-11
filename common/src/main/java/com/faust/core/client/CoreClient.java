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
 private static final StaminaClient STAMINA=java.util.ServiceLoader.load(StaminaClient.class,CoreClient.class.getClassLoader()).findFirst().orElseGet(()->new StaminaClient(){});
 public static void beginHud(){STAMINA.beginHud();}
 public static void airSprite(net.minecraft.resources.ResourceLocation sprite,int y){STAMINA.airSprite(sprite,y);}
 public static void reset(){progress=null;balance=null;STAMINA.reset();}

 public static boolean canSprint(Object entity){var mc=Minecraft.getInstance();return entity!=mc.player||mc.player==null||mc.player.isCreative()||mc.player.isSpectator()||progress==null||balance==null||!progress.actionsBlocked(balance);}
 static {com.faust.core.runtime.ClientActions.sprintAllowed=CoreClient::canSprint;com.faust.core.runtime.ClientActions.jumpAllowed=()->{var mc=Minecraft.getInstance();if(mc.player==null||mc.player.isCreative()||mc.player.isSpectator()||balance==null||progress==null||balance.stamina.get("enabled")==0)return true;if(progress.actionsBlocked(balance)){mc.player.setSprinting(false);return weight<=progress.capacity(balance);}return !mc.player.isSprinting()||progress.stamina>=balance.stamina.get("sprintJumpCost")+weight*balance.stamina.get("jumpArmorCost");};}
 public static void tick() {
  Minecraft mc=Minecraft.getInstance();if(mc.player==null||balance==null)return;
  if(balance.stamina.get("enabled")>0&&progress!=null&&progress.actionsBlocked(balance))mc.player.setSprinting(false);
  if(mc.player.tickCount%5==0)sendInput.accept(new com.faust.core.runtime.SwimInput(mc.screen==null&&mc.player.input.forwardImpulse>0,STAMINA.lastAirDrawn()));
  while(OPEN.consumeClick())if(mc.screen==null)mc.setScreen(new CoreScreen());
 }
 public static void hud(GuiGraphics g) {STAMINA.hud(g);}
}