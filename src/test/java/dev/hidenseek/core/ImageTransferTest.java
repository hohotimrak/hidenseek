package dev.hidenseek.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ImageTransferTest {
 @Test void exactBoundedReassembly(){byte[] all=new byte[ImageTransfer.LENGTH];new Random(42).nextBytes(all);ImageTransfer t=new ImageTransfer();for(int i=ImageTransfer.COUNT-1;i>=0;i--){var part=Arrays.copyOfRange(all,i*ImageTransfer.CHUNK,Math.min(all.length,(i+1)*ImageTransfer.CHUNK));t.accept(i,ImageTransfer.COUNT,part);t.accept(i,ImageTransfer.COUNT,part);}assertArrayEquals(all,t.bytes());}
 @Test void malformedPartsRejected(){ImageTransfer t=new ImageTransfer();assertThrows(IllegalArgumentException.class,()->t.accept(-1,12,new byte[1]));assertThrows(IllegalArgumentException.class,()->t.accept(0,ImageTransfer.COUNT,new byte[16001]));assertThrows(IllegalStateException.class,t::bytes);}
}
