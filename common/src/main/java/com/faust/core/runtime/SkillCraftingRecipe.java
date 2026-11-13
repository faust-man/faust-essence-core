package com.faust.core.runtime;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
public final class SkillCraftingRecipe extends CustomRecipe {
 public static java.util.function.Supplier<? extends RecipeSerializer<?>> SERIALIZER;
 public static final ThreadLocal<java.util.Deque<Player>> PLAYERS=ThreadLocal.withInitial(java.util.ArrayDeque::new);
 public SkillCraftingRecipe(CraftingBookCategory category){super(category);}
 private int kind(CraftingInput input){
  var stacks=new java.util.ArrayList<ItemStack>();for(int i=0;i<input.size();i++)if(!input.getItem(i).isEmpty())stacks.add(input.getItem(i));
  if(stacks.size()==1&&stacks.getFirst().is(net.minecraft.tags.ItemTags.SAPLINGS))return 1;
  if(stacks.size()==2&&stacks.stream().allMatch(s->s.is(Items.STICK)))return 2;return 0;
 }
 @Override public boolean matches(CraftingInput input,Level level){
  var player=PLAYERS.get().peek();if(!(player instanceof net.minecraft.server.level.ServerPlayer p)||!CoreRuntime.active(p))return false;
  int kind=kind(input);return kind!=0&&CoreRuntime.data(p).enabled(kind==1?"faustcore:forester":"faustcore:fire_starter",CoreRuntime.balance);
 }
 @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries){return kind(input)==1?new ItemStack(Items.STICK,2):new ItemStack(FireStarterItem.ITEM.get());}
 @Override public boolean canCraftInDimensions(int width,int height){return width*height>=1;}
 @Override public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
}
