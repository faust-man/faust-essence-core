package com.faust.core;
import com.faust.core.runtime.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
@Mod(Constants.MOD_ID)
public final class CoreMod {
 public CoreMod(IEventBus bus,net.neoforged.fml.ModContainer container) {
  CoreRuntime.initialize(FMLPaths.CONFIGDIR.get().resolve(Constants.MOD_ID));
  bus.addListener(this::payloads);
  if(FMLEnvironment.dist==Dist.CLIENT)ClientRegistration.keys(bus,container);
  NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event)->CoreCommands.register(event.getDispatcher()));
  NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)CoreRuntime.disconnected(p);});
 }
 private void payloads(RegisterPayloadHandlersEvent event) {
  event.registrar("3").playToServer(ConfigEdit.TYPE,ConfigEdit.CODEC,(payload,context)->context.enqueueWork(()->{if(context.player() instanceof net.minecraft.server.level.ServerPlayer p)com.faust.core.platform.Services.PLATFORM.configResult(p,CoreRuntime.editConfig(p,payload));}));
  event.registrar("3").playToServer(SwimInput.TYPE,SwimInput.CODEC,(payload,context)->context.enqueueWork(()->{if(context.player() instanceof net.minecraft.server.level.ServerPlayer p)CoreRuntime.input(p,payload.forward(),payload.airVisible());}));
  if(FMLEnvironment.dist==Dist.CLIENT) ClientRegistration.register(event);
  else {event.registrar("3").playToClient(Snapshot.TYPE,Snapshot.CODEC,(payload,context)->{});event.registrar("3").playToClient(ConfigResult.TYPE,ConfigResult.CODEC,(payload,context)->{});}
 }
 private static final class ClientRegistration {
  static void keys(IEventBus bus,net.neoforged.fml.ModContainer container){
   com.faust.core.client.CoreClient.configDirectory=FMLPaths.CONFIGDIR.get().resolve("faustcore");
   container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,(net.neoforged.neoforge.client.gui.IConfigScreenFactory)(c,parent)->new com.faust.core.client.CoreConfigScreen(parent));bus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event)->event.register(com.faust.core.client.CoreClient.OPEN));}
  static void register(RegisterPayloadHandlersEvent event) {
   com.faust.core.client.CoreClient.sendConfig=value->net.neoforged.neoforge.network.PacketDistributor.sendToServer(value);
   event.registrar("3").playToClient(ConfigResult.TYPE,ConfigResult.CODEC,(payload,context)->context.enqueueWork(()->com.faust.core.client.CoreClient.configResult(payload)));
   com.faust.core.client.CoreClient.sendInput=value->net.neoforged.neoforge.network.PacketDistributor.sendToServer(value);
   event.registrar("3").playToClient(Snapshot.TYPE,Snapshot.CODEC,(payload,context)->context.enqueueWork(()->com.faust.core.client.CoreClient.receive(payload)));}
 }
}
