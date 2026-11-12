package com.faust.core.mixin;
import com.faust.core.runtime.PlacedLogs;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public abstract class LogChangeMixin {
 @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("RETURN")) private void core$changed(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue()&&(Object)this instanceof ServerLevel level)PlacedLogs.get(level).remove(pos);}
}
