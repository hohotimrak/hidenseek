package dev.hidenseek.mixin;
import dev.hidenseek.StillHunt;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class LivingMixin {
 @Inject(method="tickGliding",at=@At("HEAD"),cancellable=true)
 private void virtualEquipment(CallbackInfo ci){var e=(LivingEntity)(Object)this;var mod=StillHunt.INSTANCE;if(mod!=null&&!e.getWorld().isClient&&mod.match.active()&&mod.match.effect(e.getUuid(),"flight"))ci.cancel();}
 @Inject(method="canGlide",at=@At("HEAD"),cancellable=true)
 private void pocketWings(CallbackInfoReturnable<Boolean> ci){var e=(LivingEntity)(Object)this;var mod=StillHunt.INSTANCE;if(mod!=null&&!e.getWorld().isClient&&mod.match.active()&&mod.match.effect(e.getUuid(),"flight")&&!e.isOnGround())ci.setReturnValue(true);}
 @Inject(method="jump",at=@At("HEAD"),cancellable=true)
 private void noJump(CallbackInfo ci){if(StillHunt.noJump((LivingEntity)(Object)this))ci.cancel();}
 @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
 private void noPush(CallbackInfoReturnable<Boolean> ci){var e=(LivingEntity)(Object)this;if(StillHunt.locked(e)||StillHunt.temporary(e))ci.setReturnValue(false);}
}
