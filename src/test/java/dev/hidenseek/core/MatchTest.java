package dev.hidenseek.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import dev.hidenseek.core.Geometry.*;
class MatchTest {
 Rules r=new Rules();UUID h=UUID.randomUUID(),s=UUID.randomUUID(),t=UUID.randomUUID();
 Match game(){Match m=new Match();m.start(h,List.of(s,t),new Point(0,64,0),-512,-512,"minecraft:overworld",r);m.lock(new Point(0,64,0));m.phaseTicks=18000;m.intel=15;return m;}
 Match.Receipt buy(Match m,String id,UUID who){return m.purchase(id,who,"east","",new Point(10,64,0),r,()->new Match.Evaluation("false",new Constraint(id,"east",10,0,0,0,false,m.tick)));}
 @Test void idempotentPurchase(){Match m=game();assertTrue(buy(m,"id",s).ok());int intel=m.intel;assertEquals(buy(m,"id",s),buy(m,"id",s));assertEquals(intel,m.intel);assertEquals(1,m.history.size());assertEquals(3,m.offer.size());assertTrue(m.offer.stream().allMatch(c->c.rarity()==3));}
 @Test void simultaneousPurchases()throws Exception{Match m=game();try(var executor=Executors.newFixedThreadPool(2)){var a=executor.submit(()->buy(m,"a",s));var b=executor.submit(()->buy(m,"b",t));assertNotEquals(a.get().ok(),b.get().ok());}assertEquals(9,m.intel);}
 @Test void failedEvaluationDoesNotCharge(){Match m=game();assertFalse(m.purchase("bad",s,"east","",new Point(0,0,0),r,()->null).ok());assertEquals(15,m.intel);assertTrue(m.offer.isEmpty());}
 @Test void privateDataNotInSeekerView(){Match m=game();buy(m,"x",s);var view=m.view(s,r);for(String k:List.of("hide","hand","offer","rewardProgress","hider"))assertFalse(view.containsKey(k),k);assertTrue(m.view(h,r).containsKey("hand"));}
 @Test void deathRewardOnceAndEffectsPersist(){Match m=game();m.participants.get(s).effects.put("boots",1000L);m.death(s,"event",r);m.death(s,"event",r);assertEquals(1,m.participants.get(s).deaths);assertTrue(m.effect(s,"boots"));assertEquals(3,m.offer.size());}
 @Test void pauseFreezesAllTimers(){Match m=game();long tick=m.tick;m.pause();for(int i=0;i<100;i++)m.advance(r);assertEquals(tick,m.tick);m.resume();m.advance(r);assertEquals(tick+1,m.tick);}
 @Test void boundedRewards(){Match m=game();for(int i=0;i<100;i++)m.reward(1,r);assertEquals(r.handLimit,m.hand.size());assertEquals(3,m.offer.size());}
 @Test void configRejectsCheapHalving(){r.tiers.put("east",1);assertThrows(IllegalArgumentException.class,r::validate);}
 @Test void imageRefundExactlyOnce(){Match m=game();m.phaseTicks=36000;var result=m.purchase("image",s,"screenshot","",new Point(0,0,0),r,()->new Match.Evaluation("pending_image",null));assertTrue(result.ok());assertEquals(7,m.intel);assertTrue(m.offer.isEmpty());assertTrue(m.completeImage("image",false,r));assertFalse(m.completeImage("image",false,r));assertFalse(m.completeImage("image",true,r));assertEquals(15,m.intel);assertTrue(m.offer.isEmpty());}
}
