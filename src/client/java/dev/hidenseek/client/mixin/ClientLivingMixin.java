package dev.hidenseek.client.mixin;
import dev.hidenseek.client.HuntClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class ClientLivingMixin {
 @Inject(method="canGlide",at=@At("HEAD"),cancellable=true)
 private void wings(CallbackInfoReturnable<Boolean> ci){if((Object)this==MinecraftClient.getInstance().player&&HuntClient.effect("flight"))ci.setReturnValue(true);}
}
