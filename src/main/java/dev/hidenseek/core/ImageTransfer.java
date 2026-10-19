package dev.hidenseek.core;
import java.util.*;
/** Fixed raw RGB format: no decompression bombs, metadata, filesystem access or arbitrary dimensions. */
public final class ImageTransfer {
 public static final int WIDTH=320,HEIGHT=180,LENGTH=WIDTH*HEIGHT*3,CHUNK=16000,COUNT=(LENGTH+CHUNK-1)/CHUNK;
 private final byte[] image=new byte[LENGTH];private final BitSet received=new BitSet(COUNT);
 public boolean accept(int index,int total,byte[] bytes){
  if(total!=COUNT||index<0||index>=COUNT||bytes.length!=Math.min(CHUNK,LENGTH-index*CHUNK))throw new IllegalArgumentException("Image part size");
  if(!received.get(index)){System.arraycopy(bytes,0,image,index*CHUNK,bytes.length);received.set(index);}
  return received.cardinality()==COUNT;
 }
 public byte[] bytes(){if(received.cardinality()!=COUNT)throw new IllegalStateException("Incomplete image");return image.clone();}
}
