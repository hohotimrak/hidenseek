package dev.hidenseek.core;

import java.util.*;

/** JSON configuration, bounded at load time. */
public class Rules {
 public transient ContentCatalog content=ContentCatalog.defaults();
 public int hidingSeconds=120, seekingSeconds=2700, disconnectSeconds=120;
 public int initialIntel=5, maxIntel=15, regenSeconds=45, finalRegenSeconds=30;
 public int teamCooldown=20, hintCooldown=90, levelFiveCooldown=360, areaCooldown=180;
 public int[] costs={1,2,4,6,8};
 public int handLimit=6, offerSeconds=30, heavyImmunitySeconds=45;
 public int blockRadius=4, respawnSeconds=10;
 public List<Integer> challengeAtSeconds=new ArrayList<>(List.of(480,1200,1920));
 public int challengeDurationSeconds=90,flightSeconds=12;
 public int arenaSize=1024, minY=-64, maxY=320, cellSize=16;
 public List<Integer> radii=new ArrayList<>(List.of(64,128,256));
 public List<String> blocks=new ArrayList<>(List.of("minecraft:stone","minecraft:dirt","minecraft:water","minecraft:oak_log"));
 public Map<String,Integer> tiers=new LinkedHashMap<>(Map.ofEntries(
  Map.entry("animal",1),Map.entry("block",3),Map.entry("biome",4),Map.entry("east",4),Map.entry("south",4),
  Map.entry("above",4),Map.entry("circle",4),Map.entry("rectangle",4),Map.entry("sector",4),Map.entry("distance",4),Map.entry("firework",5),Map.entry("screenshot",5),Map.entry("bearing",5),Map.entry("biome_is",4),Map.entry("structure",4)));
 public void validate() {
  if(hidingSeconds<1||seekingSeconds<30||disconnectSeconds<1||initialIntel<0||maxIntel<initialIntel||maxIntel>100||
    regenSeconds<1||finalRegenSeconds<1||teamCooldown<1||hintCooldown<1||levelFiveCooldown<1||areaCooldown<1||
    arenaSize<64||arenaSize>2048||cellSize<8||cellSize>64||arenaSize%cellSize!=0||minY>=maxY||minY< -64||maxY>320||
    blockRadius<1||blockRadius>8||costs.length!=5||handLimit<2||handLimit>12||offerSeconds<1||respawnSeconds<1||challengeDurationSeconds<5||flightSeconds<1||flightSeconds>30||challengeAtSeconds.size()>8)
   throw new IllegalArgumentException("Invalid Still Hunt configuration");
  for(int i=0;i<5;i++) if(costs[i]<1||costs[i]>maxIntel||(i>0&&costs[i]<costs[i-1])) throw new IllegalArgumentException("costs");
  for(var e:tiers.entrySet()) if(e.getValue()<1||e.getValue()>5) throw new IllegalArgumentException("tier");
  for(String k:List.of("east","south","above","circle","rectangle","sector","distance")) if(tiers.getOrDefault(k,0)<4) throw new IllegalArgumentException("Formal hints must have >=4 skulls");
  if(radii.isEmpty()||radii.stream().anyMatch(r->r<16||r>arenaSize)) throw new IllegalArgumentException("radii");
 }
}
