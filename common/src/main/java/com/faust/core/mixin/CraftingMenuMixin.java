package com.faust.core.mixin;
import com.faust.core.runtime.SkillCraftingRecipe;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
 @Inject(method="slotChangedCraftingGrid",at=@At("HEAD"))
 private static void core$begin(AbstractContainerMenu menu,Level level,Player player,CraftingContainer grid,ResultContainer result,RecipeHolder<CraftingRecipe> recipe,CallbackInfo ci){SkillCraftingRecipe.PLAYERS.get().push(player);}
 @Inject(method="slotChangedCraftingGrid",at=@At("RETURN"))
 private static void core$end(AbstractContainerMenu menu,Level level,Player player,CraftingContainer grid,ResultContainer result,RecipeHolder<CraftingRecipe> recipe,CallbackInfo ci){SkillCraftingRecipe.PLAYERS.get().pop();}
}
