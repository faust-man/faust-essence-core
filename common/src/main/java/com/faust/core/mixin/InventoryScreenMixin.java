package com.faust.core.mixin;
import com.faust.core.client.PanelTabs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    protected InventoryScreenMixin(InventoryMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Inject(method="render",at=@At("TAIL"))
    private void core$tabs(GuiGraphics g,int mx,int my,float delta,CallbackInfo ci){PanelTabs.draw(g,leftPos,topPos,false,mx,my);}
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void core$click(double mx,double my,int button,CallbackInfoReturnable<Boolean> ci){if(PanelTabs.click(mx,my,button,leftPos,topPos,false))ci.setReturnValue(true);}
}
