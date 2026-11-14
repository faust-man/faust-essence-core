package com.faust.core.mixin;
import com.faust.core.runtime.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class ExperienceStealMixin {
 @Unique private final java.util.ArrayDeque<Float> core$xpHealth=new java.util.ArrayDeque<>();
 @Inject(method="actuallyHurt",at=@At("HEAD")) private void core$before(DamageSource source,float damage,CallbackInfo ci){core$xpHealth.push((((Player)(Object)this).getHealth()+((Player)(Object)this).getAbsorptionAmount()));}
 @Inject(method="actuallyHurt",at=@At("RETURN")) private void core$after(DamageSource source,float damage,CallbackInfo ci){float before=core$xpHealth.pop();if((Object)this instanceof ServerPlayer p&&p.getHealth()+p.getAbsorptionAmount()<before)ExperienceSteal.hit(p,source);}
 @Inject(method={"giveExperiencePoints","giveExperienceLevels"},at=@At("RETURN")) private void core$levelPeak(int amount,CallbackInfo ci){if((Object)this instanceof ServerPlayer p&&CoreRuntime.active(p))CoreRuntime.data(p).recordMaximum("experience_levels_held",Math.min(25,p.experienceLevel),CoreRuntime.balance);}
}
