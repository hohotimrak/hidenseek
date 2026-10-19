package dev.hidenseek.core;
import dev.hidenseek.core.Geometry.*;
/** Public-data-only quote. Balanced partitions of remaining conservative cells cost five skulls. */
public final class Pricing {
 public static int tier(Match match,Rules rules,String kind,Constraint shape){
  int base=rules.tiers.getOrDefault(kind,0);if(shape==null||!java.util.Set.of("circle","rectangle","sector").contains(kind))return base;
  double yes=0,total=0;int step=rules.cellSize;
  for(int x=match.originX;x<match.originX+rules.arenaSize;x+=step)for(int z=match.originZ;z<match.originZ+rules.arenaSize;z+=step){
   Box b=new Box(x,rules.minY,z,x+step,rules.maxY,z+step);if(!match.possible(b))continue;total++;Truth t=shape.classify(b);yes+=t==Truth.TRUE?1:t==Truth.UNKNOWN?.5:0;
  }
  double fraction=total==0?0:yes/total;return fraction>=.4&&fraction<=.6?Math.max(base,5):Math.max(base,4);
 }
}
