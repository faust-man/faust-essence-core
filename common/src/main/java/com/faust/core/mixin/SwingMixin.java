package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SwingMixin {
    @Shadow public ServerPlayer player;
    @org.spongepowered.asm.mixin.Unique private int core$dropTick=Integer.MIN_VALUE;
    @Inject(method="handlePlayerAction",at=@At("HEAD")) private void core$drop(net.minecraft.network.protocol.game.ServerboundPlayerActionPacket packet,CallbackInfo ci){if(packet.getAction()==net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM||packet.getAction()==net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS)core$dropTick=player.tickCount;}

    @Inject(method="handleAnimate",at=@At("TAIL"))
    private void core$airAttack(ServerboundSwingPacket packet,CallbackInfo ci){
        if((long)player.tickCount-core$dropTick>1&&packet.getHand()==InteractionHand.MAIN_HAND&&player.pick(player.blockInteractionRange(),0,false).getType()!=HitResult.Type.BLOCK)CoreRuntime.attack(player,true);
    }
}
