package com.faust.core.mixin;

import com.faust.core.progression.Progress;
import com.faust.core.runtime.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements PlayerData {
    @Unique private Progress faustcore$data=new Progress();
    @Override public Progress coreData(){return faustcore$data;}
    @Override public void coreData(Progress data){faustcore$data=data;}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void core$save(CompoundTag tag,CallbackInfo ci){tag.putString("faustcore",faustcore$data.json());}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void core$load(CompoundTag tag,CallbackInfo ci){if(tag.contains("faustcore")){faustcore$data=Progress.parse(tag.getString("faustcore"),CoreRuntime.balance);faustcore$data.externalStaminaBonus=0;var player=(ServerPlayer)(Object)this;CoreRuntime.rebuildAttributes(player);if(tag.contains("Health"))player.setHealth(tag.getFloat("Health"));}}
    @Inject(method="restoreFrom",at=@At("TAIL"))
    private void core$copy(ServerPlayer previous,boolean keepEverything,CallbackInfo ci){faustcore$data=Progress.parse(CoreRuntime.data(previous).json());CoreRuntime.disconnected(previous);var player=(ServerPlayer)(Object)this;CoreRuntime.rebuildAttributes(player);if(!keepEverything)player.setHealth(player.getMaxHealth());}
    @Inject(method="tick",at=@At("TAIL"))
    private void core$tick(CallbackInfo ci){CoreRuntime.tick((ServerPlayer)(Object)this);}
    @Inject(method="awardStat(Lnet/minecraft/stats/Stat;I)V",at=@At("TAIL"))
    private void core$stat(Stat<?> stat,int amount,CallbackInfo ci){CoreRuntime.stat((ServerPlayer)(Object)this,stat,amount);}
    @Inject(method="openItemGui",at=@At("TAIL"))
    private void core$book(ItemStack stack,InteractionHand hand,CallbackInfo ci){CoreRuntime.read((ServerPlayer)(Object)this,stack);}
}
