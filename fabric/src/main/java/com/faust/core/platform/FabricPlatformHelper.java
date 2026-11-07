package com.faust.core.platform;

import com.faust.core.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override public void send(net.minecraft.server.level.ServerPlayer player,com.faust.core.runtime.Snapshot snapshot) { if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,com.faust.core.runtime.Snapshot.TYPE)) net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,snapshot); }


    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
