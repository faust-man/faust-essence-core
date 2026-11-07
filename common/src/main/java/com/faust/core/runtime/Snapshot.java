package com.faust.core.runtime;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record Snapshot(String progress,String balance,int experience,double armorWeight) implements CustomPacketPayload {
    public static final Type<Snapshot> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("faustcore","snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of(
        (buf,p)->{buf.writeUtf(p.progress,262144);buf.writeUtf(p.balance,262144);buf.writeVarInt(p.experience);buf.writeDouble(p.armorWeight);},
        buf->new Snapshot(buf.readUtf(262144),buf.readUtf(262144),buf.readVarInt(),buf.readDouble()));
    @Override public Type<Snapshot> type(){return TYPE;}
}
