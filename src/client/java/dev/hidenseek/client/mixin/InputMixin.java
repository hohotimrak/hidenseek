package dev.hidenseek.client.mixin;
import dev.hidenseek.client.HuntClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class InputMixin extends Input {
 @Inject(method="tick",at=@At("RETURN"))
 private void restrict(CallbackInfo ci){
  if(HuntClient.frozen()){playerInput=PlayerInput.DEFAULT;movementVector=Vec2f.ZERO;return;}
  var player=MinecraftClient.getInstance().player;
  boolean allowSwim=player!=null&&(player.isTouchingWater()||player.isClimbing());
  var p=playerInput;playerInput=new PlayerInput(p.forward(),p.backward(),p.left(),p.right(),p.jump()&&(!HuntClient.effect("boots")||allowSwim),p.sneak(),p.sprint()&&!HuntClient.effect("rhythm"));
 }
}
