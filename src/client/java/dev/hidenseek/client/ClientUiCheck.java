package dev.hidenseek.client;
import dev.hidenseek.Storage;
import dev.hidenseek.core.*;
import dev.hidenseek.core.Geometry.*;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import java.util.*;

/** Opt-in rendering fixtures. These are explicitly NOT network/playtest evidence. */
public final class ClientUiCheck {
 private static int ticks,stage=-1;private static boolean saving;private static Match fixture;private static Rules rules;private static UUID h,s;
 public static void tick(MinecraftClient client){
  if(stage<0){if(client.getOverlay()!=null||++ticks<100)return;
   rules=new Rules();h=UUID.randomUUID();s=UUID.randomUUID();fixture=new Match();fixture.start(h,List.of(s,UUID.randomUUID()),new Point(0,64,0),-512,-512,"minecraft:overworld",rules);fixture.lock(new Point(100,64,0));fixture.constraints.add(new Constraint("fixture_east","east",0,0,0,0,true,0));fixture.reward(3,rules);show(client,0);return;
  }
  if(saving||++ticks<60)return;saving=true;
  ScreenshotRecorder.saveScreenshot(client.runDirectory,"stillhunt-ui-"+stage+".png",client.getFramebuffer(),1,text->client.execute(()->{
   if(stage>=3){System.out.println("STILLHUNT_UI_FIXTURES_COMPLETE: rendered 4 panels, no gameplay asserted");client.scheduleStop();}
   else{show(client,stage+1);saving=false;}
  }));
 }
 private static void show(MinecraftClient client,int next){stage=next;ticks=0;HuntClient.view=Storage.JSON.toJsonTree(fixture.view(next>=2?h:s,rules)).getAsJsonObject();client.setScreen(new HuntScreen(next));}
}
