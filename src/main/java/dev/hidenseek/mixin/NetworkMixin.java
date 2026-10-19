package dev.hidenseek.mixin;
import dev.hidenseek.StillHunt;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayNetworkHandler.class)
public abstract class NetworkMixin {
 @Shadow public ServerPlayerEntity player;
 @Inject(method="onPlayerMove",at=@At(value="INVOKE",target="Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/server/world/ServerWorld;)V",shift=At.Shift.AFTER),cancellable=true)
 private void freeze(PlayerMoveC2SPacket packet,CallbackInfo ci){if(StillHunt.movementBlocked(player)){
  float yaw=packet.getYaw(player.getYaw()),pitch=packet.getPitch(player.getPitch());
  if(Float.isFinite(yaw)&&Float.isFinite(pitch)){player.setYaw(yaw);player.setPitch(Math.clamp(pitch,-90,90));}ci.cancel();
 }}
}
