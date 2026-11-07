package com.faust.core.runtime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
/** Intent only. Server checks water, flow, facing, state and input freshness. */
public record SwimInput(boolean forward,boolean airVisible) implements CustomPacketPayload {
    public static final Type<SwimInput> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("faustcore","swim_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SwimInput> CODEC=StreamCodec.of((b,p)->{b.writeBoolean(p.forward);b.writeBoolean(p.airVisible);},b->new SwimInput(b.readBoolean(),b.readBoolean()));
    @Override public Type<SwimInput> type(){return TYPE;}
}
