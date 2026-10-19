package dev.hidenseek;

import com.google.gson.*;
import dev.hidenseek.core.*;
import dev.hidenseek.core.Geometry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.command.argument.EntityArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.*;
import java.util.*;
import org.slf4j.*;
import static net.minecraft.server.command.CommandManager.*;

public class StillHunt implements ModInitializer {
 public static final Logger LOG=LoggerFactory.getLogger("StillHunt");
 public static StillHunt INSTANCE;
 public Match match=new Match();public Rules rules=new Rules();
 public MinecraftServer server;private Path savePath;
 private final Map<UUID,Long> lastIntent=new HashMap<>();
 private final Map<UUID,Long> temporary=new HashMap<>();
 private final Map<UUID,UUID> cameras=new HashMap<>();
 private final Map<UUID,CameraRelay> relays=new HashMap<>();
 private long ticks;
 private final TerrainMap terrain=new TerrainMap();
 private final Map<String,ImageTransfer> images=new HashMap<>();
 private final Map<UUID,Map<String,Object>> quotes=new HashMap<>();
 public boolean restoring;
 public static boolean locked(Entity e){return INSTANCE!=null&&e instanceof ServerPlayerEntity p&&INSTANCE.match.active()&&INSTANCE.match.hider(p.getUuid())&&INSTANCE.match.hide!=null&&!INSTANCE.restoring;}
 public static boolean movementBlocked(Entity e){return locked(e)||INSTANCE!=null&&e instanceof ServerPlayerEntity&&INSTANCE.match.phase==Match.Phase.PAUSED&&INSTANCE.match.participants.containsKey(e.getUuid());}
 public static boolean noJump(Entity e){return locked(e)||INSTANCE!=null&&INSTANCE.match.active()&&INSTANCE.match.effect(e.getUuid(),"boots");}
 public static boolean noSprint(Entity e){return locked(e)||INSTANCE!=null&&INSTANCE.match.active()&&INSTANCE.match.effect(e.getUuid(),"rhythm");}
 public static boolean temporary(Entity e){return e.getCommandTags().contains("stillhunt.temporary");}
 public void onInitialize(){
  INSTANCE=this;
  PayloadTypeRegistry.playC2S().register(Wire.Intent.ID,Wire.Intent.CODEC);PayloadTypeRegistry.playS2C().register(Wire.View.ID,Wire.View.CODEC);
  PayloadTypeRegistry.playS2C().register(Wire.Tiles.ID,Wire.Tiles.CODEC);
  PayloadTypeRegistry.playC2S().register(Wire.ImagePart.ID,Wire.ImagePart.CODEC);PayloadTypeRegistry.playS2C().register(Wire.ImagePart.ID,Wire.ImagePart.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(Wire.ImagePart.ID,(payload,ctx)->ctx.server().execute(()->receiveImage(ctx.player(),payload)));
  ServerPlayNetworking.registerGlobalReceiver(Wire.Intent.ID,(payload,ctx)->ctx.server().execute(()->handle(ctx.player(),payload.json())));
  ServerLifecycleEvents.SERVER_STARTED.register(s->{server=s;savePath=s.getSavePath(WorldSavePath.ROOT).resolve("stillhunt/match.json");try{
   Path config=FabricLoader.getInstance().getConfigDir().resolve("stillhunt.json");rules=Storage.read(config,Rules.class,new Rules());rules.validate();Storage.write(config,rules);
   Path contentPath=FabricLoader.getInstance().getConfigDir().resolve("stillhunt-content.json");rules.content=Storage.read(contentPath,ContentCatalog.class,ContentCatalog.defaults());rules.content.validate(ContentRegistry.cardHandlers(),ContentRegistry.taskObjectives());Storage.write(contentPath,rules.content);
   for(String hint:rules.tiers.keySet())if(!HintRegistry.has(hint))throw new IllegalArgumentException("Unregistered hint handler: "+hint);
   match=Storage.read(savePath,Match.class,new Match());if(match.active()){if(match.rulesSnapshot!=null)rules=match.rulesSnapshot;if(match.contentSnapshot!=null)rules.content=match.contentSnapshot;match.pause();}for(String id:new ArrayList<>(match.pending.keySet()))match.completeImage(id,false,rules);
   // Only entities carrying our namespaced tag are stale temporary entities.
   for(ServerWorld w:s.getWorlds())for(Entity e:w.iterateEntities())if(temporary(e))e.discard();
   if(Boolean.getBoolean("stillhunt.smoke")){RuntimeChecks.run(this);s.stop(false);}
  }catch(Exception e){throw new IllegalStateException("Still Hunt save/config failed; refusing silent reset",e);}});
  ServerLifecycleEvents.SERVER_STOPPING.register(s->save());
  ServerTickEvents.END_SERVER_TICK.register(this::tick);
  ServerPlayConnectionEvents.JOIN.register((h,sender,s)->{if(match.participants.containsKey(h.player.getUuid()))restorePlayer(h.player);send(h.player);});
  AttackEntityCallback.EVENT.register((player,world,hand,target,hit)->{
   if(world.isClient)return ActionResult.PASS;
   if(match.active()&&match.hider(player.getUuid())||match.phase==Match.Phase.PAUSED&&match.participants.containsKey(player.getUuid()))return ActionResult.FAIL;
   if(target instanceof ServerPlayerEntity h&&match.hider(h.getUuid())&&match.active()){
    if(match.phase==Match.Phase.SEEKING&&match.seeker(player.getUuid())&&player.isAlive()&&!player.isSpectator()&&player.squaredDistanceTo(h)<=9&&player.canSee(h))finish("seekers");
    return ActionResult.FAIL;
   }return ActionResult.PASS;
  });
  AttackBlockCallback.EVENT.register((p,w,h,pos,d)->match.active()&&match.hider(p.getUuid())?ActionResult.FAIL:ActionResult.PASS);
  UseBlockCallback.EVENT.register((p,w,h,hit)->match.active()&&match.hider(p.getUuid())?ActionResult.FAIL:ActionResult.PASS);
  UseItemCallback.EVENT.register((p,w,h)->match.active()&&match.hider(p.getUuid())?ActionResult.FAIL:ActionResult.PASS);
  UseEntityCallback.EVENT.register((p,w,h,e,hit)->match.active()&&match.hider(p.getUuid())?ActionResult.FAIL:ActionResult.PASS);
  PlayerBlockBreakEvents.BEFORE.register((w,p,pos,state,be)->!(match.active()&&match.hider(p.getUuid())));
  PlayerBlockBreakEvents.AFTER.register((w,p,pos,state,be)->{if(match.phase==Match.Phase.SEEKING&&match.seeker(p.getUuid()))ContentRegistry.taskEvent(this,p.getUuid(),"break_block",Registries.BLOCK.getId(state.getBlock()).toString());});
  ServerLivingEntityEvents.ALLOW_DAMAGE.register((e,source,amount)->{
   if(movementBlocked(e))return false;
   var p=match.participants.get(e.getUuid());if(match.active()&&p!=null&&p.protectedUntil>match.tick)return false;
   if(match.active()&&p!=null&&source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FALL)&&(p.fallGuard||match.effect(e.getUuid(),"landing"))){p.fallGuard=false;return false;}
   return !temporary(e);
  });
  ServerLivingEntityEvents.AFTER_DEATH.register((e,source)->{
   if(e instanceof ServerPlayerEntity p&&match.phase==Match.Phase.SEEKING&&match.seeker(p.getUuid())){
    match.death(p.getUuid(),p.getUuid()+":"+match.tick,rules);save();
   }
  });
  ServerPlayerEvents.AFTER_RESPAWN.register((old,p,alive)->{if(match.active()&&match.seeker(p.getUuid())){var data=match.participants.get(p.getUuid());data.protectedUntil=Math.max(data.respawnAt+100,match.tick+100);teleport(p,match.waiting);}});
  CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(literal("hunt")
   .then(literal("start").requires(s->s.hasPermissionLevel(2)).then(argument("hider",EntityArgumentType.player()).then(argument("seeker1",EntityArgumentType.player()).then(argument("seeker2",EntityArgumentType.player()).executes(ctx->{start(EntityArgumentType.getPlayer(ctx,"hider"),EntityArgumentType.getPlayer(ctx,"seeker1"),EntityArgumentType.getPlayer(ctx,"seeker2"));return 1;})))))
   .then(literal("stop").requires(s->s.hasPermissionLevel(2)).executes(ctx->{finish("admin");return 1;}))
   .then(literal("pause").requires(s->s.hasPermissionLevel(2)).executes(ctx->{match.pause();save();return 1;}))
   .then(literal("resume").requires(s->s.hasPermissionLevel(2)).executes(ctx->{if(present())match.resume();save();return 1;}))
   .then(literal("cancel_effects").requires(s->s.hasPermissionLevel(2)).then(argument("player",EntityArgumentType.player()).executes(ctx->{var p=match.participants.get(EntityArgumentType.getPlayer(ctx,"player").getUuid());if(p!=null){p.effects.clear();p.slots.clear();ContentRegistry.clearTask(p);}return 1;})))
   .then(literal("action").then(argument("json",StringArgumentType.greedyString()).executes(ctx->{handle(ctx.getSource().getPlayerOrThrow(),StringArgumentType.getString(ctx,"json"));return 1;})))
   .executes(ctx->{send(ctx.getSource().getPlayerOrThrow());ctx.getSource().sendFeedback(()->Text.translatable("hunt.help"),false);return 1;})));
 }
 private void start(ServerPlayerEntity h,ServerPlayerEntity a,ServerPlayerEntity b){
  if(match.active()){message(h,"active");return;}
  if(h==a||h==b||a==b||h.getWorld()!=a.getWorld()||a.getWorld()!=b.getWorld()){message(h,"roles");return;}
  for(var p:List.of(h,a,b))if(!ServerPlayNetworking.canSend(p,Wire.View.ID)||p.isSpectator()||p.isCreative()){message(h,"clients");return;}
  match=new Match();int size=rules.arenaSize;
  match.start(h.getUuid(),List.of(a.getUuid(),b.getUuid()),point(a),Math.floorDiv(h.getBlockX(),16)*16-size/2,Math.floorDiv(h.getBlockZ(),16)*16-size/2,h.getWorld().getRegistryKey().getValue().toString(),rules);
  for(var p:List.of(h,a,b)){p.sendMessage(Text.translatable("hunt.notice"),false);send(p);}save();
 }
 private boolean present(){return server.getPlayerManager().getPlayer(match.hider)!=null&&match.seekers.stream().anyMatch(u->server.getPlayerManager().getPlayer(u)!=null);}
 private void tick(MinecraftServer s){
  ticks++;for(var e:relays.entrySet()){var viewer=s.getPlayerManager().getPlayer(e.getKey());if(viewer!=null)e.getValue().tick(viewer,cameras.containsKey(e.getKey())?s.getPlayerManager().getPlayer(cameras.get(e.getKey())):null);}
  if(!match.active()){if(ticks%20==0)broadcast();return;}
  if(!present()&&match.phase!=Match.Phase.PAUSED)match.pause();
  if(match.phase==Match.Phase.PAUSED&&!present()){
   if(++match.absenceTicks>=rules.disconnectSeconds*20L)finish(s.getPlayerManager().getPlayer(match.hider)==null?"seekers":"hider");
  }
  match.advance(rules);
  images.keySet().removeIf(id->!match.pending.containsKey(id));
  var arena=server.getWorld(RegistryKey.of(RegistryKeys.WORLD,Identifier.of(match.dimension)));
  if(arena!=null){var tiles=terrain.sample(arena,match,rules);if(!tiles.isEmpty()){var packet=new Wire.Tiles(Storage.JSON.toJson(Map.of("match",match.id,"tiles",tiles)));for(UUID u:match.participants.keySet()){var q=server.getPlayerManager().getPlayer(u);if(q!=null&&ServerPlayNetworking.canSend(q,Wire.Tiles.ID))ServerPlayNetworking.send(q,packet);}}}
  if(match.phase==Match.Phase.FINISHED){cleanup();save();}
  if(match.phase==Match.Phase.HIDING&&match.phaseTicks>=rules.hidingSeconds*20L){
   var h=s.getPlayerManager().getPlayer(match.hider);String reason=validHide(h);
   if(reason==null){for(UUID u:match.participants.keySet()){var data=match.participants.get(u);if(data.kit.isEmpty())data.kit=data.kits.getFirst();var q=s.getPlayerManager().getPlayer(u);if(q!=null)giveKit(q,data);}h.stopRiding();h.setVelocity(Vec3d.ZERO);match.lock(point(h));save();broadcast();}
   else if(ticks%100==0)message(h,reason);
  }
  for(UUID u:match.participants.keySet()){
   var p=s.getPlayerManager().getPlayer(u);if(p==null)continue;
   var data=match.participants.get(u);
   if(match.seeker(u)){
    if(!p.isAlive()&&data.respawnAt>0&&data.respawnAt<=match.tick&&match.phase==Match.Phase.SEEKING){server.getPlayerManager().respawnPlayer(p,false,Entity.RemovalReason.KILLED);data.respawnAt=0;continue;}
    if(match.effect(u,"flight")){if(!p.isOnGround())p.startGliding();}
    else if(data.effects.containsKey("flight")){data.effects.remove("flight");p.stopGliding();data.effects.put("landing",match.tick+600);}
    if(match.effect(u,"landing")){if(p.isOnGround())data.effects.remove("landing");else if(p.getVelocity().y<-.25){p.setVelocity(p.getVelocity().multiply(1,0,1).add(0,-.25,0));p.velocityModified=true;}}
    if(match.phase==Match.Phase.HIDING||data.respawnAt>match.tick){if(p.squaredDistanceTo(vec(match.waiting))>0.25)teleport(p,match.waiting);}
    if(match.phase==Match.Phase.SEEKING&&!inside(point(p)))teleport(p,match.waiting);
    if(match.effect(u,"rhythm"))p.setSprinting(false);
    if(match.effect(u,"noise")&&ticks%20==0){p.getWorld().playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.BLOCK_NOTE_BLOCK_BELL,SoundCategory.PLAYERS,1,1);p.getWorld().spawnParticles(ParticleTypes.NOTE,p.getX(),p.getY()+2,p.getZ(),2,0.1,0.1,0.1,0);}
    if(match.phase==Match.Phase.SEEKING)ContentRegistry.taskEvent(this,u,"tick","");
    if(data.challengeAccepted&&match.tick>=data.challengeEnd){data.challengeAccepted=false;data.challengeComplete=true;data.challengesCompleted++;data.wings++;message(p,"challenge_won");}
   }
  }
  temporary.entrySet().removeIf(e->{if(match.tick<e.getValue()&&match.active())return false;for(var w:s.getWorlds()){var entity=w.getEntity(e.getKey());if(entity!=null)entity.discard();}return true;});
  if(ticks%20==0)broadcast();if(ticks%100==0)save();
 }
 private String validHide(ServerPlayerEntity p){
  if(p==null)return "missing";
  if(!inside(point(p))||!p.getWorld().getRegistryKey().getValue().toString().equals(match.dimension))return "outside";
  if(!p.isOnGround()||p.hasVehicle()||p.isTouchingWater()||p.isInLava()||!p.getWorld().isSpaceEmpty(p))return "unsafe_hide";
  return null;
 }
 private boolean inside(Point p){return p.x()>=match.originX&&p.x()<match.originX+rules.arenaSize&&p.z()>=match.originZ&&p.z()<match.originZ+rules.arenaSize&&p.y()>=rules.minY&&p.y()<rules.maxY;}
 private void restorePlayer(ServerPlayerEntity p){if(match.hider(p.getUuid())&&match.hide!=null)teleport(p,match.hide);}
 private void teleport(ServerPlayerEntity p,Point at){if(at==null)return;var w=server.getWorld(RegistryKey.of(RegistryKeys.WORLD,Identifier.of(match.dimension)));if(w==null)return;restoring=true;try{p.teleport(w,at.x(),at.y(),at.z(),Set.of(),p.getYaw(),p.getPitch(),true);}finally{restoring=false;}}
 public void finish(String who){match.finish(who);cleanup();save();broadcast();}
 private void cleanup(){cameras.clear();images.clear();for(String id:new ArrayList<>(match.pending.keySet()))match.completeImage(id,false,rules);for(var world:server.getWorlds())for(Entity e:world.iterateEntities())if(temporary(e))e.discard();temporary.clear();for(var p:match.participants.values())p.effects.clear();}
 private void save(){if(savePath!=null)try{Storage.write(savePath,match);}catch(Exception e){LOG.error("Cannot save match",e);match.pause();}}
 private void broadcast(){for(var p:server.getPlayerManager().getPlayerList())if(ServerPlayNetworking.canSend(p,Wire.View.ID))send(p);}
 private void send(ServerPlayerEntity p){
  if(!ServerPlayNetworking.canSend(p,Wire.View.ID))return;
  var view=match.view(p.getUuid(),rules);
  if(match.active()&&match.participants.containsKey(p.getUuid())){
   List<Map<String,Object>> positions=new ArrayList<>();for(UUID u:match.seekers){var q=server.getPlayerManager().getPlayer(u);if(q!=null)positions.add(Map.of("id",u.toString(),"name",q.getName().getString(),"x",q.getX(),"y",q.getY(),"z",q.getZ(),"yaw",q.getYaw(),"pitch",q.getPitch()));}view.put("positions",positions);
   if(match.hider(p.getUuid())&&cameras.containsKey(p.getUuid())){UUID target=cameras.get(p.getUuid());if(server.getPlayerManager().getPlayer(target)!=null)view.put("camera",target.toString());else cameras.remove(p.getUuid());}
   if(match.hider(p.getUuid())&&!match.pending.isEmpty())view.put("imageRequest",match.pending.keySet().iterator().next());
   if(match.hider(p.getUuid())){Map<String,Object> targets=new LinkedHashMap<>();for(UUID u:match.seekers){var data=match.participants.get(u);targets.put(u.toString(),Map.of("effects",data.effects,"task",data.taskId,"progress",data.taskProgress));}view.put("targets",targets);}
   if(match.seeker(p.getUuid())&&quotes.containsKey(p.getUuid()))view.put("quote",quotes.get(p.getUuid()));
  }
  ServerPlayNetworking.send(p,new Wire.View(Storage.JSON.toJson(view)));
 }
 private void message(ServerPlayerEntity p,String key){if(p!=null)p.sendMessage(Text.translatable("hunt."+key),false);}
 private static Point point(Entity e){return new Point(e.getX(),e.getY(),e.getZ());}
 private static Vec3d vec(Point p){return new Vec3d(p.x(),p.y(),p.z());}
 private static String str(JsonObject o,String key,String fallback){return o.has(key)?o.get(key).getAsString():fallback;}
 private static double num(JsonObject o,String key,double fallback){double n=o.has(key)?o.get(key).getAsDouble():fallback;if(!Double.isFinite(n)||Math.abs(n)>30000000)throw new IllegalArgumentException("number");return n;}
 private void handle(ServerPlayerEntity p,String json){
  if(ticks-lastIntent.getOrDefault(p.getUuid(),-100L)<3)return;lastIntent.put(p.getUuid(),ticks);
  try{
   JsonObject o=JsonParser.parseString(json).getAsJsonObject();if(!str(o,"match","").equals(match.id)||!match.participants.containsKey(p.getUuid())||!p.isAlive()||p.isSpectator()||match.participants.get(p.getUuid()).respawnAt>match.tick)return;
   String action=str(o,"action","");
   switch(action){
    case "quote" -> {if(match.seeker(p.getUuid())){String kind=str(o,"kind","");int tier=quoteTier(kind,o);if(tier>0)quotes.put(p.getUuid(),Map.of("kind",kind,"tier",tier,"cost",rules.costs[tier-1],"history",match.history.size()));}}
    case "hint" -> hint(p,o);
    case "kit" -> {var data=match.participants.get(p.getUuid());String kit=str(o,"kit","");if(match.phase==Match.Phase.HIDING&&data.kit.isEmpty()&&data.kits.contains(kit)){data.kit=kit;giveKit(p,data);}}
    case "choose" -> {if(match.hider(p.getUuid())&&match.phase==Match.Phase.SEEKING&&str(o,"offer","").equals(match.offerId))match.choose((int)num(o,"index",-1),(int)num(o,"replace",-1),rules);}
    case "card" -> playCard(p,o);
    case "camera" -> {if(match.hider(p.getUuid())&&match.phase==Match.Phase.SEEKING){String target=str(o,"target","");relays.computeIfAbsent(p.getUuid(),u->new CameraRelay());if(target.isEmpty())cameras.remove(p.getUuid());else{UUID u=UUID.fromString(target);if(match.seeker(u))cameras.put(p.getUuid(),u);}}}
    case "mark" -> mark(p,o);
    case "challenge" -> {var data=match.participants.get(p.getUuid());if(match.seeker(p.getUuid())&&match.phase==Match.Phase.SEEKING&&data.challengesCompleted<rules.challengeAtSeconds.size()&&match.phaseTicks>=rules.challengeAtSeconds.get(data.challengesCompleted)*20L&&!data.challengeAccepted&&!match.effect(p.getUuid(),"scavenge")&&!match.effect(p.getUuid(),"silence")&&match.phaseTicks+rules.challengeDurationSeconds*20L<rules.seekingSeconds*20L){data.challengeAccepted=true;data.challengeEnd=match.tick+rules.challengeDurationSeconds*20L;}}
    case "antidote" -> {var data=match.participants.get(p.getUuid());if(data.antidotes>0&&match.phase==Match.Phase.SEEKING){for(String key:List.of("boots","rhythm","noise"))if(match.effect(p.getUuid(),key)){data.effects.remove(key);data.antidotes--;break;}}}
    case "wings" -> {var data=match.participants.get(p.getUuid());if(match.seeker(p.getUuid())&&match.phase==Match.Phase.SEEKING&&data.wings>0&&!match.effect(p.getUuid(),"flight")&&!match.effect(p.getUuid(),"quarantine")&&!match.effect(p.getUuid(),"pilgrimage")){data.wings--;data.effects.put("flight",match.tick+20L*rules.flightSeconds);p.setVelocity(p.getRotationVec(1).multiply(1.3).add(0,.8,0));p.velocityModified=true;}}
    case "kit_power" -> {var data=match.participants.get(p.getUuid());if(match.phase==Match.Phase.SEEKING){switch(data.kit){
     case "engineer" -> {if(!data.engineerUsed&&data.antidotes==0){data.antidotes++;data.engineerUsed=true;}}
     case "naturalist" -> {if(!data.naturalistUsed&&!data.taskId.isEmpty()){ContentRegistry.clearTask(data);data.naturalistUsed=true;}}
     case "tactician" -> {if(!match.offer.isEmpty()&&data.refreshedAct<match.act(rules)){int rarity=match.offer.getFirst().rarity();match.offer.clear();match.reward(rarity,rules);data.refreshedAct=match.act(rules);}}
     case "archivist" -> {if(!data.engineerUsed&&match.hand.size()>=2&&match.hand.get(0).rarity()==1&&match.hand.get(1).rarity()==1){match.hand.removeFirst();match.hand.removeFirst();match.reward(1,rules);data.engineerUsed=true;}}
    }}}
   }
   save();send(p);
  }catch(RuntimeException e){message(p,"invalid");LOG.debug("Rejected invalid intent",e);}
 }
 private void giveKit(ServerPlayerEntity p,Match.Participant data){
  if(data.kitGiven)return;data.kitGiven=true;
  if(match.seeker(p.getUuid())){p.giveItemStack(new ItemStack(Items.STONE_PICKAXE));p.giveItemStack(new ItemStack(Items.BREAD,16));if(data.kit.equals("climber")){p.giveItemStack(new ItemStack(Items.LADDER,24));data.fallGuard=true;}if(data.kit.equals("naturalist")||data.kit.equals("engineer"))data.antidotes++;}
 }
 private void mark(ServerPlayerEntity p,JsonObject o){
  if(!match.seeker(p.getUuid())||!match.active())return;String delete=str(o,"delete","");
  if(!delete.isEmpty()){match.marks.removeIf(m->m.id().equals(delete)&&m.author().equals(p.getUuid()));return;}
  if(match.marks.stream().filter(m->m.author().equals(p.getUuid())).count()>=32)return;
  double x=num(o,"x",0),z=num(o,"z",0);if(!inside(new Point(x,rules.minY,z)))return;String label=str(o,"label","?");if(label.length()>32)return;
  match.marks.add(new Match.Mark(UUID.randomUUID().toString(),p.getUuid(),o.has("team")&&o.get("team").getAsBoolean(),x,z,label,0xffffcc55,str(o,"icon","suspect"),str(o,"icon","").equals("ping")?match.tick+200:0));
 }
 private void playCard(ServerPlayerEntity p,JsonObject o){
  if(!match.hider(p.getUuid())||match.phase!=Match.Phase.SEEKING)return;
  String id=str(o,"card","");var card=match.hand.stream().filter(c->c.id().equals(id)).findFirst().orElse(null);if(card==null)return;
  UUID target=UUID.fromString(str(o,"target",""));if(!match.seeker(target))return;
  var data=match.participants.get(target);var definition=rules.content.cards.get(card.kind());if(definition==null||!rules.content.profile.containsAll(definition.requires()))return;
  int duration=definition.durationSeconds();String category=definition.category();
  if(data.slots.getOrDefault(category,0L)>match.tick||category.equals("heavy")&&data.immunityUntil>match.tick||match.phaseTicks+duration*20L>=rules.seekingSeconds*20L){message(p,"restricted");return;}
  if(!ContentRegistry.apply(new ContentRegistry.CardContext(this,p,target,definition))){message(p,"invalid");return;}
  data.slots.put(category,match.tick+duration*20L);
  if(category.equals("heavy"))data.immunityUntil=match.tick+20L*(duration+rules.heavyImmunitySeconds);
  match.hand.remove(card);var q=server.getPlayerManager().getPlayer(target);if(q!=null)q.sendMessage(Text.translatable("hunt.card_applied",Text.literal(definition.names().getOrDefault(q.getClientOptions().language(),definition.names().get("en_us"))),duration),false);
 }
 public boolean spawnDecoys(int count,int duration){
  var w=server.getWorld(RegistryKey.of(RegistryKeys.WORLD,Identifier.of(match.dimension)));if(w==null)return false;
  for(int i=0;i<count;i++){var at=BlockPos.ofFloored(match.waiting.x()+i*4,match.waiting.y(),match.waiting.z()+4);var e=EntityType.ARMOR_STAND.create(w,SpawnReason.COMMAND);if(e!=null){e.refreshPositionAndAngles(at,0,0);e.addCommandTag("stillhunt.temporary");e.setCustomName(Text.translatable("hunt.decoy"));e.setCustomNameVisible(false);w.spawnEntity(e);temporary.put(e.getUuid(),match.tick+duration*20L);}}return true;
 }
 private void hint(ServerPlayerEntity p,JsonObject o){
  String kind=str(o,"kind",""),request=str(o,"request","");if(request.isEmpty())return;
  int tier=quoteTier(kind,o);if(o.has("quotedTier")&&o.get("quotedTier").getAsInt()!=tier){message(p,"quote_changed");return;}
  var result=match.purchase(request,p.getUuid(),kind,o.toString(),point(p),rules,tier,()->HintRegistry.evaluate(new HintRegistry.Context(this,p,request,kind,o)));
  if(result.ok())p.sendMessage(Text.literal("☠".repeat(result.tier())+" ").append(Text.translatable("hint.hidenseek."+kind)).append(": "+result.message()),false);else message(p,result.message());
 }
 private int quoteTier(String kind,JsonObject o){
  Constraint shape=null;
  if(kind.equals("circle"))shape=new Constraint("preview",kind,num(o,"x",0),num(o,"z",0),num(o,"radius",128),0,false,match.tick);
  else if(kind.equals("rectangle")||kind.equals("sector")){double x=num(o,"x",0),z=num(o,"z",0);shape=new Constraint("preview",kind,x,z,num(o,"x2",x+128),num(o,"z2",z+128),false,match.tick);}
  return Pricing.tier(match,rules,kind,shape);
 }
 public Match.Evaluation evaluateBuiltIn(ServerPlayerEntity p,String kind,String request,JsonObject o){
  if(match.hide==null||!p.getWorld().getRegistryKey().getValue().toString().equals(match.dimension))return null;
  ServerWorld w=p.getWorld();Point h=match.hide;double a=0,b=0,c=0,d=0;
  switch(kind){
   case "east" -> a=p.getX();case "south" -> a=p.getZ();case "above" -> {a=num(o,"y",64);if(a%16!=0||a<rules.minY||a>rules.maxY)return null;}
   case "distance" -> {a=p.getX();b=p.getZ();c=num(o,"radius",128);if(!rules.radii.contains((int)c)||c!=(int)c)return null;}
   case "circle" -> {a=num(o,"x",0);b=num(o,"z",0);c=num(o,"radius",128);if(!rules.radii.contains((int)c)||c!=(int)c||!inside(new Point(a,rules.minY,b)))return null;}
   case "sector","rectangle" -> {a=num(o,"x",0);b=num(o,"z",0);c=num(o,"x2",a+128);d=num(o,"z2",b+128);if(c-a<64||d-b<64||c-a>rules.arenaSize||d-b>rules.arenaSize||a%16!=0||b%16!=0||c%16!=0||d%16!=0)return null;if(kind.equals("sector")&&(c-a!=128||d-b!=128||Math.floorMod((int)a-match.originX,128)!=0||Math.floorMod((int)b-match.originZ,128)!=0))return null;}
   case "block" -> {
    String block=str(o,"block","");if(!rules.blocks.contains(block))return null;int radius=rules.blockRadius;boolean found=false;BlockPos center=BlockPos.ofFloored(h.x(),h.y(),h.z());
    for(BlockPos q:BlockPos.iterate(center.add(-radius,-radius,-radius),center.add(radius,radius,radius)))if(q.toCenterPos().squaredDistanceTo(vec(h))<=radius*radius){if(!w.isChunkLoaded(q))return null;if(Registries.BLOCK.getId(w.getBlockState(q).getBlock()).toString().equals(block))found=true;}
    return new Match.Evaluation(Boolean.toString(found),null);
   }
   case "biome" -> {return new Match.Evaluation(w.getBiome(BlockPos.ofFloored(h.x(),h.y(),h.z())).getKey().map(k->k.getValue().toString()).orElse("unknown"),null);}
   case "biome_is" -> {String selected=str(o,"biome","");var id=Identifier.tryParse(selected);if(id==null||!w.getRegistryManager().getOrThrow(RegistryKeys.BIOME).containsId(id))return null;return new Match.Evaluation(Boolean.toString(w.getBiome(BlockPos.ofFloored(h.x(),h.y(),h.z())).getKey().map(k->k.getValue().equals(id)).orElse(false)),null);}
   case "structure" -> {var pos=BlockPos.ofFloored(h.x(),h.y(),h.z());var registry=w.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);List<String> found=new ArrayList<>();for(var structure:registry)if(w.getStructureAccessor().getStructureContaining(pos,structure).hasChildren())found.add(registry.getId(structure).toString());Collections.sort(found);return new Match.Evaluation(found.isEmpty()?"outside":String.join(", ",found),null);}
   case "bearing" -> {int sector=Math.floorMod((int)Math.floor((Math.atan2(h.z()-p.getZ(),h.x()-p.getX())+Math.PI/8)/(Math.PI/4)),8);return new Match.Evaluation(List.of("E","SE","S","SW","W","NW","N","NE").get(sector),new Constraint(request,"bearing",p.getX(),p.getZ(),sector,0,true,match.tick));}
   case "screenshot" -> {if(!match.pending.isEmpty())return null;cameras.remove(match.hider);relays.computeIfAbsent(match.hider,u->new CameraRelay());return new Match.Evaluation("pending_image",null);}
   case "animal" -> {return spawnAnimal(w,h)?new Match.Evaluation("local_sound",null):null;}
   case "firework" -> {for(int y=0;y<20;y++){double height=h.y()+y;w.spawnParticles(ParticleTypes.FIREWORK,h.x(),height,h.z(),10,.15,.15,.15,.03);if(!w.getBlockState(BlockPos.ofFloored(h.x(),height+1,h.z())).isAir())break;}w.playSound(null,h.x(),h.y(),h.z(),SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH,SoundCategory.AMBIENT,2,1);return new Match.Evaluation("local_visual",null);}
   default -> {return null;}
  }
  Constraint probe=new Constraint(request,kind,a,b,c,d,false,match.tick);boolean answer=probe.test(h);
  return new Match.Evaluation(Boolean.toString(answer),new Constraint(request,kind,a,b,c,d,answer,match.tick));
 }
 private boolean spawnAnimal(ServerWorld w,Point h){
  for(int dx:new int[]{2,-2,3,-3})for(int dz:new int[]{2,-2}){
   double x=h.x()+dx,z=h.z()+dz;BlockPos pos=BlockPos.ofFloored(x,h.y(),z);
   if(!w.isChunkLoaded(pos)||!w.getBlockState(pos.down()).isSolidBlock(w,pos.down())||!w.getBlockState(pos).isAir())continue;
   ChickenEntity e=EntityType.CHICKEN.create(w,SpawnReason.COMMAND);if(e==null)return false;e.refreshPositionAndAngles(x,h.y(),z,0,0);
   if(!w.isSpaceEmpty(e)){e.discard();continue;}e.setAiDisabled(true);e.setInvulnerable(true);e.setNoGravity(true);e.addCommandTag("stillhunt.temporary");w.spawnEntity(e);temporary.put(e.getUuid(),match.tick+600);return true;
  }return false;
 }
 private void receiveImage(ServerPlayerEntity p,Wire.ImagePart part){
  if(!match.hider(p.getUuid())||match.phase!=Match.Phase.SEEKING||!part.match().equals(match.id)||!match.pending.containsKey(part.request()))return;
  try{
   ImageTransfer transfer=images.computeIfAbsent(part.request(),id->new ImageTransfer());
   if(transfer.accept(part.index(),part.total(),part.data())){
    byte[] bytes=transfer.bytes();if(!match.completeImage(part.request(),true,rules))return;images.remove(part.request());
    for(UUID u:match.seekers){var seeker=server.getPlayerManager().getPlayer(u);if(seeker==null)continue;for(int i=0;i<ImageTransfer.COUNT;i++)ServerPlayNetworking.send(seeker,new Wire.ImagePart(match.id,part.request(),i,ImageTransfer.COUNT,Arrays.copyOfRange(bytes,i*ImageTransfer.CHUNK,Math.min(bytes.length,(i+1)*ImageTransfer.CHUNK))));}
    save();broadcast();
   }
  }catch(RuntimeException e){images.remove(part.request());match.completeImage(part.request(),false,rules);save();}
 }
}
