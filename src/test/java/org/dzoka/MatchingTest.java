package org.dzoka;

import java.util.List;
import java.util.Map;
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
        assertEquals(RouteState.TRUNCATED,result.routes().stream()
                .filter(route -> route.route()==Route.EXACT_VECTOR).findFirst().orElseThrow().state());
        var incompatible=Matching.search(source,List.of(observation(1,"other-space",1,0)),2L,5);
        assertTrue(incompatible.candidates().isEmpty());
        var busy=Matching.search(source,IntStream.range(0,513).mapToObj(i -> observation(i,"active-space",1,0)).toList(),2L,5);
        assertTrue(busy.candidates().isEmpty());
        assertTrue(busy.routes().stream().allMatch(r -> r.state()==RouteState.CAPACITY_EXCEEDED));
    }

    @Test void highJointCosineCannotOverrideTrustedContradiction() {
        var right=new UUID(0,2);
        var wrong=new UUID(0,3);
        var source=new Observation(new UUID(0,1),1,null,null,null,null,new Identifier(1,"issuer","exact",null),null);
        var positive=new Observation(right,1,null,null,null,null,new Identifier(1,"issuer","exact",null),null);
        var conflict=new Observation(wrong,1,null,null,null,null,new Identifier(1,"issuer","different",null),null);
        var both=new JointRoute(true,false,Map.of(right,new JointMatch(1,0.9,
                        new JointEvidence(source.report(),right,"same","JOINT_IMAGE","JOINT_IMAGE")),
                wrong,new JointMatch(1,1.0,new JointEvidence(source.report(),wrong,"same","JOINT_IMAGE","JOINT_IMAGE"))));
        var result=Matching.search(source,List.of(positive,conflict),2L,5,null,Map.of(),both);
        assertEquals(right,result.candidates().get(0).report());
        assertEquals(wrong,result.candidates().get(1).report());
        assertEquals(Disposition.CONFLICT,result.candidates().get(1).score().disposition());
        assertEquals(1,result.candidates().get(1).ranks().get(Route.JOINT_VECTOR));
        assertNull(result.candidates().get(1).confidence());
    }

    private static Observation observation(long id,String space,double x,double y) {
        var key=new UUID(0,id);
        return new Observation(key,1,null,null,null,null,null,new Vector(key,space,new double[]{x,y}));
    }
}
