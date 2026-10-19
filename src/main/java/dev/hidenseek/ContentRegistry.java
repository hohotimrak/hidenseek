package dev.hidenseek;

import dev.hidenseek.core.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Public addon API. Register during the Fabric initializer, before a server starts. */
public final class ContentRegistry {
 @FunctionalInterface public interface CardHandler { boolean apply(CardContext context); }
 @FunctionalInterface public interface TaskObjective { boolean onEvent(TaskContext context,String event,String value); }
 public record CardContext(StillHunt game,ServerPlayerEntity actor,UUID target,ContentCatalog.CardDefinition definition) {}
 public record TaskContext(StillHunt game,UUID player,ContentCatalog.TaskDefinition definition) {}
 private static final Map<String,CardHandler> CARDS=new LinkedHashMap<>();
 private static final Map<String,TaskObjective> TASKS=new LinkedHashMap<>();
 static {
  registerCardHandler("effect",ctx->{
   var data=ctx.game.match.participants.get(ctx.target);String effect=ctx.definition.parameters().get("effect");
   if(ctx.game.match.effect(ctx.target,effect))return false;
   data.effects.put(effect,ctx.game.match.tick+20L*ctx.definition.durationSeconds());return true;
  });
  registerCardHandler("task",ctx->{
   var data=ctx.game.match.participants.get(ctx.target);String id=ctx.definition.parameters().get("task");
   if(!data.taskId.isEmpty())return false;
   var task=ctx.game.rules.content.tasks.get(id);if(task==null||!ctx.game.rules.content.profile.containsAll(task.requires()))return false;
   data.taskId=id;data.taskProgress=0;data.taskStarted=ctx.game.match.tick;data.taskExpires=ctx.game.match.tick+20L*task.timeoutSeconds();
   data.taskEffect=ctx.definition.parameters().get("effect");data.effects.put(data.taskEffect,data.taskExpires);return true;
  });
  registerCardHandler("decoy",ctx->ctx.game.spawnDecoys(Integer.parseInt(ctx.definition.parameters().get("count")),ctx.definition.durationSeconds()));
  registerTaskObjective("break_block",(ctx,event,value)->{
   if(!event.equals("break_block")||!value.equals(ctx.definition.target()))return false;
   var p=ctx.game.match.participants.get(ctx.player);return ++p.taskProgress>=ctx.definition.count();
  });
  registerTaskObjective("visit_waiting_point",(ctx,event,value)->{
   if(!event.equals("tick"))return false;var p=ctx.game.server.getPlayerManager().getPlayer(ctx.player);var at=ctx.game.match.waiting;
   return p!=null&&p.squaredDistanceTo(at.x(),at.y(),at.z())<9;
  });
 }
 public static void registerCardHandler(String id,CardHandler handler){ContentCatalog.checkId(id);if(CARDS.putIfAbsent(id,Objects.requireNonNull(handler))!=null)throw new IllegalArgumentException("Duplicate card handler: "+id);}
 public static void registerTaskObjective(String id,TaskObjective handler){ContentCatalog.checkId(id);if(TASKS.putIfAbsent(id,Objects.requireNonNull(handler))!=null)throw new IllegalArgumentException("Duplicate task objective: "+id);}
 public static Set<String> cardHandlers(){return Set.copyOf(CARDS.keySet());}
 public static Set<String> taskObjectives(){return Set.copyOf(TASKS.keySet());}
 public static boolean apply(CardContext context){return CARDS.get(context.definition.handler()).apply(context);}
 public static void taskEvent(StillHunt game,UUID player,String event,String value){
  var p=game.match.participants.get(player);if(p==null||p.taskId.isEmpty())return;
  var task=game.rules.content.tasks.get(p.taskId);if(task==null||p.taskExpires<=game.match.tick||TASKS.get(task.objective()).onEvent(new TaskContext(game,player,task),event,value))clearTask(p);
 }
 public static void clearTask(Match.Participant p){p.effects.remove(p.taskEffect);p.slots.remove("task");p.taskId="";p.taskProgress=0;p.taskEffect="";}
}
