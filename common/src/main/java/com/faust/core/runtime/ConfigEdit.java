package com.faust.core.runtime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
/** Empty section = permission/read request, otherwise edits exactly one whitelisted group. */
public record ConfigEdit(String section,String json) implements CustomPacketPayload {
    public static final Type<ConfigEdit> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("faustcore","config_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ConfigEdit> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.section,64);b.writeUtf(p.json,8192);},b->new ConfigEdit(b.readUtf(64),b.readUtf(8192)));
    @Override public Type<ConfigEdit> type(){return TYPE;}
}
