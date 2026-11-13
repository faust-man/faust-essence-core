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
 private void core$catScent(CallbackInfo ci){goalSelector.addGoal(1,new AvoidEntityGoal<Player>(this,Player.class,5.0F,1.0,1.2,e->e instanceof ServerPlayer p&&CoreRuntime.active(p)&&CoreRuntime.data(p).enabled("faustcore:cat_scent",CoreRuntime.balance)));}
 @Inject(method="tick",at=@At("HEAD"))
 private void core$defuse(CallbackInfo ci){
  var self=(Creeper)(Object)this;
  if(!level().isClientSide&&level().getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(5),p->p.distanceToSqr(self)<=25&&CoreRuntime.active(p)&&CoreRuntime.data(p).enabled("faustcore:cat_scent",CoreRuntime.balance)).size()>0){
   self.setSwellDir(-1);self.getEntityData().set(DATA_IS_IGNITED,false);core$resetSwell();
  }
 }
 @org.spongepowered.asm.mixin.Shadow private int swell;
 @org.spongepowered.asm.mixin.Unique private void core$resetSwell(){swell=0;}
 @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private static net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_IS_IGNITED;
}
