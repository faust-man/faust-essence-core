package com.faust.core.runtime;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;

/** Transform an incoming potion effect for its recipient, never the shared source instance. */
public final class Metabolism {
    private Metabolism() {}
    public static MobEffectInstance potion(LivingEntity target,MobEffectInstance effect){
        if(!(target instanceof ServerPlayer player)||!CoreRuntime.active(player)||!CoreRuntime.data(player).enabled("faustcore:metabolism",CoreRuntime.balance)
            ||effect.isInfiniteDuration()||effect.getEffect().value().isInstantenous()
            ||CoreRuntime.balance.metabolismExcludedEffects.contains(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString()))return effect;
        return new MobEffectInstance(effect.getEffect(),Math.max(1,effect.getDuration()/2),effect.getAmplifier()==Integer.MAX_VALUE?Integer.MAX_VALUE:effect.getAmplifier()+1,effect.isAmbient(),effect.isVisible(),effect.showIcon());
    }
}
