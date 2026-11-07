package com.faust.core.mixin;

import com.faust.core.runtime.CoreRuntime;
import com.faust.core.runtime.CombatContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayDeque;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique private final ArrayDeque<Float> faustcore$health=new ArrayDeque<>();
    @Unique private double faustcore$budget=-1;
    @Inject(method="isDamageSourceBlocked",at=@At("RETURN"),cancellable=true)
    private void core$blockCost(DamageSource source,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue()&&(Object)this instanceof ServerPlayer p&&CombatContext.DAMAGE.get().peek()==source&&!CoreRuntime.shieldPayment(p))ci.setReturnValue(false);}
    @Inject(method="hurt",at=@At("HEAD"))
    private void core$before(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci) {faustcore$health.push(((LivingEntity)(Object)this).getHealth());CombatContext.DAMAGE.get().push(source);}
    @Inject(method="hurt",at=@At("RETURN"))
    private void core$after(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci) {
        LivingEntity entity=(LivingEntity)(Object)this;
        float before=faustcore$health.pop();CombatContext.DAMAGE.get().pop();double lost=Math.max(0,before-entity.getHealth());
        if(entity.level().isClientSide||lost<=0)return;
        // All sources count for constitution. Intentional damage training is explicitly allowed.
        if(entity instanceof ServerPlayer player&&com.faust.core.progression.SkillEffects.damageCounts(lost,CoreRuntime.balance)) CoreRuntime.grant(player,"damage",lost,lost);
        var attack=CombatContext.ATTACK.get().peek();
        if(entity instanceof Enemy && source.getDirectEntity() instanceof ServerPlayer attacker && !attacker.isAlliedTo(entity)
            && attack!=null && attack.target()==entity && attack.charge()>=CoreRuntime.balance.skills.get("hitCharge")) {
            if(faustcore$budget<0)faustcore$budget=entity.getMaxHealth();
            double q=Math.min(Math.min(lost,faustcore$budget),CoreRuntime.balance.skills.get("hitMaximum"));
            faustcore$budget-=q;
            CoreRuntime.grant(attacker,"melee",q,1);
        }
    }
}
