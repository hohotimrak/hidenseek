package dev.hidenseek;
import com.google.gson.JsonObject;
import dev.hidenseek.core.*;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;
/** Server-side hints. The common economy validates role, phase, funds and cooldown first. */
public final class HintRegistry {
 public record Context(StillHunt game,ServerPlayerEntity buyer,String request,String kind,JsonObject parameters) {}
 @FunctionalInterface public interface Handler { Match.Evaluation evaluate(Context context); }
 private static final Map<String,Handler> HANDLERS=new LinkedHashMap<>();
 static {for(String id:List.of("animal","block","biome","east","south","above","circle","rectangle","sector","distance","firework","screenshot","bearing","biome_is","structure"))register(id,c->c.game.evaluateBuiltIn(c.buyer,c.kind,c.request,c.parameters));}
 public static void register(String id,Handler handler){ContentCatalog.checkId(id);if(HANDLERS.putIfAbsent(id,Objects.requireNonNull(handler))!=null)throw new IllegalArgumentException("Duplicate hint handler: "+id);}
 public static boolean has(String id){return HANDLERS.containsKey(id);}
 public static Match.Evaluation evaluate(Context c){var handler=HANDLERS.get(c.kind);return handler==null?null:handler.evaluate(c);}
}
