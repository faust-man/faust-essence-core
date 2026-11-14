package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(SculkShriekerBlockEntity.class)
public class WardenNoiseMixin {
 @Unique private java.util.UUID core$noisePlayer;
 @Inject(method="shriek",at=@At("HEAD")) private void core$source(ServerLevel level,net.minecraft.world.entity.Entity entity,CallbackInfo ci){core$noisePlayer=entity instanceof ServerPlayer p?p.getUUID():null;}
 @Inject(method="saveAdditional",at=@At("TAIL")) private void core$save(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,CallbackInfo ci){if(core$noisePlayer!=null)tag.putUUID("faustcore_noise_player",core$noisePlayer);}
 @Inject(method="loadAdditional",at=@At("TAIL")) private void core$load(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,CallbackInfo ci){core$noisePlayer=tag.hasUUID("faustcore_noise_player")?tag.getUUID("faustcore_noise_player"):null;}
 @Inject(method="trySummonWarden",at=@At("RETURN")) private void core$spawned(ServerLevel level,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue()&&core$noisePlayer!=null){var p=level.getServer().getPlayerList().getPlayer(core$noisePlayer);if(p!=null&&CoreRuntime.active(p))CoreRuntime.data(p).recordMaximum("warden_noise",1,CoreRuntime.balance);}}
}
