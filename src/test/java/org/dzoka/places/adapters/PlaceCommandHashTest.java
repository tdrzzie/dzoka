package org.dzoka.places.adapters;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.places.domain.PlaceDraft;
import org.junit.jupiter.api.Test;
class PlaceCommandHashTest {
    private PlaceDraft fixture(){return new PlaceDraft("Synthetic institution","FACILITY",null,null,null,null,"Africa/Harare",List.of("Synthetic alias"),true,0L);}
    @Test void canonicalGoldenVectorUsesSpecifiedBinaryFraming(){
        // Independently generated with UTF-8, big-endian length/int/long and boolean framing.
        assertEquals("728f6989f01bb363b84d950978da6c83c676103499f4c42b5a195165e313e104",PlaceCommandHash.canonical(null,fixture()));
    }
    @Test void bothObservedHistoricalDigestsAreAcceptedWithoutWeakeningBodyChecks(){
        var d=fixture();var legacy=Set.of(PlaceCommandHash.legacy(null,d,false),PlaceCommandHash.legacy(null,d,true));
        assertEquals(Set.of("0b96326d588fb61bd31291304358e6c1b547ba88708ae3f7ada0a71c48da73be","bc33e0440698a8d2b25e2357af6167e3f8c068d55b8b93c546b0999ebdb22b6b"),legacy);
        var changed=new PlaceDraft("Changed institution","FACILITY",null,null,null,null,"Africa/Harare",d.aliases(),true,0L);
        for(String digest:legacy){assertTrue(PlaceCommandHash.matches(1,digest,null,d));assertFalse(PlaceCommandHash.matches(1,digest,null,changed));assertFalse(PlaceCommandHash.matches(2,digest,null,d));}
    }
    @Test void fieldBoundariesNullsRawAliasesAndUpdateTargetRemainSignificant(){
        var d=fixture();String baseline=PlaceCommandHash.canonical(null,d);
        assertNotEquals(baseline,PlaceCommandHash.canonical(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),d));
        assertNotEquals(baseline,PlaceCommandHash.canonical(null,new PlaceDraft(d.name(),d.kind(),null,0d,0d,0d,d.timezone(),d.aliases(),true,0L)));
        assertNotEquals(baseline,PlaceCommandHash.canonical(null,new PlaceDraft(d.name(),d.kind(),null,null,null,null,d.timezone(),List.of("Synthetic alias "),true,0L)));
        var a=new PlaceDraft("a|b","OTHER",null,null,null,null,null,List.of("c"),true,0L);
        var b=new PlaceDraft("a","OTHER",null,null,null,null,null,List.of("b|c"),true,0L);
        assertNotEquals(PlaceCommandHash.canonical(null,a),PlaceCommandHash.canonical(null,b));
    }
    @Test void unknownVersionsAndUnrecognizedDigestsFailClosed(){
        var d=fixture();assertFalse(PlaceCommandHash.matches(0,PlaceCommandHash.canonical(null,d),null,d));assertFalse(PlaceCommandHash.matches(3,PlaceCommandHash.canonical(null,d),null,d));
        assertFalse(PlaceCommandHash.matches(1,"0".repeat(64),null,d));assertTrue(PlaceCommandHash.matches(2,PlaceCommandHash.canonical(null,d),null,d));
    }
    @Test void validSupplementaryTextRetainsCanonicalAndBothHistoricalGoldenHashes(){
        var d=new PlaceDraft("Synthetic \uD83D\uDE80","FACILITY",null,null,null,null,"Africa/Harare",List.of("Door \uD83D\uDE80"),true,0L);
        assertEquals("4c66e71751d6668b71234609ae107f5f9ed7510e2f97a0a56b2f3b1238cd91d6",PlaceCommandHash.canonical(null,d));
        for(String digest:Set.of("737e4026a6da44167c5713d397f26a964428710c57032ccc44737efe391f6bd2","b0f75df8c81c5c86aab2baeddd7a34287259a98a17bdc1e85077fb2022a74dca"))assertTrue(PlaceCommandHash.matches(1,digest,null,d));
        assertNotEquals(PlaceCommandHash.canonical(null,d),PlaceCommandHash.canonical(null,new PlaceDraft("Synthetic ?","FACILITY",null,null,null,null,"Africa/Harare",d.aliases(),true,0L)));
    }
}
