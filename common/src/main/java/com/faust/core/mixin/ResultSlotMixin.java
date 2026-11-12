package com.faust.core.mixin;
import com.faust.core.runtime.SkillCraftingRecipe;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin extends net.minecraft.world.inventory.Slot {
 public ResultSlotMixin(net.minecraft.world.Container container,int slot,int x,int y){super(container,slot,x,y);}
 @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.world.inventory.CraftingContainer craftSlots;
 @Override public boolean mayPickup(Player player){
  var input=craftSlots.asCraftInput();var stacks=new java.util.ArrayList<ItemStack>();for(int i=0;i<input.size();i++)if(!input.getItem(i).isEmpty())stacks.add(input.getItem(i));
  boolean sapling=stacks.size()==1&&stacks.getFirst().is(net.minecraft.tags.ItemTags.SAPLINGS)&&getItem().is(net.minecraft.world.item.Items.STICK);
  boolean starter=getItem().is(com.faust.core.runtime.FireStarterItem.ITEM.get());
  if((sapling||starter)&&player instanceof net.minecraft.server.level.ServerPlayer p)return com.faust.core.runtime.CoreRuntime.data(p).enabled(sapling?"faustcore:forester":"faustcore:fire_starter",com.faust.core.runtime.CoreRuntime.balance);
  return super.mayPickup(player);
 }
 @Inject(method="onTake",at=@At("HEAD")) private void core$begin(Player player,ItemStack stack,CallbackInfo ci){SkillCraftingRecipe.PLAYERS.get().push(player);}
 @Inject(method="onTake",at=@At("RETURN")) private void core$end(Player player,ItemStack stack,CallbackInfo ci){SkillCraftingRecipe.PLAYERS.get().pop();}
}
