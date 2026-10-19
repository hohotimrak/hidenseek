package dev.hidenseek.core;
import dev.hidenseek.Storage;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PersistenceTest {
 @Test void privateStateAndRulesSurviveRoundTrip(){var r=new Rules();var m=new Match();UUID h=UUID.randomUUID(),s=UUID.randomUUID();m.start(h,List.of(s,UUID.randomUUID()),new Geometry.Point(0,70,0),-512,-512,"minecraft:overworld",r);m.lock(new Geometry.Point(10.25,-23,12.75));m.participants.get(s).effects.put("boots",400L);m.reward(3,r);m.pause();Match copy=Storage.JSON.fromJson(Storage.JSON.toJson(m),Match.class);assertEquals(m.hide,copy.hide);assertEquals(m.offer,copy.offer);assertTrue(copy.effect(s,"boots"));assertEquals(2700,copy.rulesSnapshot.seekingSeconds);assertTrue(copy.contentSnapshot.cards.containsKey("decoy"));assertEquals(Match.Phase.PAUSED,copy.phase);assertEquals(Match.Phase.SEEKING,copy.previous);}
}
