package dev.hidenseek.mixin;
import dev.hidenseek.StillHunt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class EntityMixin {
 @Inject(method="move",at=@At("HEAD"),cancellable=true)
 private void stopMove(MovementType type,Vec3d movement,CallbackInfo ci){if(StillHunt.movementBlocked((Entity)(Object)this))ci.cancel();}
 @Inject(method="setPosition(DDD)V",at=@At("HEAD"),cancellable=true)
 private void stopPosition(double x,double y,double z,CallbackInfo ci){if(StillHunt.locked((Entity)(Object)this))ci.cancel();}
 @Inject(method="setVelocity(Lnet/minecraft/util/math/Vec3d;)V",at=@At("HEAD"),cancellable=true)
 private void stopVelocity(Vec3d v,CallbackInfo ci){if(StillHunt.locked((Entity)(Object)this))ci.cancel();}
 @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
 private void noPush(CallbackInfoReturnable<Boolean> ci){Entity e=(Entity)(Object)this;if(StillHunt.locked(e)||StillHunt.temporary(e))ci.setReturnValue(false);}
 @Inject(method="canAvoidTraps",at=@At("HEAD"),cancellable=true)
 private void noTraps(CallbackInfoReturnable<Boolean> ci){if(StillHunt.temporary((Entity)(Object)this))ci.setReturnValue(true);}
 @Inject(method="setSprinting",at=@At("HEAD"),cancellable=true)
 private void noSprint(boolean sprint,CallbackInfo ci){if(sprint&&StillHunt.noSprint((Entity)(Object)this))ci.cancel();}
}
