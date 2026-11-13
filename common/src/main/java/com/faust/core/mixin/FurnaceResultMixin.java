package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceResultMixin {
 @Shadow @Final private Player player;
 @Inject(method="checkTakeAchievements",at=@At("HEAD")) private void core$charcoal(ItemStack stack,CallbackInfo ci){if(stack.is(Items.CHARCOAL)&&player instanceof ServerPlayer p&&CoreRuntime.active(p))CoreRuntime.data(p).recordMaximum("charcoal_smelted",1,CoreRuntime.balance);}
}
