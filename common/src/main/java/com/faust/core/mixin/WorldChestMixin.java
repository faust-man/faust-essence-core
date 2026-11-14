package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RandomizableContainer.class)
public interface WorldChestMixin {
    @Inject(method="unpackLootTable",at=@At(value="INVOKE",target="Lnet/minecraft/advancements/critereon/LootTableTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceKey;)V"))
    private void core$chest(Player player,CallbackInfo ci){
        if((Object)this instanceof net.minecraft.world.level.block.entity.ChestBlockEntity&&player instanceof ServerPlayer p&&CoreRuntime.active(p))CoreRuntime.data(p).recordCounter("world_chests",1,CoreRuntime.balance);
    }
}
