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
 public CoreMod(IEventBus bus) {
  CoreRuntime.initialize(FMLPaths.CONFIGDIR.get().resolve(Constants.MOD_ID));
  bus.addListener(this::payloads);
  NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event)->CoreCommands.register(event.getDispatcher()));
  NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event)->{if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)CoreRuntime.disconnected(p);});
 }
 private void payloads(RegisterPayloadHandlersEvent event) {
  event.registrar("1").playToServer(SwimInput.TYPE,SwimInput.CODEC,(payload,context)->context.enqueueWork(()->{if(context.player() instanceof net.minecraft.server.level.ServerPlayer p)CoreRuntime.input(p,payload.forward());}));
  if(FMLEnvironment.dist==Dist.CLIENT) ClientRegistration.register(event);
  else event.registrar("1").playToClient(Snapshot.TYPE,Snapshot.CODEC,(payload,context)->{});
 }
 private static final class ClientRegistration {
  static void register(RegisterPayloadHandlersEvent event) {
   com.faust.core.client.CoreClient.sendInput=value->net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SwimInput(value));
   event.registrar("1").playToClient(Snapshot.TYPE,Snapshot.CODEC,(payload,context)->context.enqueueWork(()->com.faust.core.client.CoreClient.receive(payload)));}
 }
}
