package com.faust.core.mixin;

import com.faust.core.runtime.CoreRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Inject(method="releaseUsing",at=@At("HEAD"),cancellable=true)
    private void core$shot(ItemStack bow,Level level,LivingEntity entity,int remaining,CallbackInfo ci){
        if(entity instanceof ServerPlayer p&&!p.getProjectile(bow).isEmpty()&&BowItem.getPowerForTime(bow.getUseDuration(entity)-remaining)>=0.1f&&!CoreRuntime.ranged(p,"bow"))ci.cancel();
    }
}
