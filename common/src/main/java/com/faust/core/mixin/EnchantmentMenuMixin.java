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
    @ModifyArg(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"),index=2)
    private int core$power(int shelves){return shelves+(core$inventory!=null&&core$inventory.player instanceof ServerPlayer p&&CoreRuntime.active(p)?PracticeBonuses.shelves(CoreRuntime.data(p),CoreRuntime.balance):0);}
}
