package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Creeper.class)
public abstract class CreeperMixin extends Monster {
 protected CreeperMixin(EntityType<? extends Monster> type,Level level){super(type,level);}
 @Inject(method="registerGoals",at=@At("RETURN"))
 private void core$catScent(CallbackInfo ci){goalSelector.addGoal(3,new AvoidEntityGoal<Player>(this,Player.class,3.0F,1.0,1.2,e->e instanceof ServerPlayer p&&CoreRuntime.active(p)&&CoreRuntime.data(p).enabled("faustcore:cat_scent",CoreRuntime.balance)));}
}
