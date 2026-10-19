package dev.hidenseek.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import dev.hidenseek.core.Geometry.*;
class GeometryTest {
 @Test void truthfulSequencesNeverExcludeHide(){
  Random rng=new Random(780121);
  for(int run=0;run<1000;run++){
   Point p=new Point(rng.nextDouble()*1024-512,rng.nextDouble()*384-64,rng.nextDouble()*1024-512);
   Box cell=new Box(Math.floor(p.x()/16)*16,Math.floor(p.y()/16)*16,Math.floor(p.z()/16)*16,Math.floor(p.x()/16)*16+16,Math.floor(p.y()/16)*16+16,Math.floor(p.z()/16)*16+16);
   Match m=new Match();
   for(int j=0;j<50;j++){
    String kind=List.of("east","south","above","circle","rectangle","distance","sector").get(rng.nextInt(7));
    double a=rng.nextDouble()*1024-512,b=rng.nextDouble()*1024-512;
    Constraint q=new Constraint("q",kind,a,b,kind.equals("circle")||kind.equals("distance")?128:a+64,b+64,false,j);
    m.constraints.add(new Constraint(q.id(),kind,a,b,q.c(),q.d(),q.test(p),j));
    assertTrue(m.possible(cell),kind);
   }
  }
 }
 @Test void exactBoundaries(){
  var circle=new Constraint("c","circle",0,0,5,0,true,0);
  assertTrue(circle.test(new Point(3,999,4)));
  assertFalse(circle.excludes(new Box(3,-64,4,4,320,5)));
  assertTrue(circle.excludes(new Box(6,-64,6,7,320,7)));
  var east=new Constraint("e","east",0,0,0,0,false,0);
  assertFalse(east.excludes(new Box(0,0,0,1,1,1)));
  assertTrue(east.excludes(new Box(0.001,0,0,1,1,1)));
 }
 @Test void verticalColumnAndUnknown(){
  Match m=new Match();m.constraints.add(new Constraint("a","above",64,0,0,0,true,0));
  assertTrue(m.possible(new Box(0,-64,0,16,320,16)));
  assertFalse(m.possible(new Box(0,-64,0,16,64,16)));
  assertFalse(new Constraint("b","mutable_block",0,0,0,0,false,0).excludes(new Box(0,0,0,1,1,1)));
 }
 @Test void bearingRetainsActualPoint(){Random r=new Random(27);for(int i=0;i<10000;i++){double x=r.nextDouble()*200-100,z=r.nextDouble()*200-100;int sector=Math.floorMod((int)Math.floor((Math.atan2(z,x)+Math.PI/8)/(Math.PI/4)),8);var q=new Constraint("b","bearing",0,0,sector,0,true,0);assertTrue(q.test(new Point(x,0,z)));assertFalse(q.excludes(new Box(Math.floor(x),0,Math.floor(z),Math.floor(x)+1,1,Math.floor(z)+1)));}}
}
