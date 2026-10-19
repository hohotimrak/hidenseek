package dev.hidenseek.client;
import dev.hidenseek.Wire;
import dev.hidenseek.core.ImageTransfer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.Identifier;
import java.util.*;

public final class ImageCapture {
 private static String request="",completed="";
 private static int frames;
 private static long readyAt;
 private static boolean oldHud;
 private static Screen oldScreen;
 public static Identifier image;
 private static ImageTransfer incoming;
 private static String incomingId="";
 public static void tick(MinecraftClient client){
  String next=HuntClient.string(HuntClient.view,"imageRequest","");
  if(!next.isEmpty()&&!next.equals(completed)&&request.isEmpty()&&client.player!=null){
   request=next;oldHud=client.options.hudHidden;oldScreen=client.currentScreen;
   client.setScreen(null);client.options.hudHidden=true;client.setCameraEntity(client.player);
   // Allow body chunks and rendering to settle before capture.
   frames=1;readyAt=System.nanoTime()+3_000_000_000L;
  }
 }
 public static void rendered(){
  if(request.isEmpty())return;var client=MinecraftClient.getInstance();
  if(client.player==null||!HuntClient.view.has("imageRequest")){restore(client);return;}
  client.setCameraEntity(client.player);client.options.hudHidden=true;
  if(System.nanoTime()<readyAt||--frames>0)return;String id=request,match=HuntClient.string(HuntClient.view,"id","");completed=id;
  ScreenshotRecorder.takeScreenshot(client.getFramebuffer(),full->{
   try(full;var small=new NativeImage(ImageTransfer.WIDTH,ImageTransfer.HEIGHT,false)){
    full.resizeSubRectTo(0,0,full.getWidth(),full.getHeight(),small);byte[] rgb=new byte[ImageTransfer.LENGTH];
    for(int y=0;y<ImageTransfer.HEIGHT;y++)for(int x=0;x<ImageTransfer.WIDTH;x++){int color=small.getColorArgb(x,y),offset=(y*ImageTransfer.WIDTH+x)*3;rgb[offset]=(byte)(color>>16);rgb[offset+1]=(byte)(color>>8);rgb[offset+2]=(byte)color;}
    client.execute(()->{if(!match.equals(HuntClient.string(HuntClient.view,"id","")))return;for(int i=0;i<ImageTransfer.COUNT;i++)ClientPlayNetworking.send(new Wire.ImagePart(match,id,i,ImageTransfer.COUNT,Arrays.copyOfRange(rgb,i*ImageTransfer.CHUNK,Math.min(rgb.length,(i+1)*ImageTransfer.CHUNK))));});
   }
  });
  restore(client);
 }
 private static void restore(MinecraftClient client){client.options.hudHidden=oldHud;if(client.player!=null)client.setScreen(oldScreen);oldScreen=null;request="";}
 public static void receive(Wire.ImagePart part){
  if(!part.match().equals(HuntClient.string(HuntClient.view,"id","")))return;
  if(!part.request().equals(incomingId)){incomingId=part.request();incoming=new ImageTransfer();}
  if(incoming.accept(part.index(),part.total(),part.data())){
   byte[] rgb=incoming.bytes();var pixels=new NativeImage(ImageTransfer.WIDTH,ImageTransfer.HEIGHT,false);
   for(int y=0;y<ImageTransfer.HEIGHT;y++)for(int x=0;x<ImageTransfer.WIDTH;x++){int o=(y*ImageTransfer.WIDTH+x)*3;pixels.setColorArgb(x,y,0xff000000|(rgb[o]&255)<<16|(rgb[o+1]&255)<<8|(rgb[o+2]&255));}
   var client=MinecraftClient.getInstance();if(image!=null)client.getTextureManager().destroyTexture(image);
   image=Identifier.of("hidenseek","latest_image");client.getTextureManager().registerTexture(image,new NativeImageBackedTexture(()->"Still Hunt clue",pixels));
   client.setScreen(new ImageScreen());incoming=null;incomingId="";
  }
 }
 public static void clear(){var client=MinecraftClient.getInstance();if(!request.isEmpty())restore(client);if(image!=null)client.getTextureManager().destroyTexture(image);image=null;incoming=null;completed="";}
}
