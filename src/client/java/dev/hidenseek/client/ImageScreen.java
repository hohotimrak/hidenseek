package dev.hidenseek.client;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.text.Text;
public class ImageScreen extends Screen {
 public ImageScreen(){super(Text.translatable("hint.hidenseek.screenshot"));}
 public boolean shouldPause(){return false;}
 protected void init(){addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(width/2-40,height-30,80,20).build());}
 public void render(DrawContext ctx,int mx,int my,float delta){ctx.fill(0,0,width,height,0xff101820);ctx.drawCenteredTextWithShadow(textRenderer,title,width/2,12,0xffffffff);if(ImageCapture.image!=null){int w=Math.min(width-24,(height-70)*16/9),h=w*9/16;ctx.drawTexture(RenderPipelines.GUI_TEXTURED,ImageCapture.image,(width-w)/2,32,0,0,w,h,320,180,320,180);}super.render(ctx,mx,my,delta);}
}
