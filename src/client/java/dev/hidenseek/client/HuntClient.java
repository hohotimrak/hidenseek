package dev.hidenseek.client;

import com.google.gson.*;
import dev.hidenseek.Wire;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import com.mojang.authlib.GameProfile;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class HuntClient implements ClientModInitializer {
 public static JsonObject view=new JsonObject();
 public static Vec3d anchor;
 public static final Map<String,JsonObject> terrain=new HashMap<>();
 private boolean pressed;
 private OtherClientPlayerEntity camera;
 public void onInitializeClient(){
  ClientPlayNetworking.registerGlobalReceiver(Wire.View.ID,(payload,ctx)->ctx.client().execute(()->{
   String old=string(view,"phase",""),oldId=string(view,"id","");view=JsonParser.parseString(payload.json()).getAsJsonObject();if(!oldId.equals(string(view,"id","")))terrain.clear();
   if(view.has("anchor")){var a=view.getAsJsonObject("anchor");anchor=new Vec3d(number(a,"x",0),number(a,"y",0),number(a,"z",0));}
   if(string(view,"phase","").equals("FINISHED")&&!old.equals("FINISHED")){ImageCapture.clear();anchor=null;}
  }));
  ClientPlayNetworking.registerGlobalReceiver(Wire.Tiles.ID,(payload,ctx)->ctx.client().execute(()->{var data=JsonParser.parseString(payload.json()).getAsJsonObject();if(!string(data,"match","").equals(string(view,"id","")))return;for(var t:data.getAsJsonArray("tiles")){var tile=t.getAsJsonObject();terrain.put(tile.get("x").getAsInt()+":"+tile.get("z").getAsInt(),tile);}}));
  ClientPlayNetworking.registerGlobalReceiver(Wire.ImagePart.ID,(payload,ctx)->ctx.client().execute(()->ImageCapture.receive(payload)));
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{ImageCapture.clear();view=new JsonObject();anchor=null;camera=null;terrain.clear();});
  ClientTickEvents.END_CLIENT_TICK.register(client->{
   if(Boolean.getBoolean("stillhunt.uiCheck"))ClientUiCheck.tick(client);
   if(client.player==null||client.world==null)return;
   ImageCapture.tick(client);
   boolean down=GLFW.glfwGetKey(client.getWindow().getHandle(),GLFW.GLFW_KEY_H)==GLFW.GLFW_PRESS;
   if(down&&!pressed&&client.currentScreen==null&&view.has("role"))client.setScreen(new HuntScreen());pressed=down;
   String phase=string(view,"phase","");boolean active=phase.equals("SEEKING")||phase.equals("PAUSED");
   if(active&&string(view,"role","").equals("hider")&&anchor!=null){client.player.setVelocity(Vec3d.ZERO);client.player.setPosition(anchor);}
   if(active&&view.has("camera")){
    String target=view.get("camera").getAsString();JsonObject pose=null;
    for(var value:view.getAsJsonArray("positions"))if(value.getAsJsonObject().get("id").getAsString().equals(target))pose=value.getAsJsonObject();
    if(pose!=null){
     if(camera==null)camera=new OtherClientPlayerEntity(client.world,new GameProfile(UUID.randomUUID(),"StillHuntCamera"));
     camera.refreshPositionAndAngles(pose.get("x").getAsDouble(),pose.get("y").getAsDouble(),pose.get("z").getAsDouble(),pose.get("yaw").getAsFloat(),pose.get("pitch").getAsFloat());
     camera.setHeadYaw(camera.getYaw());client.setCameraEntity(camera);
    }
   }else if(camera!=null){client.setCameraEntity(client.player);camera=null;}
  });
 }
 public static boolean frozen(){return anchor!=null&&string(view,"role","").equals("hider")&&Set.of("SEEKING","PAUSED").contains(string(view,"phase",""));}
 public static boolean effect(String id){return view.has("participant")&&view.getAsJsonObject("participant").getAsJsonObject("effects").has(id)&&view.getAsJsonObject("participant").getAsJsonObject("effects").get(id).getAsLong()>number(view,"tick",0);}
 public static String string(JsonObject o,String key,String fallback){return o.has(key)?o.get(key).getAsString():fallback;}
 public static double number(JsonObject o,String key,double fallback){return o.has(key)?o.get(key).getAsDouble():fallback;}
 public static void intent(JsonObject o){if(!view.has("id"))return;o.addProperty("match",view.get("id").getAsString());ClientPlayNetworking.send(new Wire.Intent(o.toString()));}
 public static JsonObject action(String action){var o=new JsonObject();o.addProperty("action",action);return o;}
}
