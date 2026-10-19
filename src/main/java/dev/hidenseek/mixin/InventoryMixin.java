package dev.hidenseek.mixin;
import dev.hidenseek.StillHunt;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerEntity.class)
public abstract class InventoryMixin {
 @Inject(method="dropInventory",at=@At("HEAD"),cancellable=true)
 private void keepRemainingKit(CallbackInfo ci){var mod=StillHunt.INSTANCE;if(mod!=null&&mod.match.active()&&mod.match.seeker(((PlayerEntity)(Object)this).getUuid()))ci.cancel();}
}
