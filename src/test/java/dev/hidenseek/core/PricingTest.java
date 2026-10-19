package dev.hidenseek.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.hidenseek.core.Geometry.*;
class PricingTest {
 @Test void priceCannotLeakHiddenAnswer(){Rules r=new Rules();Match m=new Match();m.originX=-512;m.originZ=-512;var q=new Constraint("q","rectangle",-512,-512,0,512,false,0);m.hide=new Point(-100,50,0);int a=Pricing.tier(m,r,"rectangle",q);m.hide=new Point(100,50,0);assertEquals(a,Pricing.tier(m,r,"rectangle",q));assertEquals(5,a);}
 @Test void smallRegionStillNotCheap(){Rules r=new Rules();Match m=new Match();assertEquals(4,Pricing.tier(m,r,"circle",new Constraint("q","circle",0,0,64,0,false,0)));}
}
