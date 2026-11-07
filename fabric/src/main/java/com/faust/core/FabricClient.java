package com.faust.core;
import com.faust.core.client.CoreClient;
import com.faust.core.runtime.Snapshot;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
public final class FabricClient implements ClientModInitializer {
 public void onInitializeClient() {CoreClient.sendInput=value->ClientPlayNetworking.send(new com.faust.core.runtime.SwimInput(value));ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE,(payload,context)->context.client().execute(()->CoreClient.receive(payload)));}
}
