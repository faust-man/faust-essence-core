package com.faust.core.mixin;
import com.faust.core.runtime.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method="slotChangedCraftingGrid",at=@At("RETURN"))
    private static void core$craft(AbstractContainerMenu menu,Level level,Player player,CraftingContainer grid,ResultContainer result,RecipeHolder<CraftingRecipe> recipe,CallbackInfo ci){
        if(!(player instanceof ServerPlayer p)||!CoreRuntime.active(p))return;
        var data=CoreRuntime.data(p);var stacks=new java.util.ArrayList<ItemStack>();
        for(int i=0;i<grid.getContainerSize();i++)if(!grid.getItem(i).isEmpty())stacks.add(grid.getItem(i));
        ItemStack output=ItemStack.EMPTY;
        if(data.enabled("faustcore:forester",CoreRuntime.balance)&&stacks.size()==1&&stacks.getFirst().is(net.minecraft.tags.ItemTags.SAPLINGS))output=new ItemStack(Items.STICK,2);
        if(data.enabled("faustcore:fire_starter",CoreRuntime.balance)){
            if(stacks.size()==2&&stacks.stream().allMatch(s->s.is(Items.STICK)))output=new ItemStack(FireStarterItem.ITEM.get());
            if(stacks.size()==2&&stacks.stream().anyMatch(s->s.is(FireStarterItem.ITEM.get()))&&stacks.stream().anyMatch(s->s.is(net.minecraft.tags.ItemTags.LOGS)))output=new ItemStack(Items.CAMPFIRE);
        }
        if(output.isEmpty())return;
        result.setRecipeUsed(null);result.setItem(0,output);menu.setRemoteSlot(0,output);
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(menu.containerId,menu.incrementStateId(),0,output));
    }
}
