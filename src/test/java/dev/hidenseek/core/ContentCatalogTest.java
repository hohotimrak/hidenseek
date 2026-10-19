package dev.hidenseek.core;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ContentCatalogTest {
 Set<String> handlers=Set.of("effect","task","decoy"),objectives=Set.of("break_block","visit_waiting_point");
 @Test void defaultsAreValid(){ContentCatalog.defaults().validate(handlers,objectives);}
 @Test void unknownCodeNotExecutable(){var c=ContentCatalog.defaults();c.cards.put("bad",new ContentCatalog.CardDefinition("java.exec",1,10,"heavy",60,Map.of(),Map.of(),Set.of()));assertThrows(IllegalArgumentException.class,()->c.validate(handlers,objectives));}
 @Test void profileFiltersUnsupportedTasks(){var c=ContentCatalog.defaults();c.profile.remove("stone");assertFalse(c.eligible(1).contains("scavenge"));}
 @Test void missingTaskFailsEarly(){var c=ContentCatalog.defaults();c.tasks.remove("quarry");assertThrows(IllegalArgumentException.class,()->c.validate(handlers,objectives));}
}
