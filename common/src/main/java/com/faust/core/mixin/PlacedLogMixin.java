package com.faust.core.mixin;
import com.faust.core.runtime.PlacedLogs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public abstract class PlacedLogMixin {
 @Inject(method="placeBlock",at=@At("RETURN")) private void core$placed(BlockPlaceContext context,BlockState state,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue()&&context.getPlayer()!=null&&context.getLevel() instanceof ServerLevel level&&state.is(net.minecraft.tags.BlockTags.LOGS))PlacedLogs.get(level).add(context.getClickedPos());}
}
