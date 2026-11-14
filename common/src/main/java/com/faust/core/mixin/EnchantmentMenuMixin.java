package com.faust.core.mixin;
import com.faust.core.runtime.CoreRuntime;
import com.faust.core.progression.PracticeBonuses;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Unique private Inventory core$inventory;
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at=@At("TAIL"))
    private void core$owner(int id,Inventory inventory,ContainerLevelAccess access,CallbackInfo ci){core$inventory=inventory;}
    @Redirect(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"))
    private int core$power(net.minecraft.util.RandomSource random,int slot,int shelves,net.minecraft.world.item.ItemStack stack){
        int bonus=core$inventory!=null&&core$inventory.player instanceof ServerPlayer p&&CoreRuntime.active(p)?PracticeBonuses.shelves(CoreRuntime.data(p),CoreRuntime.balance):0;
        if(bonus<=0)return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentCost(random,slot,shelves,stack);
        if(stack.getItem().getEnchantmentValue()<=0)return 0;
        int power=Math.min(15,shelves)+bonus;
        int value=random.nextInt(8)+1+(power>>1)+random.nextInt(power+1);
        return slot==0?Math.max(value/3,1):slot==1?value*2/3+1:Math.max(value,power*2);
    }
}
