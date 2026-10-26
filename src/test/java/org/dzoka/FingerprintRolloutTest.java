package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.junit.jupiter.api.Test;

class FingerprintRolloutTest {
    @Test void cohortIdentityIsOrderIndependentButNeverDropsDuplicateOrMissingPolicy(){
        var one=new Source(UUID.randomUUID(),1);var two=new Source(UUID.randomUUID(),2);
        UUID id=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();
        var left=new Plan(id,List.of(one,two),List.of(a,b),5000,"f".repeat(64));
        var right=new Plan(id,List.of(two,one),List.of(b,a),5000,"f".repeat(64));assertEquals(left.canonical(),right.canonical());
        assertThrows(IllegalArgumentException.class,()->new Plan(id,List.of(one,one),List.of(a),10000,"f".repeat(64)));
        assertThrows(IllegalArgumentException.class,()->new Plan(id,List.of(one),List.of(a,a),10000,"f".repeat(64)));
        assertThrows(IllegalArgumentException.class,()->new Plan(id,List.of(one),List.of(a),null,"f".repeat(64)));
        assertThrows(IllegalArgumentException.class,()->new Plan(id,List.of(one),List.of(a),10001,"f".repeat(64)));
        assertThrows(IllegalArgumentException.class,()->new Plan(id,List.of(one),List.of(a),10000,"not-an-evaluation-digest"));
        assertThrows(IllegalArgumentException.class,()->new Source(one.reportId(),0));
    }
}
