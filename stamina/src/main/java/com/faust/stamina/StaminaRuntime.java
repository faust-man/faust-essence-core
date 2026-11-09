package com.faust.stamina;
import com.faust.core.runtime.CoreRuntime;
import com.faust.core.progression.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Server resource simulation; no work runs when the optional mod is absent. */
public final class StaminaRuntime {
    private StaminaRuntime(){}
    public static void tick(ServerPlayer player,Progress p,CoreRuntime.Session s,Balance b) {
        if(b.stamina.get("enabled")>0&&p.actionsBlocked(b))player.setSprinting(false);
        boolean moving=s.lastPosition!=null&&s.lastPosition.distanceToSqr(player.position())>0.0001;
        if(moving)s.movedTick=player.tickCount;
        s.lastPosition=player.position();
        double armorRate=player.isSprinting()&&player.tickCount-s.ranTick<=1?b.stamina.get("armorCost")*Math.max(0,CoreRuntime.weight(player)-p.capacity(b)):0;
        Vec3 flow=player.level().getFluidState(player.blockPosition()).getFlow(player.level(),player.blockPosition());
        Vec3 look=Vec3.directionFromRotation(0,player.getYRot());
        // Server movement input plus facing direction; passive water movement alone does not qualify.
        boolean upstream=player.isSwimming()&&!player.isPassenger()&&s.forward&&player.tickCount-s.inputTick<=20&&look.dot(flow)<-b.stamina.get("flowThreshold");
        double waterRate=upstream?b.stamina.get("waterCost")*Math.max(0,-look.dot(flow)):0;
        double runRate=player.isSprinting()&&player.tickCount-s.ranTick<=1?b.stamina.get("runCost"):0;
        if(player.isSwimming()&&player.isSprinting()&&moving)s.swimTicks++;else s.swimTicks=0;
        double swimRate=s.swimTicks>b.stamina.get("swimDelaySeconds")*20?b.stamina.get("swimCost"):0;
        double rate=runRate+armorRate+waterRate+swimRate;
        if(p.stamina>0&&rate>0&&b.stamina.get("enabled")>0) {p.spend(rate/20,b,player.serverLevel().getServer().overworld().getGameTime());s.lastSpent=player.tickCount;}
        if(player.tickCount%20==0) {
            s.campfire=false;s.soulCampfire=false;
            {
                int r=(int)Math.ceil(b.fatigue.get("campfireRadius"));
                BlockPos origin=player.blockPosition();
                for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-r,-r,-r),origin.offset(r,r,r))) {
                    if(!player.level().hasChunkAt(pos)||pos.distSqr(origin)>r*r)continue;
                    var state=player.level().getBlockState(pos);
                    if(state.getBlock() instanceof CampfireBlock&&state.getValue(CampfireBlock.LIT)){if(state.is(Blocks.SOUL_CAMPFIRE))s.soulCampfire=true;else s.campfire=true;}
                }
            }
        }
        if(!player.isOnFire())p.soulFireBurn=false;else if(player.level().getBlockState(player.blockPosition()).is(Blocks.SOUL_FIRE)||player.level().getBlockState(player.blockPosition().below()).is(Blocks.SOUL_FIRE))p.soulFireBurn=true;
        p.environmentalFatigue(0.05,s.soulCampfire,p.soulFireBurn&&player.isOnFire(),b,player.serverLevel().getServer().overworld().getGameTime());
        boolean shielding=player.isUsingItem()&&player.getUseItem().is(Items.SHIELD);
        boolean airVisible=com.faust.core.progression.Breathing.visible(player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER),player.getAirSupply(),player.getMaxAirSupply())||(s.airVisible&&player.tickCount-s.inputTick<=20);
        p.recover(0.05,player.isSleeping(),s.campfire&&player.tickCount-s.movedTick>5,player.tickCount-s.lastSpent>=b.stamina.get("delaySeconds")*20,shielding,player.isFallFlying(),airVisible,player.isSwimming()&&player.isSprinting(),b);
        if(p.actionsBlocked(b)&&shielding)player.stopUsingItem();
        if(b.stamina.get("enabled")>0&&p.actionsBlocked(b))player.setSprinting(false);
        if(b.stamina.get("enabled")>0&&p.actionsBlocked(b)&&upstream)player.setSprinting(false);
    }
}
