package com.faust.core.mixin;
import com.faust.core.runtime.MovementPerks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.MagmaBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(MagmaBlock.class)
public class MagmaPerkMixin {
 @Redirect(method="stepOn",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
 private boolean core$magma(Entity entity,DamageSource source,float amount){return !MovementPerks.softFoot(entity)&&entity.hurt(source,amount);}
}
