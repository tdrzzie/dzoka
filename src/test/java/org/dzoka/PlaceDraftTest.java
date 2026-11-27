package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.places.domain.PlaceDraft;
import org.junit.jupiter.api.Test;
class PlaceDraftTest {
    @Test void aliasesRetainRawUnicodeAndNormalizeSeparately(){
        var d=new PlaceDraft("  Cafe\u0301  ","FACILITY",null,0d,0d,12d,"Africa/Harare",List.of("  CHITORO  "),true,0L);
        assertEquals("  Cafe\u0301  ",d.name());assertEquals("café",PlaceDraft.normalize(d.name()));assertEquals("  CHITORO  ",d.aliases().getFirst());
        assertThrows(UnsupportedOperationException.class,()->d.aliases().add("another"));
    }
    @Test void invalidCoordinatesAndAmbiguousWithinPlaceAliasesAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new PlaceDraft("Place","FACILITY",null,0d,null,0d,null,List.of(),true,0L));
        assertThrows(IllegalArgumentException.class,()->new PlaceDraft("Place","FACILITY",null,Double.NaN,0d,0d,null,List.of(),true,0L));
        assertThrows(IllegalArgumentException.class,()->new PlaceDraft("Place","FACILITY",null,null,null,null,null,List.of("Room"," ROOM "),true,0L));
        assertThrows(IllegalArgumentException.class,()->new PlaceDraft("Place","FACILITY",null,null,null,null,"Unknown/Zone",List.of(),true,0L));
    }
}
