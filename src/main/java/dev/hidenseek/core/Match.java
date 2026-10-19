package dev.hidenseek.core;

import java.util.*;
import java.util.function.Supplier;
import dev.hidenseek.core.Geometry.*;

/** No Minecraft dependency: this object is persisted, but NEVER directly networked. */
public class Match {
 public enum Phase { LOBBY,HIDING,SEEKING,PAUSED,FINISHED }
 public String id=UUID.randomUUID().toString(), dimension="minecraft:overworld", winner="";
 public Phase phase=Phase.LOBBY, previous=Phase.LOBBY;
 public long tick,phaseTicks,absenceTicks;
 public int intel,regenTicks,rewardProgress,originX,originZ;
 public UUID hider;
 public Rules rulesSnapshot;
 public ContentCatalog contentSnapshot;
 public List<UUID> seekers=new ArrayList<>();
 public Point hide,waiting;
 public List<Constraint> constraints=new ArrayList<>();
 public List<Observation> history=new ArrayList<>();
 public List<Mark> marks=new ArrayList<>();
 public List<Card> hand=new ArrayList<>();
 public List<Card> offer=new ArrayList<>();
 public long offerExpires;
 public String offerId="";
 public Map<String,Long> cooldowns=new HashMap<>();
 public Map<String,Receipt> receipts=new LinkedHashMap<>();
 public Map<String,Pending> pending=new HashMap<>();
 public Set<String> deathIds=new HashSet<>();
 public Map<UUID,Participant> participants=new HashMap<>();
 public record Card(String id,String kind,int rarity) {}
 public record Observation(String id,String kind,String params,String answer,Point buyer,String dimension,long tick) {}
 public record Mark(String id,UUID author,boolean team,double x,double z,String label,int color,String icon,long expires) {}
 public record Receipt(boolean ok,String message,int tier,int cost) {}
 public record Evaluation(String answer,Constraint constraint) {}
 public record Pending(UUID buyer,String kind,String params,Point position,int tier,int cost,long expires) {}
 public static class Participant {
  public String kit="";
  public List<String> kits=new ArrayList<>();
  public Map<String,Long> effects=new HashMap<>();
  public long immunityUntil,respawnAt,protectedUntil;
  public int deaths,discounts=2,antidotes=0,wings=0;
  public boolean kitGiven,challengeAccepted,challengeComplete,engineerUsed,naturalistUsed,fallGuard;
  public int refreshedAct;
  public int challengesCompleted;
  public String taskId="",taskEffect="";
  public int taskProgress;
  public long taskStarted,taskExpires;
  public Map<String,Long> slots=new HashMap<>();
  public long challengeEnd;
 }
 public void start(UUID h,List<UUID> s,Point wait,int x,int z,String dim,Rules r) {
  if(phase!=Phase.LOBBY||s.size()!=2||new HashSet<>(s).size()!=2||s.contains(h)) throw new IllegalStateException("roles");
  hider=h;seekers.addAll(s);waiting=wait;originX=x;originZ=z;dimension=dim;intel=r.initialIntel;phase=Phase.HIDING;
  rulesSnapshot=r;contentSnapshot=r.content;
  List<UUID> all=new ArrayList<>(s);all.add(h);
  for(UUID u:all){Participant p=new Participant();List<String> kits=new ArrayList<>(u.equals(h)?List.of("illusionist","puppeteer","archivist","tactician"):List.of("tracker","climber","engineer","naturalist"));Collections.shuffle(kits);p.kits.addAll(kits.subList(0,3));participants.put(u,p);}
  hand.add(card("boots",1));hand.add(card("rhythm",1));
 }
 public boolean active(){return phase!=Phase.LOBBY&&phase!=Phase.FINISHED;}
 public boolean seeker(UUID u){return seekers.contains(u);}
 public boolean hider(UUID u){return u.equals(hider);}
 public int act(Rules r){return Math.min(3,1+(int)(phaseTicks/Math.max(1,r.seekingSeconds*20L/3)));}
 public boolean effect(UUID u,String key){Participant p=participants.get(u);return p!=null&&p.effects.getOrDefault(key,0L)>tick;}
 public void pause(){if(phase==Phase.HIDING||phase==Phase.SEEKING){previous=phase;phase=Phase.PAUSED;absenceTicks=0;}}
 public void resume(){if(phase==Phase.PAUSED){phase=previous;absenceTicks=0;}}
 public void lock(Point p){hide=p;phase=Phase.SEEKING;phaseTicks=0;}
 public void finish(String who){winner=who;phase=Phase.FINISHED;}
 public void advance(Rules r){
  if(phase!=Phase.HIDING&&phase!=Phase.SEEKING)return;
  tick++;phaseTicks++;
  if(phase==Phase.SEEKING){
   if(++regenTicks>=20*(act(r)==3?r.finalRegenSeconds:r.regenSeconds)){intel=Math.min(r.maxIntel,intel+1);regenTicks=0;}
   if(phaseTicks>=20L*r.seekingSeconds)finish("hider");
  }
  if(!offer.isEmpty()&&tick>=offerExpires)choose(0,-1,r);
  for(String id:new ArrayList<>(pending.keySet()))if(pending.get(id).expires<=tick)completeImage(id,false,r);
  marks.removeIf(m->m.expires>0&&m.expires<=tick);
 }
 public synchronized Receipt purchase(String request,UUID who,String kind,String params,Point buyer,Rules r,Supplier<Evaluation> evaluate){return purchase(request,who,kind,params,buyer,r,r.tiers.getOrDefault(kind,0),evaluate);}
 public synchronized Receipt purchase(String request,UUID who,String kind,String params,Point buyer,Rules r,int tier,Supplier<Evaluation> evaluate){
  if(receipts.containsKey(request))return receipts.get(request);
  if(request.length()>64||receipts.size()>=2048)return new Receipt(false,"request_limit",0,0);
  if(!seeker(who)||phase!=Phase.SEEKING||tier==0)return fail(request,"unavailable");
  if(tier>Math.min(5,act(r)+2))return fail(request,"act_locked");
  if(effect(who,"scavenge")||effect(who,"silence")&&tier>=4||effect(who,"exam")&&tier==5||participants.get(who).challengeAccepted)return fail(request,"restricted");
  boolean area=List.of("circle","rectangle","sector","distance","east","south","above").contains(kind);
  if(cooldowns.getOrDefault("team",0L)>tick||cooldowns.getOrDefault(kind,0L)>tick||tier==5&&cooldowns.getOrDefault("five",0L)>tick||area&&cooldowns.getOrDefault("area",0L)>tick)return fail(request,"cooldown");
  Participant p=participants.get(who);boolean discount=p.kit.equals("tracker")&&p.discounts>0&&tier<=2;
  int cost=Math.max(1,r.costs[tier-1]-(discount?1:0));
  if(intel<cost)return fail(request,"intel");
  Evaluation result=evaluate.get();
  if(result==null)return fail(request,"invalid");
  intel-=cost;if(discount)p.discounts--;
  cooldowns.put("team",tick+20L*r.teamCooldown);cooldowns.put(kind,tick+20L*r.hintCooldown);
  if(area)cooldowns.put("area",tick+20L*r.areaCooldown);
  if(tier==5)cooldowns.put("five",tick+20L*r.levelFiveCooldown);
  if(result.answer.equals("pending_image")){
   pending.put(request,new Pending(who,kind,params,buyer,tier,cost,tick+400));
   Receipt receipt=new Receipt(true,"pending_image",tier,cost);receipts.put(request,receipt);return receipt;
  }
  if(result.constraint!=null)constraints.add(result.constraint);
  history.add(new Observation(request,kind,params,result.answer,buyer,dimension,tick));
  if(tier==1){if(++rewardProgress>=2){rewardProgress=0;reward(1,r);}}else reward(tier-1,r);
  Receipt receipt=new Receipt(true,result.answer,tier,cost);receipts.put(request,receipt);return receipt;
 }
 private Receipt fail(String request,String key){Receipt receipt=new Receipt(false,key,0,0);receipts.put(request,receipt);return receipt;}
 public synchronized boolean completeImage(String request,boolean success,Rules r){
  Pending p=pending.remove(request);if(p==null)return false;
  if(success){history.add(new Observation(request,p.kind,p.params,"image",p.position,dimension,tick));reward(p.tier-1,r);receipts.put(request,new Receipt(true,"image",p.tier,p.cost));}
  else{intel=Math.min(r.maxIntel,intel+p.cost);receipts.put(request,new Receipt(false,"image_refunded",p.tier,0));}
  return true;
 }
 private static Card card(String k,int rarity){return new Card(UUID.randomUUID().toString(),k,rarity);}
 public void reward(int rarity,Rules r){
  // Resolve old offer before replacing; bounded hand means no unbounded reward queue.
  if(!offer.isEmpty())choose(0,-1,r);
  List<String> pool=new ArrayList<>(r.content.eligible(rarity));
  offerId=UUID.randomUUID().toString();
  String kit=participants.get(hider).kit;
  int count=3;if(kit.equals("illusionist")&&pool.stream().anyMatch(k->r.content.cards.get(k).category().equals("decoy"))||kit.equals("puppeteer")&&pool.stream().anyMatch(k->r.content.cards.get(k).category().equals("task")))count++;
  for(int i=0;i<count;i++){
   List<String> available=pool.isEmpty()?r.content.eligible(rarity):pool;int total=available.stream().mapToInt(k->r.content.cards.get(k).weight()).sum();int roll=java.util.concurrent.ThreadLocalRandom.current().nextInt(total);String selected=available.getFirst();
   for(String k:available){roll-=r.content.cards.get(k).weight();if(roll<0){selected=k;break;}}
   offer.add(card(selected,rarity));pool.remove(selected);
  }
  offerExpires=tick+20L*r.offerSeconds;
 }
 public void choose(int index,int replace,Rules r){
  if(index<0||index>=offer.size())return;
  int limit=r.handLimit+(participants.get(hider).kit.equals("archivist")?1:0);
  if(hand.size()<limit)hand.add(offer.get(index));else if(replace>=0&&replace<hand.size())hand.set(replace,offer.get(index));
  offer.clear();offerId="";
 }
 public void death(UUID player,String eventId,Rules r){
  if(!seeker(player)||phase!=Phase.SEEKING||!deathIds.add(eventId))return;
  Participant p=participants.get(player);p.deaths++;p.respawnAt=tick+20L*r.respawnSeconds;reward(1,r);
 }
 public boolean possible(Box box){return constraints.stream().noneMatch(c->c.excludes(box));}
 public Map<String,Object> view(UUID viewer,Rules r){
  Map<String,Object> out=new LinkedHashMap<>();out.put("id",id);out.put("phase",phase);out.put("tick",tick);
  out.put("seconds",Math.max(0,(phase==Phase.HIDING||phase==Phase.PAUSED&&previous==Phase.HIDING?r.hidingSeconds:r.seekingSeconds)-phaseTicks/20));
  out.put("winner",winner);out.put("role",hider(viewer)?"hider":seeker(viewer)?"seeker":"observer");
  if(!participants.containsKey(viewer))return out;
  out.put("originX",originX);out.put("originZ",originZ);out.put("size",r.arenaSize);out.put("minY",r.minY);out.put("maxY",r.maxY);out.put("cell",r.cellSize);
  out.put("participant",participants.get(viewer));out.put("seekers",seekers);
  if(hider(viewer)){out.put("hand",hand);out.put("offer",offer);out.put("offerId",offerId);out.put("offerExpires",offerExpires);out.put("cardDefinitions",r.content.cards);if(hide!=null)out.put("anchor",hide);}
  else {out.put("intel",intel);out.put("constraints",constraints);out.put("history",history);out.put("cooldowns",cooldowns);out.put("tiers",r.tiers);out.put("costs",r.costs);out.put("blocks",r.blocks);out.put("radii",r.radii);out.put("marks",marks.stream().filter(m->m.team||m.author.equals(viewer)).toList());}
  return out;
 }
}
