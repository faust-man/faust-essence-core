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
    public FireStarterItem(){super(new Item.Properties().stacksTo(1));}
    @Override public int getUseDuration(ItemStack stack,LivingEntity user){return 40;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(!(entity instanceof ServerPlayer p)||!CoreRuntime.active(p))return stack;
        var hit=p.pick(5,1,false);
        if(!(hit instanceof net.minecraft.world.phys.BlockHitResult block)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return stack;
        var pos=block.getBlockPos();var state=level.getBlockState(pos);
        if(!level.mayInteract(p,pos)||!p.mayUseItemAt(pos,block.getDirection(),stack))return stack;
        boolean campfire=CampfireBlock.canLight(state);
        if(campfire)level.setBlock(pos,state.setValue(CampfireBlock.LIT,true),11);
        else if(level.random.nextInt(100)<Math.min(100,5*CoreRuntime.data(p).track(com.faust.core.progression.Skill.building).level)){
            if(block.getDirection()==net.minecraft.core.Direction.UP&&state.is(net.minecraft.tags.BlockTags.LOGS)&&CoreRuntime.data(p).enabled("faustcore:fire_starter",CoreRuntime.balance)&&PlacedLogs.get(p.serverLevel()).contains(pos)){
                level.setBlock(pos,Blocks.CAMPFIRE.defaultBlockState(),11);PlacedLogs.get(p.serverLevel()).add(pos);
            }else{
                boolean vegetation=state.getCollisionShape(level,pos).isEmpty()&&(((com.faust.core.mixin.FireBlockAccessor)Blocks.FIRE).core$igniteOdds(state)>0||state.is(net.minecraft.tags.BlockTags.SAPLINGS))&&!state.getFluidState().isSource();
                var firePos=vegetation?pos:pos.relative(block.getDirection());
                if(!p.mayUseItemAt(firePos,block.getDirection(),stack))return stack;
                if(vegetation)level.setBlock(firePos,BaseFireBlock.getState(level,firePos),11);
                else if(BaseFireBlock.canBePlacedAt(level,firePos,p.getDirection()))level.setBlock(firePos,BaseFireBlock.getState(level,firePos),11);
            }
        }
        if(!p.getAbilities().instabuild)stack.shrink(1);
        return stack;
    }
}
