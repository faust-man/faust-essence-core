package com.faust.core.mixin;
import com.faust.core.runtime.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandMixin {
    @Unique private java.util.UUID core$brewer;
    @Inject(method="createMenu",at=@At("HEAD"))
    private void core$owner(int id,Inventory inventory,CallbackInfoReturnable<AbstractContainerMenu> ci){core$brewer=inventory.player.getUUID();((BrewingStandBlockEntity)(Object)this).setChanged();}
    @Inject(method="saveAdditional",at=@At("TAIL"))
    private void core$save(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci){if(core$brewer!=null)tag.putUUID("faustcore:brewer",core$brewer);}
    @Inject(method="loadAdditional",at=@At("TAIL"))
    private void core$load(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci){core$brewer=tag.hasUUID("faustcore:brewer")?tag.getUUID("faustcore:brewer"):null;}
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="doBrew",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/alchemy/PotionBrewing;mix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack core$brew(net.minecraft.world.item.alchemy.PotionBrewing recipes,ItemStack ingredient,ItemStack input,com.llamalad7.mixinextras.injector.wrapoperation.Operation<ItemStack> original,Level level,BlockPos pos,NonNullList<ItemStack> items){
        ItemStack clean=BrewingRewards.unboosted(input);
        ItemStack result=original.call(recipes,ingredient,clean);
        if(result==clean)return input;
        if(result!=input&&level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand){var mixin=(BrewingStandMixin)(Object)stand;if(mixin.core$brewer!=null&&level.getServer()!=null){var player=level.getServer().getPlayerList().getPlayer(mixin.core$brewer);if(player!=null&&CoreRuntime.active(player))BrewingRewards.complete(player,result);}}
        return result;
    }
}
