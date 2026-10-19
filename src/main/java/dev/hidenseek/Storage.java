package dev.hidenseek;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hidenseek.core.*;
import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;

public final class Storage {
 public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
 public static <T> T read(Path path,Class<T> type,T fallback) throws IOException {
  if(!Files.exists(path))return fallback;
  try(var reader=Files.newBufferedReader(path)){T v=JSON.fromJson(reader,type);if(v==null)throw new IOException("Empty save: "+path);return v;}
 }
 public static void write(Path path,Object value)throws IOException{
  Files.createDirectories(path.getParent());Path temp=path.resolveSibling(path.getFileName()+".tmp");
  Files.writeString(temp,JSON.toJson(value));try(var ch=FileChannel.open(temp,StandardOpenOption.WRITE)){ch.force(true);}
  try{Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
 }
}
