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
    @Unique private java.util.List<ItemStack> core$before;
    @Inject(method="doBrew",at=@At("HEAD"))
    private static void core$beforeBrew(Level level,BlockPos pos,NonNullList<ItemStack> items,CallbackInfo ci){
        if(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand){
            var self=(BrewingStandMixin)(Object)stand;
            self.core$before=new java.util.ArrayList<>();
            for(int i=0;i<3;i++){
                self.core$before.add(items.get(i).copy());
                items.set(i,BrewingRewards.unboosted(items.get(i)));
            }
        }
    }
    @Inject(method="doBrew",at=@At("TAIL"))
    private static void core$afterBrew(Level level,BlockPos pos,NonNullList<ItemStack> items,CallbackInfo ci){
        if(!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand))return;
        var self=(BrewingStandMixin)(Object)stand;var before=self.core$before;self.core$before=null;
        if(before==null)return;
        var player=self.core$brewer==null||level.getServer()==null?null:level.getServer().getPlayerList().getPlayer(self.core$brewer);
        for(int i=0;i<3;i++){
            if(!BrewingRewards.recipeChanged(before.get(i),items.get(i))){items.set(i,before.get(i));continue;}
            if(player!=null&&!player.isSpectator()&&!player.isDeadOrDying())BrewingRewards.complete(player,items.get(i));
        }
    }
}
