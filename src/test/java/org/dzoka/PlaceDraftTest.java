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
    @Test void malformedUnicodeIsRejectedBeforeNormalizationAndPersistence(){
        for(String invalid:List.of("\uD800","\uDC00","x\uD800y","x\uDC00y","\uD800\uD800","\uDC00\uD800")) {
            assertThrows(IllegalArgumentException.class,()->new PlaceDraft(invalid,"FACILITY",null,null,null,null,null,List.of(),true,0L));
            assertThrows(IllegalArgumentException.class,()->new PlaceDraft("Valid","FACILITY",null,null,null,null,null,List.of(invalid),true,0L));
            assertThrows(IllegalArgumentException.class,()->PlaceDraft.normalize(invalid));
        }
    }
    @Test void validSupplementaryAndReplacementCharactersRemainExact(){
        String raw="  Cafe\u0301 \uD83D\uDE80 \uFFFD ?  ";
        var d=new PlaceDraft(raw,"FACILITY",null,null,null,null,null,List.of(raw),true,0L);
        assertEquals(raw,d.name());assertEquals(List.of(raw),d.aliases());
        assertEquals("caf\u00e9 \uD83D\uDE80 \uFFFD ?",PlaceDraft.normalize(raw));
    }
}
