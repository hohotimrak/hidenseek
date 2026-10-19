package dev.hidenseek;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;

/** Bounded snapshot streaming of already-loaded chunks; never moves the player's entity. */
public final class CameraRelay {
 private ChunkPos center;
 private final ArrayDeque<ChunkPos> pending=new ArrayDeque<>();
 private boolean remote;
 public void tick(ServerPlayerEntity viewer,ServerPlayerEntity target){
  boolean nextRemote=target!=null;if(target==null)target=viewer;
  ChunkPos next=target.getChunkPos();
  if(!next.equals(center)||remote!=nextRemote){
   center=next;remote=nextRemote;pending.clear();
   viewer.networkHandler.sendPacket(new ChunkRenderDistanceCenterS2CPacket(next.x,next.z));
   int radius=remote?3:Math.min(10,viewer.getServer().getPlayerManager().getViewDistance());
   // Near chunks first, then expanding square rings.
   for(int r=0;r<=radius;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(Math.max(Math.abs(x),Math.abs(z))==r)pending.add(new ChunkPos(next.x+x,next.z+z));
  }
  for(int i=0;i<2&&!pending.isEmpty();i++){
   ChunkPos p=pending.remove();var chunk=target.getWorld().getChunkManager().getChunk(p.x,p.z,ChunkStatus.FULL,false);
   if(chunk instanceof WorldChunk full)viewer.networkHandler.sendPacket(new ChunkDataS2CPacket(full,target.getWorld().getLightingProvider(),null,null));
  }
 }
}
