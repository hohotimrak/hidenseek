package dev.hidenseek;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class Wire {
 private Wire(){}
 public record ImagePart(String match,String request,int index,int total,byte[] data) implements CustomPayload {
  public static final Id<ImagePart> ID=new Id<>(Identifier.of("hidenseek","image"));
  public static final PacketCodec<RegistryByteBuf,ImagePart> CODEC=PacketCodec.of((p,b)->{b.writeString(p.match,64);b.writeString(p.request,64);b.writeVarInt(p.index);b.writeVarInt(p.total);b.writeByteArray(p.data);},b->new ImagePart(b.readString(64),b.readString(64),b.readVarInt(),b.readVarInt(),b.readByteArray(16000)));
  public Id<? extends CustomPayload> getId(){return ID;}
 }
 public record Tiles(String json) implements CustomPayload {
  public static final Id<Tiles> ID=new Id<>(Identifier.of("hidenseek","tiles"));
  public static final PacketCodec<RegistryByteBuf,Tiles> CODEC=PacketCodec.of((p,b)->b.writeString(p.json,32000),b->new Tiles(b.readString(32000)));
  public Id<? extends CustomPayload> getId(){return ID;}
 }
 public record Intent(String json) implements CustomPayload {
  public static final Id<Intent> ID=new Id<>(Identifier.of("hidenseek","intent"));
  public static final PacketCodec<RegistryByteBuf,Intent> CODEC=PacketCodec.of((p,b)->b.writeString(p.json,4096),b->new Intent(b.readString(4096)));
  public Id<? extends CustomPayload> getId(){return ID;}
 }
 public record View(String json) implements CustomPayload {
  public static final Id<View> ID=new Id<>(Identifier.of("hidenseek","view"));
  public static final PacketCodec<RegistryByteBuf,View> CODEC=PacketCodec.of((p,b)->b.writeString(p.json,250000),b->new View(b.readString(250000)));
  public Id<? extends CustomPayload> getId(){return ID;}
 }
}
