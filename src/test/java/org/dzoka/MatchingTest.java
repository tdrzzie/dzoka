package org.dzoka;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.dzoka.matching.domain.EvidenceScoring;
import org.dzoka.matching.domain.EvidenceScoring.*;
import org.dzoka.matching.domain.Matching;
import org.dzoka.matching.domain.Matching.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchingTest {
    @Test void sparseAndZeroReliabilityCarryNoScoreAndCorrelatedSignalsShareOneBudget() {
        var absent=new Signal("ABSENT",Group.CONTEXT,State.UNAVAILABLE,null,0,"missing");
        var uncertain=new Signal("UNCERTAIN",Group.DESCRIPTION,State.AVAILABLE,1.0,0,"zero reliability");
        var none=EvidenceScoring.score(List.of(absent,uncertain),false);
        assertNull(none.similarity());
        assertEquals(0.0,none.coverage());
        var lexical=new Signal("LEXICAL",Group.DESCRIPTION,State.AVAILABLE,0.9,0.7,"observed");
        var vector=new Signal("VECTOR",Group.DESCRIPTION,State.AVAILABLE,0.9,0.7,"observed");
        var one=EvidenceScoring.score(List.of(lexical),false);
        var both=EvidenceScoring.score(List.of(lexical,vector),false);
        assertEquals(one.coverage(),both.coverage(),1e-12);
        assertEquals(one.similarity(),both.similarity(),1e-12);
        assertEquals(Disposition.CONFLICT,EvidenceScoring.score(List.of(lexical,vector),true).disposition());
    }

    @Test void vectorRankingMatchesIndependentExactAngularOrderAndReportsBudgets() {
        var source=observation(900,"active-space",1,0);
        var pool=IntStream.rangeClosed(1,24).mapToObj(i -> observation(i,"active-space",1,i/10.0)).toList();
        var result=Matching.search(source,pool,2L,5);
        var expected=pool.subList(0,5).stream().map(Observation::report).toList();
        assertEquals(expected,result.candidates().stream().map(Candidate::report).toList());
        assertTrue(result.partial());
        assertTrue(result.resultsTruncated());
        assertEquals(RouteState.TRUNCATED,result.routes().getLast().state());
        var incompatible=Matching.search(source,List.of(observation(1,"other-space",1,0)),2L,5);
        assertTrue(incompatible.candidates().isEmpty());
        var busy=Matching.search(source,IntStream.range(0,513).mapToObj(i -> observation(i,"active-space",1,0)).toList(),2L,5);
        assertTrue(busy.candidates().isEmpty());
        assertTrue(busy.routes().stream().allMatch(r -> r.state()==RouteState.CAPACITY_EXCEEDED));
    }

    private static Observation observation(long id,String space,double x,double y) {
        var key=new UUID(0,id);
        return new Observation(key,1,null,null,null,null,null,new Vector(key,space,new double[]{x,y}));
    }
}
