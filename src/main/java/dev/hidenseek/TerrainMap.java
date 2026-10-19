package dev.hidenseek;
import dev.hidenseek.core.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import java.util.*;

public final class TerrainMap {
 private int cursor;
 public record Tile(int x,int z,int height,int color,String biome) {}
 public List<Tile> sample(ServerWorld world,Match match,Rules rules){
  int side=rules.arenaSize/rules.cellSize;List<Tile> out=new ArrayList<>();
  for(int i=0;i<16;i++){
   int index=cursor++%(side*side),x=match.originX+(index%side)*rules.cellSize,z=match.originZ+(index/side)*rules.cellSize;
   if(!world.isChunkLoaded(new BlockPos(x,0,z)))continue;
   int y=world.getTopY(Heightmap.Type.WORLD_SURFACE,x,z)-1;BlockPos pos=new BlockPos(x,y,z);
   int rgb=world.getBlockState(pos).getMapColor(world,pos).color;
   out.add(new Tile(x,z,y,0xff000000|rgb,world.getBiome(pos).getKey().map(k->k.getValue().toString()).orElse("unknown")));
  }return out;
 }
}
