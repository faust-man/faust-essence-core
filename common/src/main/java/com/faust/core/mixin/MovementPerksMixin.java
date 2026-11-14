package com.faust.core.mixin;
import com.faust.core.runtime.MovementPerks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Entity.class)
public abstract class MovementPerksMixin {
 @Redirect(method="getBlockSpeedFactor",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;getSpeedFactor()F"))
 private float core$soulSand(Block block){return block==Blocks.SOUL_SAND&&MovementPerks.softFoot((Entity)(Object)this)?1:block.getSpeedFactor();}
 @ModifyVariable(method="vibrationAndSoundEffectsFromBlock",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private boolean core$stepSound(boolean enabled){return enabled&&!MovementPerks.silent((Entity)(Object)this);}
 @ModifyVariable(method="vibrationAndSoundEffectsFromBlock",at=@At("HEAD"),argsOnly=true,ordinal=1)
 private boolean core$stepVibration(boolean enabled){return enabled&&!MovementPerks.silent((Entity)(Object)this);}
}
