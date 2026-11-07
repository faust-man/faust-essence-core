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
    @Inject(method="handleAnimate",at=@At("TAIL"))
    private void core$airAttack(ServerboundSwingPacket packet,CallbackInfo ci){
        if(packet.getHand()==InteractionHand.MAIN_HAND&&player.pick(player.blockInteractionRange(),0,false).getType()!=HitResult.Type.BLOCK)CoreRuntime.attack(player,true);
    }
}
