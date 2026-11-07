package com.faust.core.mixin;

import com.faust.core.runtime.CoreRuntime;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @Inject(method="performShooting",at=@At("HEAD"),cancellable=true)
    private void core$shot(Level level,LivingEntity entity,InteractionHand hand,ItemStack crossbow,float velocity,float inaccuracy,LivingEntity target,CallbackInfo ci){
        var loaded=crossbow.get(DataComponents.CHARGED_PROJECTILES);
        if(entity instanceof ServerPlayer p&&loaded!=null&&!loaded.isEmpty()&&!CoreRuntime.ranged(p,"crossbow"))ci.cancel();
    }
}
