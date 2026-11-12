package com.faust.core.mixin;
import com.faust.core.runtime.*;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayerGameMode.class)
public abstract class MiningMixin {
 @Shadow protected ServerLevel level;
 @Shadow @Final protected ServerPlayer player;
 @Unique private final java.util.Deque<Boolean> core$eligible=new java.util.ArrayDeque<>();
 @Inject(method="destroyBlock",at=@At("HEAD")) private void core$before(BlockPos pos,CallbackInfoReturnable<Boolean> ci){var state=level.getBlockState(pos);core$eligible.push(!state.isAir()&&!(state.is(net.minecraft.tags.BlockTags.LEAVES)&&player.getMainHandItem().is(net.minecraft.world.item.Items.SHEARS))&&!PlacedLogs.get(level).contains(pos)&&state.getDestroySpeed(level,pos)>0&&state.getDestroyProgress(player,level,pos)<1);}
 @Inject(method="destroyBlock",at=@At("RETURN")) private void core$after(BlockPos pos,CallbackInfoReturnable<Boolean> ci){if(core$eligible.pop()&&ci.getReturnValue())CoreRuntime.mined(player);}
}
