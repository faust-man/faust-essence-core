package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FoodData.class)
public abstract class MetabolismMixin {
 @Shadow private int tickTimer;
 @Inject(method="tick",at=@At("HEAD")) private void core$metabolism(Player player,CallbackInfo ci){
  if(player instanceof ServerPlayer p&&CoreRuntime.active(p)&&CoreRuntime.data(p).enabled("faustcore:metabolism",CoreRuntime.balance)&&p.isHurt()&&p.getFoodData().getFoodLevel()>=18&&p.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION))tickTimer++;
 }
}
