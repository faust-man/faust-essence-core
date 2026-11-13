package com.faust.core.mixin;
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.block.FireBlock.class)
public interface FireBlockAccessor {
 @org.spongepowered.asm.mixin.gen.Invoker("getIgniteOdds") int core$igniteOdds(net.minecraft.world.level.block.state.BlockState state);
}
