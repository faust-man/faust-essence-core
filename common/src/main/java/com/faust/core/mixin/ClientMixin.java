package com.faust.core.mixin;
import com.faust.core.client.CoreClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class ClientMixin {
 @Inject(method="tick",at=@At("TAIL")) private void core$tick(CallbackInfo ci){CoreClient.tick();}
 @Inject(method="setLevel",at=@At("HEAD")) private void core$reset(ClientLevel level,net.minecraft.client.gui.screens.ReceivingLevelScreen.Reason reason,CallbackInfo ci){if(level==null)CoreClient.reset();}
}
