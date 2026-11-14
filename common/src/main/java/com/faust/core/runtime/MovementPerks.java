package com.faust.core.runtime;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
public final class MovementPerks {
 private MovementPerks(){}
 public static boolean softFoot(Entity entity){return entity.level().isClientSide?ClientActions.softFoot.test(entity):enabled(entity,"soft_foot");}
 public static boolean silent(Entity entity){return entity.level().isClientSide?ClientActions.silentStep.test(entity):enabled(entity,"silent_step");}
 private static boolean enabled(Entity entity,String id){return entity instanceof ServerPlayer p&&CoreRuntime.active(p)&&CoreRuntime.data(p).enabled("faustcore:"+id,CoreRuntime.balance);}
}
