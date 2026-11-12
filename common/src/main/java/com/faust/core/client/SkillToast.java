package com.faust.core.client;
import net.minecraft.client.gui.components.toasts.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
public record SkillToast(Component name,ItemStack icon) implements Toast {
 @Override public int width(){return Math.max(160,net.minecraft.client.Minecraft.getInstance().font.width(name)+38);}
 @Override public Visibility render(GuiGraphics g,ToastComponent toasts,long time){
  g.blitSprite(net.minecraft.resources.ResourceLocation.withDefaultNamespace("toast/advancement"),0,0,width(),height());
  g.drawString(toasts.getMinecraft().font,Component.translatable("core.skill.available"),30,7,0xFFFFFF00,false);
  g.drawString(toasts.getMinecraft().font,name,30,18,0xFFFFFFFF,false);g.renderItem(icon,8,8);
  return time>=5000*toasts.getNotificationDisplayTimeMultiplier()?Visibility.HIDE:Visibility.SHOW;
 }
}
