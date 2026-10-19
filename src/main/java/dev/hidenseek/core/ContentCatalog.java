package dev.hidenseek.core;

import java.util.*;

/** Data-only extension point. Handler IDs refer to code registered by the mod or an addon. */
public class ContentCatalog {
 public int schemaVersion=1;
 public Set<String> profile=new HashSet<>(Set.of("stone","waiting_point"));
 public Map<String,CardDefinition> cards=new LinkedHashMap<>();
 public Map<String,TaskDefinition> tasks=new LinkedHashMap<>();
 public record CardDefinition(String handler,int rarity,int weight,String category,int durationSeconds,
                              Map<String,String> names,Map<String,String> parameters,Set<String> requires) {}
 public record TaskDefinition(String objective,String target,int count,int timeoutSeconds,
                              Map<String,String> names,Set<String> requires) {}
 public static ContentCatalog defaults(){
  ContentCatalog c=new ContentCatalog();
  c.tasks.put("quarry",new TaskDefinition("break_block","minecraft:stone",3,90,names("Каменоломня: добудьте 3 каменных блока","Quarry: mine 3 stone blocks"),Set.of("stone")));
  c.tasks.put("return",new TaskDefinition("visit_waiting_point","",1,90,names("Паломничество: вернитесь к точке ожидания","Pilgrimage: visit the waiting point"),Set.of("waiting_point")));
  c.add("boots","effect",1,"heavy",60,"Тяжёлые ботинки","Heavy boots",Map.of("effect","boots"));
  c.add("rhythm","effect",1,"light",45,"Сбитый ритм","Broken rhythm",Map.of("effect","rhythm"));
  c.add("noise","effect",1,"light",45,"Шумный охотник","Noisy hunter",Map.of("effect","noise"));
  c.add("scavenge","task",1,"task",90,"Каменоломня","Quarry",Map.of("task","quarry","effect","scavenge"));
  c.add("long_boots","effect",2,"heavy",120,"Тяжёлый марш","Heavy march",Map.of("effect","boots"));
  c.add("silence","effect",2,"access",30,"Радиомолчание","Radio silence",Map.of("effect","silence"));
  c.add("pilgrimage","task",2,"task",90,"Паломничество","Pilgrimage",Map.of("task","return","effect","pilgrimage"));
  c.add("rare_silence","effect",3,"access",45,"Глубокое радиомолчание","Deep radio silence",Map.of("effect","silence"));
  c.add("rare_boots","effect",3,"heavy",180,"Долгий марш","Long march",Map.of("effect","boots"));
  c.add("decoy","decoy",3,"decoy",60,"Ложный лагерь","False camp",Map.of("count","1"));
  c.add("quarantine","effect",4,"access",45,"Карантин инструментов","Tool quarantine",Map.of("effect","quarantine"));
  c.add("double_decoy","decoy",4,"decoy",90,"Двойная легенда","Double legend",Map.of("count","2"));
  c.add("exam","effect",4,"access",60,"Последний экзамен","Final exam",Map.of("effect","exam"));
  return c;
 }
 public static Map<String,String> names(String ru,String en){return Map.of("ru_ru",ru,"en_us",en);}
 private void add(String id,String handler,int rarity,String category,int duration,String ru,String en,Map<String,String> params){
  Set<String> requires=handler.equals("task")?tasks.get(params.get("task")).requires:Set.of();
  cards.put(id,new CardDefinition(handler,rarity,10,category,duration,names(ru,en),params,requires));
 }
 public void validate(Set<String> handlers,Set<String> objectives){
  if(schemaVersion!=1||cards.size()>256||tasks.size()>128)throw new IllegalArgumentException("Content version/size");
  for(var e:tasks.entrySet()){TaskDefinition t=e.getValue();checkId(e.getKey());if(!objectives.contains(t.objective)||t.count<1||t.count>1024||t.timeoutSeconds<5||t.timeoutSeconds>600||t.names==null||t.requires==null)throw new IllegalArgumentException("Invalid task: "+e.getKey());}
  for(var e:cards.entrySet()){
   checkId(e.getKey());CardDefinition c=e.getValue();
   if(!handlers.contains(c.handler)||c.rarity<1||c.rarity>4||c.weight<1||c.weight>1000||c.durationSeconds<1||c.durationSeconds>600||c.parameters==null||c.names==null||c.requires==null||!Set.of("heavy","light","task","access","decoy").contains(c.category))throw new IllegalArgumentException("Invalid card: "+e.getKey());
   if(c.handler.equals("task")&&!tasks.containsKey(c.parameters.get("task")))throw new IllegalArgumentException("Missing task in "+e.getKey());
   if(c.handler.equals("effect")&&!Set.of("boots","rhythm","noise","silence","quarantine","exam").contains(c.parameters.get("effect")))throw new IllegalArgumentException("Unknown built-in effect in "+e.getKey());
   if(c.handler.equals("decoy")&&!Set.of("1","2").contains(c.parameters.get("count")))throw new IllegalArgumentException("Invalid decoy count");
  }
  for(int rarity=1;rarity<=4;rarity++)if(eligible(rarity).size()<3)throw new IllegalArgumentException("Need >=3 available cards at rarity "+rarity);
 }
 public List<String> eligible(int rarity){return cards.entrySet().stream().filter(e->e.getValue().rarity==rarity&&profile.containsAll(e.getValue().requires)).map(Map.Entry::getKey).toList();}
 public static void checkId(String id){if(!id.matches("[a-z][a-z0-9_.-]{0,63}"))throw new IllegalArgumentException("Invalid ID: "+id);}
}
