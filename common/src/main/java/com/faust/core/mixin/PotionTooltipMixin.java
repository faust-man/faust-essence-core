package com.faust.core.mixin;
import com.faust.core.runtime.BrewingRewards;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin({PotionItem.class,LingeringPotionItem.class})
public abstract class PotionTooltipMixin {
    @WrapOperation(method="appendHoverText",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/alchemy/PotionContents;addPotionTooltip(Ljava/util/function/Consumer;FF)V"))
    private void core$duration(PotionContents contents,java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,float scale,float tickRate,Operation<Void> original,ItemStack stack,Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){
        original.call(BrewingRewards.displayed(stack),tooltip,scale,tickRate);
    }
}
