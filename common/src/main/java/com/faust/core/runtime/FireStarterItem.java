package com.faust.core.runtime;

import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.server.level.ServerPlayer;

/** One ignition attempt per completed use; never rolls every tick. */
public final class FireStarterItem extends Item {
    public static java.util.function.Supplier<? extends Item> ITEM;
    public FireStarterItem(){super(new Item.Properties().stacksTo(16));}
    @Override public int getUseDuration(ItemStack stack,LivingEntity user){return 40;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(!(entity instanceof ServerPlayer p)||!CoreRuntime.active(p))return stack;
        var hit=p.pick(5,0,false);
        if(!(hit instanceof net.minecraft.world.phys.BlockHitResult block)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return stack;
        var pos=block.getBlockPos();var state=level.getBlockState(pos);
        if(!level.mayInteract(p,pos)||!p.mayUseItemAt(pos,block.getDirection(),stack))return stack;
        boolean campfire=CampfireBlock.canLight(state);
        if(campfire)level.setBlock(pos,state.setValue(CampfireBlock.LIT,true),11);
        else if(level.random.nextInt(100)<Math.min(100,5*CoreRuntime.data(p).track(com.faust.core.progression.Skill.building).level)){
            var firePos=pos.relative(block.getDirection());
            if(!p.mayUseItemAt(firePos,block.getDirection(),stack))return stack;
            if(BaseFireBlock.canBePlacedAt(level,firePos,p.getDirection()))level.setBlock(firePos,BaseFireBlock.getState(level,firePos),11);
        }
        if(!p.getAbilities().instabuild)stack.shrink(1);
        return stack;
    }
}
