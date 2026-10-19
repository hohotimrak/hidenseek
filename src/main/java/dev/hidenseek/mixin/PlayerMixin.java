package dev.hidenseek.mixin;
import dev.hidenseek.StillHunt;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.TeleportTarget;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayerEntity.class)
public abstract class PlayerMixin {
 @Inject(method="copyFrom",at=@At("TAIL"))
 private void preserveInventory(ServerPlayerEntity old,boolean alive,CallbackInfo ci){var p=(ServerPlayerEntity)(Object)this;var mod=StillHunt.INSTANCE;if(mod!=null&&mod.match.active()&&mod.match.seeker(p.getUuid()))p.getInventory().clone(old.getInventory());}
 @Inject(method="teleportTo(Lnet/minecraft/world/TeleportTarget;)Lnet/minecraft/server/network/ServerPlayerEntity;",at=@At("HEAD"),cancellable=true)
 private void blockTeleport(TeleportTarget target,CallbackInfoReturnable<ServerPlayerEntity> ci){var p=(ServerPlayerEntity)(Object)this;var mod=StillHunt.INSTANCE;if(StillHunt.locked(p)||mod!=null&&!mod.restoring&&mod.match.active()&&mod.match.participants.containsKey(p.getUuid())&&!target.world().getRegistryKey().getValue().toString().equals(mod.match.dimension))ci.setReturnValue(null);}
 @Inject(method="startRiding",at=@At("HEAD"),cancellable=true)
 private void blockMount(Entity mount,boolean force,CallbackInfoReturnable<Boolean> ci){if(StillHunt.locked((Entity)(Object)this))ci.setReturnValue(false);}
}
