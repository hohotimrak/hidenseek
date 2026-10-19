package dev.hidenseek;
import dev.hidenseek.core.*;
import dev.hidenseek.core.Geometry.Point;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import java.util.*;

/** Opt-in headless integration checks, on an isolated server. Not a multiplayer playtest. */
public final class RuntimeChecks {
 public static void run(StillHunt mod){
  if(mod.match.active())throw new IllegalStateException("Smoke checks need an inactive isolated world");
  Match old=mod.match;
  try{
   var world=mod.server.getOverworld();UUID h=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();
   var player=new ServerPlayerEntity(mod.server,world,new GameProfile(h,"SmokeHider"),SyncedClientOptions.createDefault());
   player.setPosition(8,80,8);mod.match=new Match();mod.match.start(h,List.of(a,b),new Point(0,80,0),-512,-512,"minecraft:overworld",mod.rules);mod.match.lock(new Point(8,80,8));
   Vec3d initial=player.getPos();player.setPosition(25,90,25);check(player.getPos().equals(initial),"setPosition lock");
   player.move(MovementType.PISTON,new Vec3d(1,1,1));check(player.getPos().equals(initial),"piston movement lock");
   player.setVelocity(new Vec3d(5,5,5));check(player.getVelocity().equals(Vec3d.ZERO),"velocity lock");
   var result=player.teleportTo(new TeleportTarget(world,new Vec3d(40,80,40),Vec3d.ZERO,0,0,TeleportTarget.NO_OP));check(result==null&&player.getPos().equals(initial),"teleport lock");
   check(!player.isPushable(),"unpushable body");
   check(!player.damage(world,world.getDamageSources().generic(),100),"environment damage denied");
   mod.match.pause();player.setPosition(50,80,50);check(player.getPos().equals(initial),"pause retains anchor");
   String serialized=Storage.JSON.toJson(mod.match);Match restored=Storage.JSON.fromJson(serialized,Match.class);check(restored.hide.equals(mod.match.hide)&&restored.phase==Match.Phase.PAUSED,"save round-trip");
   mod.match.finish("admin");player.setPosition(12,80,12);check(player.getX()==12,"end releases anchor");
   StillHunt.LOG.info("STILLHUNT_SMOKE_PASS: 9 runtime checks; no real clients used");
  }finally{mod.match=old;}
 }
 private static void check(boolean pass,String label){if(!pass)throw new AssertionError("Runtime smoke failed: "+label);}
}
