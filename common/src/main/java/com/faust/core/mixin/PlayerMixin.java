package com.faust.core.mixin;

import com.faust.core.runtime.CoreRuntime;
import com.faust.core.runtime.CombatContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method="attack",at=@At("HEAD"),cancellable=true)
    private void core$attackStart(net.minecraft.world.entity.Entity target,CallbackInfo ci) {
        if((Object)this instanceof ServerPlayer player&&!CoreRuntime.attack(player,false)){ci.cancel();return;}
        CombatContext.ATTACK.get().push(new CombatContext.Attack(target,((Player)(Object)this).getAttackStrengthScale(0.5F)));
    }
    @Inject(method="attack",at=@At("RETURN"))
    private void core$attackEnd(net.minecraft.world.entity.Entity target,CallbackInfo ci) {if(!CombatContext.ATTACK.get().isEmpty())CombatContext.ATTACK.get().pop();}
    @ModifyVariable(method="hurtCurrentlyUsedShield",at=@At("HEAD"),argsOnly=true)
    private float core$shield(float damage) {
        if((Object)this instanceof ServerPlayer p) {
            var source=CombatContext.DAMAGE.get().peek();
            if(source!=null && source.getEntity() instanceof net.minecraft.world.entity.monster.Enemy && !p.isAlliedTo(source.getEntity()))CoreRuntime.shield(p,damage);
        }
        return damage;
    }
    @ModifyVariable(method="hurtCurrentlyUsedShield",at=@At("STORE"),ordinal=0)
    private int core$shieldWear(int wear) {
        if((Object)this instanceof ServerPlayer p) return Math.max(1,(int)Math.ceil(wear*(1-Math.min(0.95,CoreRuntime.data(p).effect("shield_durability",CoreRuntime.balance)))));
        return wear;
    }
    @Inject(method="disableShield",at=@At("TAIL"))
    private void core$cooldown(CallbackInfo ci) {
        if((Object)this instanceof ServerPlayer p) {
            double discount=Math.min(0.95,CoreRuntime.data(p).effect("shield_recovery",CoreRuntime.balance));
            if(discount>0)p.getCooldowns().addCooldown(Items.SHIELD,(int)Math.ceil(100*(1-discount)));
        }
    }
    @Inject(method="getDestroySpeed",at=@At("RETURN"),cancellable=true)
    private void core$breaking(BlockState state,CallbackInfoReturnable<Float> ci) {
        if((Object)this instanceof ServerPlayer p)ci.setReturnValue(ci.getReturnValue()*(float)(1+CoreRuntime.breaking(p)));
    }
}
