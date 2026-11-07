package com.faust.core.runtime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record ConfigResult(boolean allowed,boolean success,String message) implements CustomPacketPayload {
    public static final Type<ConfigResult> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("faustcore","config_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ConfigResult> CODEC=StreamCodec.of((b,p)->{b.writeBoolean(p.allowed);b.writeBoolean(p.success);b.writeUtf(p.message,4096);},b->new ConfigResult(b.readBoolean(),b.readBoolean(),b.readUtf(4096)));
    @Override public Type<ConfigResult> type(){return TYPE;}
}
