package dev.hidenseek.client.mixin;
import dev.hidenseek.client.ImageCapture;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class RenderMixin {
 @Inject(method="render",at=@At("RETURN"))
 private void capture(CallbackInfo ci){ImageCapture.rendered();}
}
