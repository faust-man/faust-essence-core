package com.faust.core.runtime;
import com.faust.core.progression.PracticeBonuses;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import java.util.*;
public final class BrewingRewards {
    private BrewingRewards() {}
    public static ItemStack unboosted(ItemStack input){
        var data=input.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(!data.contains("faustcore:duration_effects"))return input;
        var ids=data.getList("faustcore:duration_effects",8);var contents=input.get(DataComponents.POTION_CONTENTS);
        ItemStack copy=input.copy();
        if(contents!=null)copy.set(DataComponents.POTION_CONTENTS,new PotionContents(contents.potion(),contents.customColor(),contents.customEffects().stream().filter(e->{String id=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value()).toString();for(int i=0;i<ids.size();i++)if(ids.getString(i).equals(id))return false;return true;}).toList()));
        data.remove("faustcore:duration_effects");copy.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));return copy;
    }
    public static boolean recipeChanged(ItemStack before,ItemStack after){
        if(after.isEmpty())return false;
        if(before.getItem()!=after.getItem())return true;
        return !before.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).potion().equals(after.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).potion());
    }
    /** Tooltip uses the effective override instead of showing both durations. */
    public static PotionContents displayed(ItemStack stack){
        var contents=stack.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(!tag.contains("faustcore:duration_effects"))return contents;
        var effects=new ArrayList<MobEffectInstance>();
        contents.potion().ifPresent(potion->{for(var base:potion.value().getEffects())if(contents.customEffects().stream().noneMatch(e->e.getEffect().equals(base.getEffect())))effects.add(base);});
        effects.addAll(contents.customEffects());
        return new PotionContents(Optional.empty(),contents.customColor(),effects);
    }
    public static void complete(ServerPlayer player,ItemStack result){
        double multiplier=PracticeBonuses.duration(CoreRuntime.data(player),CoreRuntime.balance);
        extend(result,multiplier);
        CoreRuntime.grant(player,"brew",1,1);
    }
    public static void extend(ItemStack result,double multiplier){
        var contents=result.get(DataComponents.POTION_CONTENTS);
        if(contents!=null&&contents.potion().isPresent()&&multiplier>1){
            var effects=new ArrayList<MobEffectInstance>();
            for(var effect:contents.potion().get().value().getEffects())if(!effect.getEffect().value().isInstantenous()&&effect.getDuration()>0)effects.add(new MobEffectInstance(effect.getEffect(),(int)Math.min(Integer.MAX_VALUE,Math.floor(effect.getDuration()*multiplier)),effect.getAmplifier(),effect.isAmbient(),effect.isVisible(),effect.showIcon()));
            for(var effect:contents.customEffects())if(contents.potion().get().value().getEffects().stream().noneMatch(base->base.getEffect().equals(effect.getEffect())))effects.add(new MobEffectInstance(effect));
            var ids=new net.minecraft.nbt.ListTag();
            for(var effect:effects)if(contents.potion().get().value().getEffects().stream().anyMatch(e->e.getEffect().equals(effect.getEffect())))ids.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString()));
            var tag=result.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();tag.put("faustcore:duration_effects",ids);result.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            result.set(DataComponents.POTION_CONTENTS,new PotionContents(contents.potion(),contents.customColor(),effects));
        }
    }
}
