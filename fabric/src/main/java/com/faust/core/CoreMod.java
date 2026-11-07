package com.faust.core;
import com.faust.core.runtime.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
public final class CoreMod implements ModInitializer {
 public void onInitialize() {
  CoreRuntime.initialize(FabricLoader.getInstance().getConfigDir().resolve(Constants.MOD_ID));
  PayloadTypeRegistry.playS2C().register(Snapshot.TYPE,Snapshot.CODEC);
  PayloadTypeRegistry.playS2C().register(ConfigResult.TYPE,ConfigResult.CODEC);
  PayloadTypeRegistry.playC2S().register(ConfigEdit.TYPE,ConfigEdit.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(ConfigEdit.TYPE,(payload,context)->context.server().execute(()->com.faust.core.platform.Services.PLATFORM.configResult(context.player(),CoreRuntime.editConfig(context.player(),payload))));
  PayloadTypeRegistry.playC2S().register(SwimInput.TYPE,SwimInput.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(SwimInput.TYPE,(payload,context)->context.server().execute(()->CoreRuntime.input(context.player(),payload.forward())));
  CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->CoreCommands.register(dispatcher));
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->CoreRuntime.disconnected(handler.player));
 }
}
