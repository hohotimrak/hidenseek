package dev.hidenseek.client.mixin;
import dev.hidenseek.client.HuntClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerMixin {
 @Inject(method="move",at=@At("HEAD"),cancellable=true)
 private void frozen(MovementType type,Vec3d movement,CallbackInfo ci){if(HuntClient.frozen())ci.cancel();}
}
